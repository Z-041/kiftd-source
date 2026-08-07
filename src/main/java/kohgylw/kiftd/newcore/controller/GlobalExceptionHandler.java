package kohgylw.kiftd.newcore.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import kohgylw.kiftd.newcore.domain.ApiResponse;
import kohgylw.kiftd.newcore.domain.ResultCode;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.LogUtil;


@ControllerAdvice
public class GlobalExceptionHandler {

	private final LogUtil lu;

	private final Gson gson;

	private static final AtomicLong totalExceptionCount = new AtomicLong(0);
	private static final Map<String, AtomicLong> exceptionTypeCount = new ConcurrentHashMap<>();

	public GlobalExceptionHandler(LogUtil lu, Gson gson) {
		this.lu = lu;
		this.gson = gson;
	}

	@ExceptionHandler({ Exception.class })
	public void handleException(final Exception e, final HttpServletRequest request, final HttpServletResponse response) {
		boolean isApiRequest = request.getRequestURI().startsWith("/api/");

		countException(e);

		if (e instanceof NoResourceFoundException) {
			if (isApiRequest) {
				writeJsonError(response, HttpServletResponse.SC_NOT_FOUND, ResultCode.NOT_FOUND);
			} else {
				sendErrorSafe(response, HttpServletResponse.SC_NOT_FOUND, null);
			}
			return;
		}

		// 保留 ResponseStatusException 携带的状态码（如 403 权限不足），避免被统一降级为 500
		if (e instanceof ResponseStatusException) {
			ResponseStatusException rse = (ResponseStatusException) e;
			int status = rse.getStatusCode().value();
			if (isApiRequest) {
				writeJsonError(response, status, statusCodeOf(status), rse.getReason());
			} else {
				sendErrorSafe(response, status, rse.getReason());
			}
			return;
		}

		// 按异常类型映射合适的 HTTP 状态码（400/404/413/405），避免统一降级为 500
		int mappedStatus = mapExceptionStatus(e);
		if (mappedStatus > 0) {
			if (isApiRequest) {
				writeJsonError(response, mappedStatus, statusCodeOf(mappedStatus));
			} else {
				sendErrorSafe(response, mappedStatus, null);
			}
			return;
		}

		this.lu.writeException(e);
		Printer.instance.print("处理请求时发生错误：" + e.getClass().getName() + " - " + e.getMessage());

		if (isApiRequest) {
			writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ResultCode.INTERNAL_SERVER_ERROR);
		} else {
			sendErrorSafe(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
		}
		// 不再在此处调用 fbu.checkFileBlocks()：该操作会删除"数据库无对应节点"的文件块，
		// 若异常源于数据库瞬时不可用，会误删正常文件块造成数据丢失。文件块一致性校验
		// 由 ServerInitListener 启动校队与文件系统管理操作中按需执行。
	}

	private void countException(Exception e) {
		totalExceptionCount.incrementAndGet();
		String exceptionType = e.getClass().getName();
		exceptionTypeCount.computeIfAbsent(exceptionType, k -> new AtomicLong(0)).incrementAndGet();
	}

	/**
	 * 将常见异常类型映射为合适的 HTTP 状态码；无法归类的异常返回 -1（交由默认 500 处理）
	 */
	private int mapExceptionStatus(Exception e) {
		if (e instanceof IllegalArgumentException || e instanceof HttpMessageNotReadableException
				|| e instanceof MissingServletRequestParameterException || e instanceof MethodArgumentNotValidException
				|| e instanceof MethodArgumentTypeMismatchException) {
			return HttpServletResponse.SC_BAD_REQUEST;
		}
		if (e instanceof MaxUploadSizeExceededException || e instanceof MultipartException) {
			return HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
		}
		if (e instanceof FileNotFoundException) {
			return HttpServletResponse.SC_NOT_FOUND;
		}
		if (e instanceof HttpRequestMethodNotSupportedException) {
			return HttpServletResponse.SC_METHOD_NOT_ALLOWED;
		}
		return -1;
	}

	/**
	 * 将 HTTP 状态码映射为语义一致的 ResultCode，避免响应体的 code 与状态码冲突
	 */
	private ResultCode statusCodeOf(int status) {
		switch (status) {
		case HttpServletResponse.SC_BAD_REQUEST:
			return ResultCode.BAD_REQUEST;
		case HttpServletResponse.SC_UNAUTHORIZED:
			return ResultCode.UNAUTHORIZED;
		case HttpServletResponse.SC_FORBIDDEN:
			return ResultCode.FORBIDDEN;
		case HttpServletResponse.SC_NOT_FOUND:
			return ResultCode.NOT_FOUND;
		case HttpServletResponse.SC_METHOD_NOT_ALLOWED:
			return ResultCode.METHOD_NOT_ALLOWED;
		case HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE:
			return ResultCode.PAYLOAD_TOO_LARGE;
		default:
			return ResultCode.INTERNAL_SERVER_ERROR;
		}
	}

	private void sendErrorSafe(HttpServletResponse response, int statusCode, String message) {
		try {
			if (!response.isCommitted()) {
				if (message != null) {
					response.sendError(statusCode, message);
				} else {
					response.sendError(statusCode);
				}
			}
		} catch (Exception ignored) {
			// 响应已提交或连接中断时无法再发送错误信息，忽略以避免二次异常
		}
	}

	private void writeJsonError(HttpServletResponse response, int statusCode, ResultCode resultCode) {
		writeJsonError(response, statusCode, resultCode, resultCode.getMessage());
	}

	private void writeJsonError(HttpServletResponse response, int statusCode, ResultCode resultCode, String message) {
		try {
			if (!response.isCommitted()) {
				response.setStatus(statusCode);
				response.setContentType("application/json;charset=UTF-8");
				ApiResponse<Void> errorResponse = ApiResponse.failure(resultCode.getCode(), message);
				response.getWriter().write(gson.toJson(errorResponse));
				response.getWriter().flush();
			}
		} catch (IOException ignored) {
			// 响应已提交或连接中断时无法再写出 JSON，忽略以避免二次异常
		}
	}

	public static long getTotalExceptionCount() {
		return totalExceptionCount.get();
	}

	public static Map<String, AtomicLong> getExceptionTypeCount() {
		return exceptionTypeCount;
	}

	public static void resetExceptionStats() {
		totalExceptionCount.set(0);
		exceptionTypeCount.clear();
	}
}

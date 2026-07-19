package kohgylw.kiftd.newcore.controller;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kohgylw.kiftd.newcore.domain.ApiResponse;
import kohgylw.kiftd.newcore.domain.ResultCode;
import kohgylw.kiftd.newcore.exception.BusinessException;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.LogUtil;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

	private final FileBlockUtil fbu;

	private final LogUtil lu;

	private final Gson gson;

	private static final AtomicLong totalExceptionCount = new AtomicLong(0);
	private static final Map<String, AtomicLong> exceptionTypeCount = new ConcurrentHashMap<>();

	public GlobalExceptionHandler(FileBlockUtil fbu, LogUtil lu, Gson gson) {
		this.fbu = fbu;
		this.lu = lu;
		this.gson = gson;
	}

	@ExceptionHandler({ BusinessException.class })
	public void handleBusinessException(final BusinessException e, final HttpServletRequest request,
			final HttpServletResponse response) {
		boolean isApiRequest = request.getRequestURI().startsWith("/api/");

		countException(e);

		Printer.instance.print("业务异常：" + e.getResultCode().getCode() + " - " + e.getMessage());

		if (isApiRequest) {
			int statusCode = mapResultCodeToHttpStatus(e.getResultCode());
			writeJsonError(response, statusCode, e.getResultCode(), e.getMessage());
		} else {
			sendErrorSafe(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
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

		this.lu.writeException(e);
		Printer.instance.print("处理请求时发生错误：" + e.getClass().getName() + " - " + e.getMessage());

		if (isApiRequest) {
			writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ResultCode.INTERNAL_SERVER_ERROR);
		} else {
			sendErrorSafe(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
		}
		this.fbu.checkFileBlocks();
	}

	private void countException(Exception e) {
		totalExceptionCount.incrementAndGet();
		String exceptionType = e.getClass().getName();
		exceptionTypeCount.computeIfAbsent(exceptionType, k -> new AtomicLong(0)).incrementAndGet();
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
		}
	}

	private int mapResultCodeToHttpStatus(ResultCode resultCode) {
		switch (resultCode) {
		case SUCCESS:
			return HttpServletResponse.SC_OK;
		case BAD_REQUEST:
		case VERIFICATION_CODE_ERROR:
		case VERIFICATION_CODE_EXPIRED:
		case PASSWORD_TOO_WEAK:
		case FILE_SIZE_EXCEEDED:
		case FILE_TYPE_NOT_ALLOWED:
		case FOLDER_LIMIT_EXCEEDED:
			return HttpServletResponse.SC_BAD_REQUEST;
		case UNAUTHORIZED:
		case USERNAME_OR_PASSWORD_ERROR:
			return HttpServletResponse.SC_UNAUTHORIZED;
		case FORBIDDEN:
		case USER_NOT_FOUND:
		case FOLDER_ACCESS_DENIED:
		case FILE_ACCESS_DENIED:
		case FILE_CHAIN_INVALID:
		case FILE_CHAIN_EXPIRED:
		case SIGN_UP_NOT_ALLOWED:
		case PASSWORD_CHANGE_NOT_ALLOWED:
			return HttpServletResponse.SC_FORBIDDEN;
		case NOT_FOUND:
		case FOLDER_NOT_FOUND:
		case FILE_NOT_FOUND:
			return HttpServletResponse.SC_NOT_FOUND;
		default:
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
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

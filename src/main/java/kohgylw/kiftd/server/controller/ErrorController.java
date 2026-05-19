package kohgylw.kiftd.server.controller;

import jakarta.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import kohgylw.kiftd.server.util.*;
import kohgylw.kiftd.printer.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 *
 * <h2>全局异常处理控制器</h2>
 * <p>
 * 该控制器使用 @ControllerAdvice 注解捕获整个应用中所有控制器抛出的未处理异常，
 * 统一进行日志记录、文件块完整性检查以及控制台错误输出。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@ControllerAdvice
public class ErrorController {

	@Resource
	private FileBlockUtil fbu;

	@Resource
	private LogUtil lu;

	/**
	 *
	 * <h2>全局异常处理</h2>
	 * <p>
	 * 捕获应用中所有未处理的 Exception 类型异常，执行以下操作：
	 * <ol>
	 *   <li>将异常写入运行日志文件</li>
	 *   <li>设置 HTTP 500 错误状态码</li>
	 *   <li>检查文件块存储的完整性</li>
	 *   <li>在控制台输出错误信息</li>
	 * </ol>
	 * </p>
	 *
	 * @param e        Exception 捕获到的异常对象
	 * @param response HttpServletResponse 响应对象，用于设置错误状态码
	 */
	@ExceptionHandler({ Exception.class })
	public void handleException(final Exception e, final HttpServletResponse response) {
		if (e instanceof NoResourceFoundException) {
			try {
				if (!response.isCommitted()) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
				}
			} catch (Exception ignored) {
			}
			return;
		}
		this.lu.writeException(e);
		try {
			if (!response.isCommitted()) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		} catch (Exception ignored) {
		}
		this.fbu.checkFileBlocks();
		Printer.instance
				.print("处理请求时发生错误：\n\r------信息------\n\r"
						+ e.getMessage() + "\n\r------信息------");
	}
}
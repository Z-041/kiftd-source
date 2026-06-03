package kohgylw.kiftd.newcore.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.LogUtil;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

	@Resource
	private FileBlockUtil fbu;

	@Resource
	private LogUtil lu;

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
		Printer.instance.print("处理请求时发生错误：" + e.getClass().getName());
	}
}

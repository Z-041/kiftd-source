package kohgylw.kiftd.server.util;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kohgylw.kiftd.printer.Printer;

/**
 * 
 * <h2>断点式文件输出流写出工具</h2>
 * <p>
 * 该工具负责处理断点下载请求并以相应规则写出文件流。需要提供断点续传服务，请继承该类并调用writeRangeFileStream方法。
 * 若无法继承，也可以直接静态调用此方法。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class RangeFileStreamWriter {

	public static final long DOWNLOAD_CACHE_MAX_AGE = 1800L;

	/**
	 * 
	 * <h2>使用断点续传技术提供输出流</h2>
	 * <p>
	 * 处理普通的或带有断点续传参数的下载请求，并按照请求方式提供相应的输出流写出。请传入相应的参数并执行该方法以开始传输。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request      javax.servlet.http.HttpServletRequest 请求对象
	 * @param response     javax.servlet.http.HttpServletResponse 响应对象
	 * @param fo           java.io.File 需要写出的文件
	 * @param fname        java.lang.String 文件名
	 * @param contentType  java.lang.String HTTP Content-Type类型（用于控制客户端行为）
	 * @param maxRate      long 最大输出速率，以B/s为单位，若为负数则不限制输出速率（用于限制客户端的下载速度）
	 * @param eTag         java.lang.String 资源的唯一性标识，例如"aabbcc"
	 * @param isAttachment boolean 是否作为附件回传，若希望用户下载（而非预览）则应设置为true
	 * @return int 操作结束时返回的状态码
	 */
	public static int writeRangeFileStream(HttpServletRequest request, HttpServletResponse response, File fo,
			String fname, String contentType, long maxRate, String eTag, boolean isAttachment) {
		return writeRangeFile(request, response, fo, fname, contentType, maxRate, eTag, isAttachment, true);
	}

	/**
	 * 
	 * <h2>回传文件数据，可选择是否发送具体的文件内容</h2>
	 * <p>
	 * 该方法用于提供对文件下载请求的处理，并按照请求方式提供相应的输出流写出。当选择发送具体的文件内容时将会正常返回文件内容， 否则仅返回响应头而无响应体。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request      javax.servlet.http.HttpServletRequest 请求对象
	 * @param response     javax.servlet.http.HttpServletResponse 响应对象
	 * @param fo           java.io.File 需要写出的文件
	 * @param fname        java.lang.String 文件名
	 * @param contentType  java.lang.String HTTP Content-Type类型（用于控制客户端行为）
	 * @param maxRate      long 最大输出速率，以B/s为单位，若为负数则不限制输出速率（用于限制客户端的下载速度）
	 * @param eTag         java.lang.String 资源的唯一性标识，例如"aabbcc"
	 * @param isAttachment boolean 是否作为附件回传，若希望用户下载则应设置为true
	 * @param sendBody     boolean 是否发送具体的文件内容，若设置为false，则仅返回响应头
	 * @return int 操作结束时返回的状态码
	 */
	private static int setAndReturnStatus(HttpServletResponse response, int status) {
		response.setStatus(status);
		return status;
	}

	private static int writeRangeFile(HttpServletRequest request, HttpServletResponse response, File fo, String fname,
			String contentType, long maxRate, String eTag, boolean isAttachment, boolean sendBody) {
		long fileLength = fo.length();// 文件总大小
		int status = HttpServletResponse.SC_OK;// 初始响应码为200
		// 检查是否有可用的缓存
		String lastModified = ServerTimeUtil.getLastModifiedFromBlock(fo);
		String ifModifiedSince = request.getHeader("If-Modified-Since");
		String ifNoneMatch = request.getHeader("If-None-Match");
		// 是否提供了两个判断参数之一？
		if (ifModifiedSince != null || ifNoneMatch != null) {
			// 是，那么是否提供了Etag？
			if (ifNoneMatch != null) {
				// 是，则只检查Etag，理论上其比Last-Modified更准确
				if (ifNoneMatch.trim().equals(eTag)) {
					return setAndReturnStatus(response, HttpServletResponse.SC_NOT_MODIFIED);
				}
			} else {
				// 不是，则再检查Last-Modified
				if (ifModifiedSince.trim().equals(lastModified)) {
					return setAndReturnStatus(response, HttpServletResponse.SC_NOT_MODIFIED);
				}
			}
		}
		// 检查断点续传请求是否过期，两个条件，有就要满足，没有就算了
		String ifUnmodifiedSince = request.getHeader("If-Unmodified-Since");
		if (ifUnmodifiedSince != null && !(ifUnmodifiedSince.trim().equals(lastModified))) {
			return setAndReturnStatus(response, HttpServletResponse.SC_PRECONDITION_FAILED);
		}
		String ifMatch = request.getHeader("If-Match");
		if (ifMatch != null && !(ifMatch.trim().equals(eTag))) {
			return setAndReturnStatus(response, HttpServletResponse.SC_PRECONDITION_FAILED);
		}
		// 设置请求头，基于kiftd文件系统推荐使用application/octet-stream
		response.setContentType(contentType);
		// 设置文件信息
		response.setCharacterEncoding("UTF-8");
		// 设置Content-Disposition信息
		if (isAttachment) {
			response.setHeader("Content-Disposition", "attachment; filename=\"" + EncodeUtil.getFileNameByUTF8(fname)
					+ "\"; filename*=utf-8''" + EncodeUtil.getFileNameByUTF8(fname));
		} else {
			response.setHeader("Content-Disposition", "inline");
		}
		// 设置支持断点续传功能
		response.setHeader("Accept-Ranges", "bytes");
		// 设置缓存控制信息
		response.setHeader("ETag", eTag);
		response.setHeader("Last-Modified", ServerTimeUtil.getLastModifiedFromBlock(fo));
		response.setHeader("Cache-Control", "max-age=" + DOWNLOAD_CACHE_MAX_AGE);
		// 针对具备断点续传性质的请求进行解析（RFC 7233，统一解析一次并复用结果）
		long start = 0, end = 0;
		boolean hasExplicitEnd = false;
		boolean isValidRange = false;
		String range = request.getHeader("Range");
		final String ifRange = request.getHeader("If-Range");
		if (range != null && range.startsWith("bytes=")) {
			try {
				String[] values = range.split("=")[1].split("-");
				if (values[0].trim().length() == 0 && values.length > 1) {
					// 解析后缀范围 bytes=-N：请求文件末尾 N 字节
					long suffixLength = Long.parseLong(values[1].trim());
					if (suffixLength > 0) {
						start = Math.max(fileLength - suffixLength, 0);
						end = fileLength - 1;
						hasExplicitEnd = true;
						isValidRange = true;
					}
				} else {
					start = Long.parseLong(values[0].trim());
					if (values.length > 1 && values[1].trim().length() > 0) {
						end = Long.parseLong(values[1].trim());
						hasExplicitEnd = true;
					}
					isValidRange = true;
				}
			} catch (NumberFormatException e) {
				// 无法解析的 Range 头视为无效，回退为整文件响应（RFC 7233 允许忽略无效 Range）
				start = 0;
				end = 0;
				hasExplicitEnd = false;
				isValidRange = false;
			}
		}
		// RFC 7233：起始位置越界（负值或超出文件长度）时返回 416，避免 seek 越界与空响应体
		if (isValidRange && (start < 0 || start >= fileLength)) {
			status = HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE;
			response.setStatus(status);
			response.setHeader("Content-Range", "bytes */" + fileLength);
			return status;
		}
		long requestSize = 0;
		if (hasExplicitEnd && end >= start) {
			// 防止 end - start + 1 溢出（如 Range: bytes=0-9223372036854775807），并将区间收敛到文件实际长度内
			long boundedEnd = Math.min(end, fileLength - 1);
			requestSize = boundedEnd - start + 1;
		} else {
			requestSize = fileLength - start;
		}
		// 判定是否为有效的部分内容请求（带 If-Range 条件校验）
		if (isValidRange && (ifRange == null || ifRange.trim().equals(eTag) || ifRange.trim().equals(lastModified))) {
			// 设置响应状态为206
			status = HttpServletResponse.SC_PARTIAL_CONTENT;
			response.setStatus(status);
			// 设置Content-Range，格式为“bytes 起始偏移-结束偏移/文件的总大小”
			if (hasExplicitEnd) {
				long boundedEnd = Math.min(end, fileLength - 1);
				response.setHeader("Content-Length", "" + (boundedEnd - start + 1));
				response.setHeader("Content-Range", "bytes " + start + "-" + boundedEnd + "/" + fileLength);
			} else {
				response.setHeader("Content-Length", "" + (fileLength - start));
				response.setHeader("Content-Range", "bytes " + start + "-" + (fileLength - 1) + "/" + fileLength);
			}
		} else {
			// 不进行断点续传，整文件响应
			response.setHeader("Content-Length", sendBody ? String.valueOf(fileLength) : "0");
		}
		if (sendBody) {
			// 写出缓冲
			byte[] buf = new byte[ConfigurationManager.instance().getBuffSize()];
			// 读取文件并写处至输出流
			try (RandomAccessFile raf = new RandomAccessFile(fo, "r")) {
				HttpSession session = request.getSession(false);
				try (OutputStream out = maxRate > 0 && session != null
						? new VariableSpeedBufferedOutputStream(response.getOutputStream(), maxRate, session)
						: new BufferedOutputStream(response.getOutputStream())) {
					raf.seek(start);
					long needSize = requestSize;
					while (needSize > 0) {
						int n = raf.read(buf);
						if (n <= 0) {
							// 源文件被截断或提前结束，避免死循环
							break;
						}
						int toWrite = (int) Math.min(needSize, n);
						out.write(buf, 0, toWrite);
						needSize -= toWrite;
						if (n < buf.length) {
							// 已读到文件末尾，剩余部分不可再读
							break;
						}
					}
					out.flush();
				}
			} catch (IOException | IndexOutOfBoundsException ex) {
				// 针对任何IO异常忽略，传输失败不处理
				status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			} catch (IllegalArgumentException e) {
				status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
				try {
					response.sendError(status);
				} catch (IOException e1) {
					Printer.instance.print("写入错误响应时发生IO异常：" + e1.getMessage());
				}
			}
		}
		return status;
	}
}

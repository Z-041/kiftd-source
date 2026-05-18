package kohgylw.kiftd.server.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kohgylw.kiftd.server.service.ExternalDownloadService;
import kohgylw.kiftd.server.service.FileChainService;

/**
 *
 * <h2>外部链接控制器</h2>
 * <p>
 * 该控制器处理来自外部链接的请求，例如用于文件分享的临时下载链接和永久资源链。
 * 所有请求均允许跨域访问，以支持外部站点直接引用资源。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Controller
@CrossOrigin
@RequestMapping({ "/externalLinksController" })
public class ExternalLinksController {

	@Resource
	private ExternalDownloadService eds;

	@Resource
	private FileChainService fcs;

	/**
	 *
	 * <h2>获取临时下载密钥</h2>
	 * <p>
	 * 为指定的文件生成一个临时下载密钥，外部用户凭此密钥可在有效期内下载文件。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象，包含文件 ID 参数
	 * @return String 临时下载密钥字符串
	 */
	@RequestMapping("/getDownloadKey.ajax")
	public @ResponseBody String getDownloadKey(HttpServletRequest request) {
		return eds.getDownloadKey(request);
	}

	/**
	 *
	 * 
	 * <h2>通过密钥下载文件</h2>
	 * <p>外部用户使用获取的临时下载密钥来下载对应的文件。</p>
	 *
	 * @param request  HttpServletRequest 请求对象，包含密钥信息
	 * @param response HttpServletResponse 响应对象，用于输出文件流
	 */
	@RequestMapping("/downloadFileByKey/{fileName}")
	public void downloadFileByKey(HttpServletRequest request, HttpServletResponse response) {
		eds.downloadFileByKey(request, response);
	}

	/**
	 *
	 * <h2>通过永久资源链访问资源</h2>
	 * <p>
	 * 使用永久资源链接密钥（ckey）直接访问对应的文件资源，用于实现持久化的文件分享。
	 * </p>
	 *
	 * @param request  HttpServletRequest 请求对象，包含资源链密钥
	 * @param response HttpServletResponse 响应对象，用于输出文件流
	 */
	@RequestMapping("/chain/{fileName}")
	public void chain(HttpServletRequest request, HttpServletResponse response) {
		fcs.getResourceByChainKey(request, response);
	}

}
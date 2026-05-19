package kohgylw.kiftd.server.controller;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

/**
 *
 * <h2>欢迎页控制器</h2>
 * <p>该控制器处理根路径 "/" 的请求，将用户自动重定向到主页面 home.html。</p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Controller
public class WelcomeController {

	/**
	 *
	 * <h2>根路径重定向</h2>
	 * <p>
	 * 当用户访问服务器根路径时，将其重定向至 home.html 主页。
	 * </p>
	 *
	 * @return String 重定向路径 "redirect:/home.html"
	 */
	@RequestMapping({ "/" })
	public String home() {
		return "redirect:/home.html";
	}

}
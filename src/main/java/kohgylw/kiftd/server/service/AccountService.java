package kohgylw.kiftd.server.service;

import jakarta.servlet.http.*;

/**
 *
 * <h2>账户服务接口</h2>
 * <p>
 * 该接口定义了与用户账户相关的所有业务操作，包括登录校验、注销、密码管理、
 * 注册新账户、验证码生成以及会话保活应答等功能。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface AccountService {

	/**
	 *
	 * <h2>校验登录请求</h2>
	 * <p>验证用户提交的登录凭据（账号和 RSA 加密密码），校验通过后创建登录会话。</p>
	 *
	 * @param request HttpServletRequest 请求对象，包含账号和加密密码参数
	 * @param session HttpSession 会话对象，用于存储登录状态
	 * @return String 登录结果，"SUCCESS" 表示成功，其他值表示失败原因
	 */
	String checkLoginRequest(final HttpServletRequest request, final HttpSession session);

	/**
	 *
	 * <h2>注销登录</h2>
	 * <p>使指定会话失效，清除其中的登录状态信息。</p>
	 *
	 * @param session HttpSession 待注销的会话对象
	 */
	void logout(final HttpSession session);

	/**
	 *
	 * <h2>获取 RSA 公钥</h2>
	 * <p>返回用于加密用户密码的 RSA 公钥字符串，前端在登录时使用该公钥加密密码后再提交。</p>
	 *
	 * @return String RSA 公钥的 Base64 编码字符串
	 */
	String getPublicKey();

	/**
	 *
	 * <h2>生成并输出登录验证码</h2>
	 * <p>生成一张图片验证码并写入响应流，同时将验证码文本存入当前会话中以供后续校验。</p>
	 *
	 * @param request  HttpServletRequest 请求对象
	 * @param response HttpServletResponse 响应对象，用于输出验证码图片
	 * @param session  HttpSession 会话对象，用于存储验证码文本
	 */
	void getNewLoginVerCode(final HttpServletRequest request, final HttpServletResponse response,
			final HttpSession session);

	/**
	 *
	 * <h2>会话保活应答</h2>
	 * <p>
	 * 对于需要长期保持会话跟踪的操作（如上传、视频播放），提供定时应答以保持会话不超时。
	 * 若用户已登录则响应 "pong"，否则响应空字符串。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return String "pong" 表示会话有效，空字符串表示未登录
	 */
	String doPong(final HttpServletRequest request);

	/**
	 *
	 * <h2>修改账户密码</h2>
	 * <p>
	 * 处理修改账户密码的请求，必须已开启用户修改密码功能。请求需包含加密后的旧密码和新密码。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象，包含加密后的密码修改请求参数
	 * @return String 修改结果，含义如下：
	 *         <ul>
	 *         <li>success - 修改成功</li>
	 *         <li>mustlogin - 未登录任何账户</li>
	 *         <li>illegal - 修改密码功能被禁止</li>
	 *         <li>oldpwderror - 旧密码输入错误，未能通过验证</li>
	 *         <li>needsubmitvercode - 需要提交验证码</li>
	 *         <li>invalidnewpwd - 新密码格式不合法</li>
	 *         <li>cannotchangepwd - 出现意外错误导致密码修改失败</li>
	 *         <li>error - 加密验证失败</li>
	 *         </ul>
	 */
	String changePassword(final HttpServletRequest request);

	/**
	 *
	 * <h2>查询是否允许自由注册</h2>
	 * <p>返回服务器是否开启了自由注册新账户注册功能，若允许则返回字符串 "true"。</p>
	 *
	 * @return String "true" 表示允许注册，"false" 表示不允许
	 */
	String isAllowSignUp();

	/**
	 *
	 * <h2>执行账户注册</h2>
	 * <p>
	 * 执行新账户注册操作并返回注册结果，必须已开启自由注册功能。
	 * 注册成功后将自动登录新账户。
	 * </p>
	 *
	 * @param request HttpServletRequest 请求对象，包含注册信息
	 * @return String 注册结果，含义如下：
	 *         <ul>
	 *         <li>success - 注册成功并直接登录新账户</li>
	 *         <li>illegal - 注册功能已被禁用</li>
	 *         <li>mustlogout - 必须先退出当前账户</li>
	 *         <li>accountexists - 账户名已经存在</li>
	 *         <li>needvercode - 需要验证码验证</li>
	 *         <li>invalidaccount - 账户名格式不合法</li>
	 *         <li>invalidpwd - 密码格式不合法</li>
	 *         <li>cannotsignup - 出现意外错误导致注册失败</li>
	 *         <li>error - 加密验证失败</li>
	 *         </ul>
	 */
	String doSignUp(final HttpServletRequest request);
}
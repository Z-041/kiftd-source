package kohgylw.kiftd.mc;

import java.util.List;
import java.util.Scanner;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.SizeFormatUtil;


public class AccountCommandHandler {

	public void handleAccountCommand(String[] args) {
		if (args.length < 2) {
			printAccountHelp();
			return;
		}
		String subCommand = args[1];
		switch (subCommand) {
		case "list":
		case "ls":
			listAccounts();
			break;
		case "add":
		case "create":
			addAccount(args);
			break;
		case "del":
		case "delete":
		case "remove":
			deleteAccount(args);
			break;
		case "chpwd":
		case "password":
			changeAccountPassword(args);
			break;
		case "chauth":
		case "auth":
			changeAccountAuth(args);
			break;
		case "info":
		case "show":
			showAccountInfo(args);
			break;
		default:
			Printer.instance.print("错误：未知的账户管理命令 - " + subCommand);
			printAccountHelp();
			break;
		}
	}

	public void resetPassword(String[] args) {
		if (args.length != 3) {
			Printer.instance.print("用法: -resetpwd <账户名> <新密码>");
			Printer.instance.print("示例: -resetpwd admin newPassword123");
			return;
		}
		String account = args[1];
		String newPassword = args[2];
		try {
			if (ConfigurationManager.instance().foundAccount(account)) {
				boolean result = ConfigurationManager.instance().resetPassword(account, newPassword);
				if (result) {
					Printer.instance.print("密码重置成功！账户 " + account + " 的密码已更新。");
				} else {
					Printer.instance.print("错误：密码重置失败，请检查账户配置文件是否可写。");
				}
			} else {
				Printer.instance.print("错误：账户 " + account + " 不存在。");
			}
		} catch (Exception e) {
			Printer.instance.print("错误：密码重置时发生异常 - " + e.getMessage());
		}
	}

	private void printAccountHelp() {
		Printer.instance.print("账户管理命令帮助：");
		Printer.instance.print("  -account list                    列出所有账户");
		Printer.instance.print("  -account add <账户> <密码>       添加新账户");
		Printer.instance.print("  -account del <账户>              删除指定账户");
		Printer.instance.print("  -account chpwd <账户> <新密码>   修改账户密码");
		Printer.instance.print("  -account chauth <账户> <权限>    修改账户权限（cudrml等字符组合）");
		Printer.instance.print("  -account info <账户>             查看账户详细信息");
		Printer.instance.print("权限字符说明：c=新建文件夹 u=上传文件 d=删除 r=重命名 m=移动 l=下载");
		Printer.instance.print("示例：");
		Printer.instance.print("  -account list");
		Printer.instance.print("  -account add user1 password123");
		Printer.instance.print("  -account chpwd admin newpass456");
		Printer.instance.print("  -account chauth user1 cudrml");
	}

	private void listAccounts() {
		try {
			List<String> accounts = ConfigurationManager.instance().getAllAccounts();
			if (accounts.isEmpty()) {
				Printer.instance.print("当前没有任何账户。");
			} else {
				Printer.instance.print("账户列表（共 " + accounts.size() + " 个）：");
				Printer.instance.print("----------------------------------------");
				int index = 1;
				for (String account : accounts) {
					String auth = ConfigurationManager.instance().getAccountAuth(account);
					boolean isSuper = ConfigurationManager.instance().isSuperAdmin(account);
					StringBuilder line = new StringBuilder();
					line.append(String.format("%3d. %-20s", index++, account));
					if (isSuper) {
						line.append(" [超级管理员]");
					} else if (auth != null && !auth.isEmpty()) {
						line.append(" 权限: ").append(auth);
					} else {
						line.append(" 权限: (无)");
					}
					Printer.instance.print(line.toString());
				}
				Printer.instance.print("----------------------------------------");
			}
		} catch (Exception e) {
			Printer.instance.print("错误：获取账户列表失败 - " + e.getMessage());
		}
	}

	private void addAccount(String[] args) {
		if (args.length < 4) {
			Printer.instance.print("用法: -account add <账户名> <密码> [权限]");
			Printer.instance.print("示例: -account add user1 password123 cudrml");
			return;
		}
		String account = args[2];
		String password = args[3];
		String auth = args.length > 4 ? args[4] : "";
		try {
			if (ConfigurationManager.instance().foundAccount(account)) {
				Printer.instance.print("错误：账户 " + account + " 已存在。");
				return;
			}
			boolean result = ConfigurationManager.instance().createNewAccount(account, password);
			if (result && !auth.isEmpty()) {
				ConfigurationManager.instance().updateAccountAuth(account, auth);
			}
			if (result) {
				Printer.instance.print("账户创建成功！");
				Printer.instance.print("  账户名: " + account);
				if (!auth.isEmpty()) {
					Printer.instance.print("  权限: " + auth);
				}
			} else {
				Printer.instance.print("错误：账户创建失败，请检查账户配置文件是否可写。");
			}
		} catch (Exception e) {
			Printer.instance.print("错误：创建账户时发生异常 - " + e.getMessage());
		}
	}

	private void deleteAccount(String[] args) {
		if (args.length < 3) {
			Printer.instance.print("用法: -account del <账户名>");
			Printer.instance.print("示例: -account del user1");
			return;
		}
		String account = args[2];
		try {
			if (!ConfigurationManager.instance().foundAccount(account)) {
				Printer.instance.print("错误：账户 " + account + " 不存在。");
				return;
			}
			Printer.instance.print("警告：您即将删除账户 " + account + "，该操作不可恢复！");
			Printer.instance.print("确认删除请输入: YES");
			System.out.print("> ");
			Scanner confirmScanner = new Scanner(System.in);
			String confirm = confirmScanner.nextLine().trim();
			if (!"YES".equals(confirm)) {
				Printer.instance.print("已取消删除操作。");
				return;
			}
			boolean result = ConfigurationManager.instance().deleteAccount(account);
			if (result) {
				Printer.instance.print("账户 " + account + " 已成功删除。");
			} else {
				Printer.instance.print("错误：删除账户失败，请检查账户配置文件是否可写。");
			}
		} catch (Exception e) {
			Printer.instance.print("错误：删除账户时发生异常 - " + e.getMessage());
		}
	}

	private void changeAccountPassword(String[] args) {
		if (args.length < 4) {
			Printer.instance.print("用法: -account chpwd <账户名> <新密码>");
			Printer.instance.print("示例: -account chpwd admin newpass123");
			return;
		}
		String account = args[2];
		String newPassword = args[3];
		try {
			if (!ConfigurationManager.instance().foundAccount(account)) {
				Printer.instance.print("错误：账户 " + account + " 不存在。");
				return;
			}
			boolean result = ConfigurationManager.instance().resetPassword(account, newPassword);
			if (result) {
				Printer.instance.print("密码修改成功！账户 " + account + " 的密码已更新。");
			} else {
				Printer.instance.print("错误：密码修改失败，请检查账户配置文件是否可写。");
			}
		} catch (Exception e) {
			Printer.instance.print("错误：修改密码时发生异常 - " + e.getMessage());
		}
	}

	private void changeAccountAuth(String[] args) {
		if (args.length < 4) {
			Printer.instance.print("用法: -account chauth <账户名> <权限>");
			Printer.instance.print("权限字符：c=新建文件夹 u=上传文件 d=删除 r=重命名 m=移动 l=下载");
			Printer.instance.print("示例: -account chauth user1 cudrml");
			return;
		}
		String account = args[2];
		String auth = args[3];
		try {
			if (!ConfigurationManager.instance().foundAccount(account)) {
				Printer.instance.print("错误：账户 " + account + " 不存在。");
				return;
			}
			boolean result = ConfigurationManager.instance().updateAccountAuth(account, auth);
			if (result) {
				Printer.instance.print("权限修改成功！账户 " + account + " 的权限已更新为: " + auth);
			} else {
				Printer.instance.print("错误：权限修改失败，请检查账户配置文件是否可写。");
			}
		} catch (Exception e) {
			Printer.instance.print("错误：修改权限时发生异常 - " + e.getMessage());
		}
	}

	private void showAccountInfo(String[] args) {
		if (args.length < 3) {
			Printer.instance.print("用法: -account info <账户名>");
			Printer.instance.print("示例: -account info admin");
			return;
		}
		String account = args[2];
		try {
			if (!ConfigurationManager.instance().foundAccount(account)) {
				Printer.instance.print("错误：账户 " + account + " 不存在。");
				return;
			}
			String auth = ConfigurationManager.instance().getAccountAuth(account);
			String group = ConfigurationManager.instance().getAccountGroup(account);
			boolean isSuper = ConfigurationManager.instance().isSuperAdmin(account);
			long maxSize = ConfigurationManager.instance().getUploadFileSize(account);
			long maxRate = ConfigurationManager.instance().getDownloadMaxRate(account);

			Printer.instance.print("账户信息: " + account);
			Printer.instance.print("----------------------------------------");
			Printer.instance.print("  是否存在: 是");
			Printer.instance.print("  超级管理员: " + (isSuper ? "是" : "否"));
			Printer.instance.print("  权限: " + (auth != null ? auth : "(无)"));
			Printer.instance.print("  所属组: " + (group != null ? group : "(无)"));
			Printer.instance.print("  上传容量限制: " + SizeFormatUtil.formatFileSize(maxSize));
			Printer.instance.print("  下载速率限制: " + (maxRate > 0 ? maxRate + " B/s" : "无限制"));
			Printer.instance.print("----------------------------------------");
		} catch (Exception e) {
			Printer.instance.print("错误：获取账户信息失败 - " + e.getMessage());
		}
	}

}

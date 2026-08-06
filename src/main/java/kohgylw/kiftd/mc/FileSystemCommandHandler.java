package kohgylw.kiftd.mc;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.exception.FilesTotalOutOfLimitException;
import kohgylw.kiftd.server.exception.FoldersTotalOutOfLimitException;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.util.SizeFormatUtil;
import kohgylw.kiftd.util.file_system_manager.FileSystemManager;
import kohgylw.kiftd.util.file_system_manager.pojo.FileSystemFolderView;
import kohgylw.kiftd.util.file_system_manager.pojo.FolderTreeNode;


public class FileSystemCommandHandler {

	private FileSystemFolderView currentFolder;
	private final ScannerProvider scannerProvider;
	private final ProgressListenerFactory progressListenerFactory;

	public FileSystemCommandHandler(ScannerProvider scannerProvider, ProgressListenerFactory progressListenerFactory) {
		this.scannerProvider = scannerProvider;
		this.progressListenerFactory = progressListenerFactory;
	}

	public FileSystemFolderView getCurrentFolder() {
		return currentFolder;
	}

	public void enterFileManager() {
		Printer.instance.print("已进入文件管理功能。");
		try {
			if (currentFolder == null || currentFolder.getCurrent() == null
					|| FileSystemManager.getInstance().selectFolderById(currentFolder.getCurrent().getFolderId()) == null) {
				getFolderView("root");
			}
		} catch (Exception e) {
			Printer.instance.print("错误：无法打开文件系统，该文件系统可能正在被另一个kiftd占用。");
			return;
		}

		String fsCommandTips = "kiftd files:您可以输入以下指令进行文件管理：\r\n"
				+ "ls 显示当前文件夹内容（可使用参数 \"-l\" 显示所有项目的详细信息）\r\n"
				+ "cd {\"文件夹名称\" 或 \"--文件夹序号\"} 进入指定文件夹（示例：\"cd foo\" 或 \"cd --1\"，如需返回上一级请输入\"cd ../\"）\r\n"
				+ "import {要导入的本地文件（必须使用完整路径）} 将本地文件或文件夹导入至此\r\n"
				+ "export {\"目标名称\" 或 \"--目标序号\"（省略该项则导出当前文件夹的全部内容）} {要导出至本地的路径（必须使用完整路径）} 将指定文件或文件夹导出本地\r\n"
				+ "rm {\"目标名称\" 或 \"--目标序号\"} 删除指定文件或文件夹\r\n"
				+ "exit 退出文件管理并返回kiftd控制台\r\n"
				+ "help 显示帮助文本";

		Printer.instance.print("命令帮助：\r\n" + fsCommandTips);

		try {
			while (true) {
				Printer.instance.print("kiftd: " + currentFolder.getCurrent().getFolderName() + "$ ");
				String command = scannerProvider.getScanner().nextLine().trim();
				if (command.isEmpty()) {
					continue;
				}
				if (handleFileCommand(command, fsCommandTips)) {
					return;
				}
			}
		} catch (Exception e) {
			Printer.instance.print("错误：读取命令时出现意外，已退出文件管理功能。");
		}
	}

	private boolean handleFileCommand(String command, String fsCommandTips) {
		if (command.startsWith("cd ")) {
			gotoFolder(command.substring(3));
			return false;
		}
		if (command.startsWith("import ")) {
			doImport(command.substring(7));
			return false;
		}
		if (command.startsWith("rm ")) {
			doDelete(command.substring(3));
			return false;
		}
		if (command.startsWith("export ")) {
			doExport(command.substring(7));
			return false;
		}
		if (command.startsWith("ls ") || command.equals("ls")) {
			handleLsCommand(command);
			return false;
		}
		switch (command) {
		case "exit":
			Printer.instance.print("退出文件管理。");
			return true;
		case "help":
		case "--help":
		case "-help":
			Printer.instance.print("命令帮助：\r\n" + fsCommandTips);
			return false;
		default:
			Printer.instance.print("错误：无法识别的指令。\r\n" + fsCommandTips);
			return false;
		}
	}

	private void handleLsCommand(String command) {
		String[] oArgs = command.substring(2).trim().split(" ");
		String[] args = Arrays.stream(oArgs).filter(s -> !s.isEmpty()).toArray(String[]::new);
		if (args.length == 0) {
			showCurrentFolder(false);
		} else if (args.length == 1 && "-l".equals(args[0])) {
			showCurrentFolder(true);
		} else {
			Printer.instance.print("错误：显示当前文件夹内容失败，输入参数不正确。");
		}
	}

	private void getFolderView(String fid) throws java.sql.SQLException {
		currentFolder = FileSystemManager.getInstance().getFolderView(fid);
	}

	private void showCurrentFolder(boolean showDetailedInformation) {
		try {
			String folderId = currentFolder.getCurrent().getFolderId();
			currentFolder = FileSystemManager.getInstance().getFolderView(folderId);
		} catch (java.sql.SQLException e) {
			openFolderError();
			return;
		}
		List<FolderTreeNode> fls = currentFolder.getFolders();
		int index = 1;
		for (FolderTreeNode f : fls) {
			StringBuilder row = new StringBuilder();
			row.append("--").append(index);
			row.append("\t");
			row.append("[文件夹]");
			row.append("\t");
			row.append(f);
			if (showDetailedInformation) {
				row.append("\t").append(f.getFolderCreationDate());
				row.append("\t--\t").append(f.getFolderCreator());
			}
			Printer.instance.print(row.toString());
			index++;
		}
		List<Node> fs = currentFolder.getFiles();
		for (Node f : fs) {
			StringBuilder row = new StringBuilder();
			row.append("--").append(index);
			row.append("\t");
			row.append("[文件]");
			row.append("\t");
			row.append(f.getFileName());
			if (showDetailedInformation) {
				row.append("\t").append(f.getFileCreationDate());
				row.append("\t").append(SizeFormatUtil.formatFileSize(f.getFileSize()));
				row.append("\t").append(f.getFileCreator());
			}
			Printer.instance.print(row.toString());
			index++;
		}
		Printer.instance.print("");
	}

	private void gotoFolder(String fname) {
		fname = fname.trim();
		try {
			currentFolder = FileSystemManager.getInstance().getFolderView(currentFolder.getCurrent().getFolderId());
			String fid = getSelectFolderId(fname);
			if (fid != null) {
				getFolderView(fid);
				return;
			}
			Printer.instance.print("错误：该文件夹不存在或其不是一个文件夹（" + fname + "）。");
		} catch (java.sql.SQLException e) {
			openFolderError();
		}
	}

	public Object getPath(String path) {
		if (path.startsWith("/ROOT")) {
			String[] paths = path.split("/");
			try {
				String parent = "null";
				for (int i = 1; i < paths.length - 1; i++) {
					String folderName = paths[i];
					FolderTreeNode folder = FileSystemManager.getInstance().getFoldersByParentId(parent).stream()
							.filter(e -> e.getFolderName().equals(folderName)).findFirst().orElse(null);
					if (folder == null) {
						return null;
					}
					parent = folder.getFolderId();
				}
				String fname = paths[paths.length - 1];
				List<FolderTreeNode> folders = FileSystemManager.getInstance().getFoldersByParentId(parent);
				if (path.endsWith("/") || folders.stream().anyMatch(e -> e.getFolderName().equals(fname))) {
					return folders.stream().filter(e -> e.getFolderName().equals(fname)).findFirst().orElse(null);
				} else {
					return FileSystemManager.getInstance().selectNodesByFolderId(parent).stream()
							.filter(e -> e.getFileName().equals(fname)).findFirst().orElse(null);
				}
			} catch (Exception ignored) {
				// 路径解析或查询异常时视为未找到，返回 null 由调用方提示错误
			}
		}
		return null;
	}

	private String getSelectFolderId(String fname) {
		if ("../".equals(fname) || "..".equals(fname)) {
			if (currentFolder.getCurrent().getFolderId().equals("root")) {
				return "root";
			} else {
				return currentFolder.getCurrent().getFolderParent();
			}
		}
		if ("./".equals(fname) || ".".equals(fname)) {
			return currentFolder.getCurrent().getFolderId();
		}
		if (fname.startsWith("--")) {
			try {
				int index = Integer.parseInt(fname.substring(2));
				if (index >= 1 && index <= currentFolder.getFolders().size()) {
					return currentFolder.getFolders().get(index - 1).getFolderId();
				}
			} catch (Exception ignored) {
				// 序号参数解析失败或越界时视为非法序号，返回 null
			}
			return null;
		}
		FolderTreeNode folder = currentFolder.getFolders().stream()
				.filter(e -> e.getFolderName().equals(fname)).findFirst().orElse(null);
		return folder != null ? folder.getFolderId() : null;
	}

	private String getSelectFolderOrFileId(String fname) {
		if ("../".equals(fname) || "..".equals(fname)) {
			if (currentFolder.getCurrent().getFolderId().equals("root")) {
				return "root";
			} else {
				return currentFolder.getCurrent().getFolderParent();
			}
		}
		if ("./".equals(fname) || ".".equals(fname)) {
			return currentFolder.getCurrent().getFolderId();
		}
		if (fname.startsWith("--")) {
			try {
				int index = Integer.parseInt(fname.substring(2));
				if (index >= 1 && index <= currentFolder.getFolders().size()) {
					return currentFolder.getFolders().get(index - 1).getFolderId();
				} else {
					return currentFolder.getFiles().get(index - currentFolder.getFolders().size() - 1).getFileId();
				}
			} catch (Exception ignored) {
				// 序号参数解析失败或越界时视为非法序号，返回 null
			}
			return null;
		}
		FolderTreeNode folder = currentFolder.getFolders().stream()
				.filter(e -> e.getFolderName().equals(fname)).findFirst().orElse(null);
		if (folder != null) {
			return folder.getFolderId();
		}
		Node file = currentFolder.getFiles().stream()
				.filter(m -> m.getFileName().equals(fname)).findFirst().orElse(null);
		return file != null ? file.getFileId() : null;
	}

	public void doImport(String[] args) {
		try {
			String importTarget;
			String importPath;
			Object path;
			File target;
			if (args.length == 2) {
				importTarget = args[1];
				importPath = "/ROOT";
				path = FileSystemManager.getInstance().selectFolderById("root");
				target = new File(importTarget);
			} else if (args.length == 3 || args.length == 4) {
				importPath = args[1];
				importTarget = args[2];
				target = new File(importTarget);
				path = getPath(importPath);
			} else {
				Printer.instance.print("错误：导入失败，必须指定导入目标（示例：\"-import /ROOT/ /home/your/import/file.txt\"）。");
				return;
			}
			if (!(path instanceof FolderTreeNode)) {
				Printer.instance.print("错误：导入位置（" + importPath + "）必须是一个文件夹（示例：\"/ROOT\"）。");
				return;
			}
			String folderId = ((FolderTreeNode) path).getFolderId();
			if (!target.exists()) {
				Printer.instance.print("错误：导入失败，要导入的文件或文件夹不存在（" + importTarget + "）。");
				return;
			}
			File[] files = new File[] { target };
			String type;
			if (FileSystemManager.getInstance().hasExistsFilesOrFolders(files, folderId) > 0) {
				if (args.length == 4) {
					switch (args[3]) {
					case "-C":
						type = FileSystemManager.COVER;
						break;
					case "-B":
						type = FileSystemManager.BOTH;
						break;
					default:
						Printer.instance.print("错误：导入失败，导入路径下存在相同的文件或文件夹（请使用以下参数：[-C]覆盖 [-B]保留两者）。");
						return;
					}
				} else if (args.length == 2) {
					type = FileSystemManager.COVER;
				} else {
					Printer.instance.print("错误：导入失败，导入路径下存在相同的文件或文件夹（请增加以下参数：[-C]覆盖 [-B]保留两者）。");
					return;
				}
			} else {
				type = "cancel";
			}
			if (FileSystemManager.getInstance().importFrom(files, folderId, type)) {
				return;
			} else {
				Printer.instance.print("错误：导入失败，可能导入全部文件。");
			}
		} catch (Exception e1) {
			Printer.instance.print("错误：导入失败，出现意外错误。");
		}
	}

	private void doImport(String fpath) {
		fpath = fpath.trim();
		File f = new File(fpath);
		if (!f.exists()) {
			Printer.instance.print("错误：无法导入文件或文件夹，该目标不存在（" + fpath + "），请重新检查。");
			return;
		}
		String targetFolder = currentFolder.getCurrent().getFolderId();
		String type = "";
		File[] importFiles = new File[] { f };
		Runnable pl = null;
		try {
			if (FileSystemManager.getInstance().hasExistsFilesOrFolders(importFiles, targetFolder) > 0) {
				Printer.instance.print("提示：该路径下已经存在同名文件或文件夹（" + f.getName() + "），您希望？[C]取消 [V]覆盖 [B]保留两者");
				q: while (true) {
					String command = scannerProvider.getScanner().nextLine().trim();
					switch (command) {
					case "C":
						Printer.instance.print("导入被取消。");
						return;
					case "V":
						type = FileSystemManager.COVER;
						break q;
					case "B":
						type = FileSystemManager.BOTH;
						break q;
					default:
						Printer.instance.print("请输入C、V或B：");
						break;
					}
				}
			}
			Printer.instance.print("正在导入，请稍候...");
			pl = progressListenerFactory.create();
			FileSystemManager.getInstance().importFrom(importFiles, targetFolder, type);
			progressListenerFactory.stop(pl);
			Printer.instance.print("导入完成。");
		} catch (FilesTotalOutOfLimitException e1) {
			if (pl != null) {
				progressListenerFactory.stop(pl);
			}
			Printer.instance.print("错误：导入失败，该文件夹内的文件数目已达上限，无法导入更多文件。");
		} catch (FoldersTotalOutOfLimitException e2) {
			if (pl != null) {
				progressListenerFactory.stop(pl);
			}
			Printer.instance.print("错误：导入失败，该文件夹内的文件夹数目已达上限，无法导入更多文件夹。");
		} catch (Exception e3) {
			if (pl != null) {
				progressListenerFactory.stop(pl);
			}
			Printer.instance.print("错误：无法导入该文件（或文件夹），请重试。");
		}
	}

	public void doExport(String[] args) {
		try {
			String exportTarget;
			String exportPath;
			Object target;
			File path;
			if (args.length == 2) {
				exportPath = args[1];
				exportTarget = "/ROOT";
				path = new File(exportPath);
				target = FileSystemManager.getInstance().selectFolderById("root");
			} else if (args.length == 3 || args.length == 4) {
				exportTarget = args[1];
				exportPath = args[2];
				target = getPath(exportTarget);
				path = new File(exportPath);
			} else {
				Printer.instance.print("错误：导出失败，必须指定导出路径（示例：\"-export /ROOT/ /home/your/export/folder\"）。");
				return;
			}
			if (!path.isDirectory()) {
				Printer.instance.print("错误：导出路径（" + exportPath + "）必须指向一个已经存在的文件夹。");
				return;
			}
			if (target == null) {
				Printer.instance.print("错误：导出失败，要导出的文件或文件夹不存在（" + exportTarget + "）。");
				return;
			}
			String[] foldersId;
			String[] filesId;
			String type;
			if (target instanceof Node) {
				foldersId = new String[] {};
				filesId = new String[] { ((Node) target).getFileId() };
			} else if (target instanceof FolderTreeNode) {
				foldersId = new String[] { ((FolderTreeNode) target).getFolderId() };
				filesId = new String[] {};
			} else {
				Printer.instance.print("错误：导出失败，出现意外错误。");
				return;
			}
			if (FileSystemManager.getInstance().hasExistsFilesOrFolders(foldersId, filesId, path) > 0) {
				if (args.length == 4) {
					switch (args[3]) {
					case "-C":
						type = FileSystemManager.COVER;
						break;
					case "-B":
						type = FileSystemManager.BOTH;
						break;
					default:
						Printer.instance.print("错误：导出失败，导出路径下存在相同的文件或文件夹（请使用以下参数：[-C]覆盖 [-B]保留两者）。");
						return;
					}
				} else if (args.length == 2) {
					type = FileSystemManager.COVER;
				} else {
					Printer.instance.print("错误：导出失败，导出路径下存在相同的文件或文件夹（请增加以下参数：[-C]覆盖 [-B]保留两者）。");
					return;
				}
			} else {
				type = "cancel";
			}
			if (FileSystemManager.getInstance().exportTo(foldersId, filesId, path, type)) {
				return;
			} else {
				Printer.instance.print("错误：导出失败，可能未导出全部文件。");
			}
		} catch (Exception e1) {
			Printer.instance.print("错误：导出失败，出现意外错误。");
		}
	}

	private void doExport(String command) {
		command = command.trim();
		String[] oArgs = command.split(" ");
		String[] args = Arrays.stream(oArgs).filter(s -> !s.isEmpty()).toArray(String[]::new);
		String id;
		String path;
		Runnable pl = null;
		if (args.length == 1) {
			path = args[0];
			id = currentFolder.getCurrent().getFolderId();
		} else if (args.length == 2) {
			id = getSelectFolderOrFileId(args[0]);
			path = args[1];
		} else {
			Printer.instance.print("错误：导出失败，输入参数不正确。");
			return;
		}
		File targetPath = new File(path);
		if (targetPath.isDirectory()) {
			if (id == null) {
				Printer.instance.print("错误：导出失败，该文件或文件夹不存在（" + args[0] + "）。");
				return;
			}
			try {
				String[] foldersId;
				String[] filesId;
				String type = "";
				if (id.equals(currentFolder.getCurrent().getFolderId())
						|| currentFolder.getFolders().stream().anyMatch(e -> e.getFolderId().equals(id))) {
					foldersId = new String[] { id };
					filesId = new String[] {};
				} else if (currentFolder.getFiles().stream().anyMatch(e -> e.getFileId().equals(id))) {
					foldersId = new String[] {};
					filesId = new String[] { id };
				} else {
					Printer.instance.print("错误：要导出的文件（或文件夹）不合法，只允许在当前文件夹内的选择（" + path + "）。");
					return;
				}
				if (FileSystemManager.getInstance().hasExistsFilesOrFolders(foldersId, filesId, targetPath) > 0) {
					Printer.instance.print("提示：该路径下已经存在同名文件或文件夹（" + targetPath.getName() + "），您希望？[C]取消 [V]覆盖 [B]保留两者");
					q: while (true) {
						String command2 = scannerProvider.getScanner().nextLine().trim();
						switch (command2) {
						case "C":
							Printer.instance.print("导出被取消。");
							return;
						case "V":
							type = FileSystemManager.COVER;
							break q;
						case "B":
							type = FileSystemManager.BOTH;
							break q;
						default:
							Printer.instance.print("请输入C、V或B：");
							break;
						}
					}
				}
				Printer.instance.print("正在导出，请稍候...");
				pl = progressListenerFactory.create();
				FileSystemManager.getInstance().exportTo(foldersId, filesId, targetPath, type);
				progressListenerFactory.stop(pl);
				Printer.instance.print("导出完成。");
			} catch (Exception e1) {
				if (pl != null) {
					progressListenerFactory.stop(pl);
				}
				Printer.instance.print("错误：无法导出该文件（或文件夹），请重试。");
			}
		} else {
			Printer.instance.print("错误：导出失败，导出的目标必须是一个文件夹（" + path + "）。");
		}
	}

	public void doTransfer(String[] args) {
		short extendStoreIndex;
		File reservePath;
		if (args.length == 3) {
			try {
				extendStoreIndex = Short.parseShort(args[1]);
				reservePath = new File(args[2]);
				if (!reservePath.isDirectory()) {
					Printer.instance.print("错误：移出数据的保存路径（" + args[2] + "）必须指向一个已经存在的文件夹。");
					return;
				}
				try {
					long total = FileSystemManager.getInstance().getTotalOfNodesAtExtendStore(extendStoreIndex);
					if (total > 0) {
						FileSystemManager.getInstance().transferExtendStore(extendStoreIndex, reservePath);
						return;
					} else {
						Printer.instance.print("错误：该扩展存储区（" + args[1] + "）不存在或其内部尚无任何数据，无需执行该操作。");
						return;
					}
				} catch (java.nio.file.FileAlreadyExistsException e1) {
					Printer.instance.print("错误：目标文件夹内存在同名文件，建议选择一个空文件夹作为移出数据的保存路径。");
				} catch (Exception e2) {
					Printer.instance.print("错误：无法移出该扩展存储区内的数据，请重试。");
				}
			} catch (NumberFormatException e) {
				Printer.instance.print("错误：扩展存储区编号（" + args[1] + "）无法被识别。");
			}
		} else {
			Printer.instance.print("错误：移出失败，必须指定要移出的扩展存储区编号和移出数据的保存路径（示例：\"-transfer 1 /home/your/transfer/folder\"）。");
		}
	}

	private void doDelete(String fname) {
		fname = fname.trim();
		Runnable pl = null;
		try {
			currentFolder = FileSystemManager.getInstance().getFolderView(currentFolder.getCurrent().getFolderId());
		} catch (java.sql.SQLException e2) {
			openFolderError();
			return;
		}
		String id = getSelectFolderOrFileId(fname);
		try {
			if (currentFolder.getFolders().stream().anyMatch(e -> e.getFolderId().equals(id))) {
				if (confirmOpt("确认要删除该文件夹么？")) {
					Printer.instance.print("正在删除文件夹，请稍候...");
					pl = progressListenerFactory.create();
					if (FileSystemManager.getInstance().delete(new String[] { id }, new String[] {})) {
						Printer.instance.print("删除完毕。");
					} else {
						Printer.instance.print("删除失败，可能未能删除该文件夹，请重试。");
					}
					progressListenerFactory.stop(pl);
				} else {
					Printer.instance.print("已取消删除。");
				}
				return;
			}
			if (currentFolder.getFiles().stream().anyMatch(e -> e.getFileId().equals(id))) {
				if (confirmOpt("确认要删除该文件么？")) {
					Printer.instance.print("正在删除文件，请稍候...");
					pl = progressListenerFactory.create();
					if (FileSystemManager.getInstance().delete(new String[] {}, new String[] { id })) {
						Printer.instance.print("删除完毕。");
					} else {
						Printer.instance.print("删除失败，可能未能删除该文件，请重试。");
					}
					progressListenerFactory.stop(pl);
				} else {
					Printer.instance.print("已取消删除。");
				}
				return;
			}
		} catch (Exception e1) {
			if (pl != null) {
				progressListenerFactory.stop(pl);
			}
			Printer.instance.print("错误：无法删除文件，请重试。");
		}
		Printer.instance.print("错误：该文件或文件夹不存在（" + fname + "）。");
	}

	private boolean confirmOpt(String tip) {
		Printer.instance.print("提示：" + tip + " [Y/N]");
		while (true) {
			System.out.print("> ");
			String command = scannerProvider.getScanner().nextLine().trim();
			switch (command) {
			case "Y":
				return true;
			case "N":
				return false;
			default:
				Printer.instance.print("必须正确输入Y或N：");
				break;
			}
		}
	}

	private void openFolderError() {
		Printer.instance.print("错误：无法读取指定文件夹，是否返回根目录？[Y/N]");
		while (true) {
			System.out.print("> ");
			String command = scannerProvider.getScanner().nextLine().trim();
			switch (command) {
			case "Y":
				try {
					currentFolder = FileSystemManager.getInstance().getFolderView("root");
				} catch (java.sql.SQLException e1) {
					Printer.instance.print("错误：无法读取根目录，请尝试重新打开文件管理系统或重启kiftd。");
				}
				return;
			case "N":
				return;
			default:
				Printer.instance.print("请输入Y或N：");
				break;
			}
		}
	}

	@FunctionalInterface
	public interface ScannerProvider {
		java.util.Scanner getScanner();
	}

	public interface ProgressListenerFactory {
		Runnable create();

		void stop(Runnable listener);
	}
}

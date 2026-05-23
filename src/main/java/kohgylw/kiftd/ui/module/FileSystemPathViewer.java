package kohgylw.kiftd.ui.module;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.ui.pojo.FileSystemPath;
import kohgylw.kiftd.ui.util.PathsTable;
import kohgylw.kiftd.util.file_system_manager.FileSystemManager;

/**
 * 
 * <h2>管理文件系统路径窗口</h2>
 * <p>
 * 该窗口用于提供用户管理文件系统路径的界面功能，包括修改主文件系统路径和新增、删除、修改扩展存储区的功能。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FileSystemPathViewer extends KiftdDynamicWindow {

	private JDialog window;
	private JButton addBtn;
	private JButton changeBtn;
	private JButton removeBtn;
	private PathsTable pathsTable;
	private int maxExtendStoresNum;

	private static FileSystemPathViewer fsv;
	private static List<FileSystemPath> paths;
	private static ExecutorService worker;
	private CharsetEncoder encoder;

	private static final String INVALID_PATH_ALTER = "错误：该路径中含有程序无法识别的字符，请使用其他路径（推荐使用纯英文路径）。";

	private FileSystemPathViewer() {
		encoder = Charset.forName("ISO-8859-1").newEncoder();
		setUIFont();
		worker = Executors.newSingleThreadExecutor();
		window = new JDialog(SettingWindow.getInstance().getWindow(), "管理文件系统路径");
		window.setModal(true);
		window.setSize(600, 240);
		window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		window.setLocation(200, 200);
		window.setResizable(false);
		paths = new ArrayList<>();
		Container c = window.getContentPane();
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		addBtn = new JButton("新建 扩展存储区[Add]");
		changeBtn = new JButton("修改路径[Change]");
		removeBtn = new JButton("移除路径[Remove]");
		addBtn.setPreferredSize(new Dimension((int) (110 * proportion), (int) (35 * proportion)));
		changeBtn.setPreferredSize(new Dimension((int) (110 * proportion), (int) (35 * proportion)));
		changeBtn.setEnabled(false);
		removeBtn.setPreferredSize(new Dimension((int) (105 * proportion), (int) (35 * proportion)));
		removeBtn.setEnabled(false);
		toolBar.add(addBtn);
		toolBar.addSeparator();
		toolBar.add(changeBtn);
		toolBar.add(removeBtn);
		toolBar.addSeparator();
		c.add(toolBar, BorderLayout.NORTH);
		SettingWindow sw = SettingWindow.getInstance();
		maxExtendStoresNum = (sw.st == null) ? 0 : sw.st.getMaxExtendStoresNum();
		addBtn.addActionListener(e -> {
			disableAllButtons();
			if (sw.extendStores != null && sw.extendStores.size() < maxExtendStoresNum) {
				JFileChooser addExtendStoresChooer = new JFileChooser();
				addExtendStoresChooer.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				addExtendStoresChooer.setPreferredSize(fileChooerSize);
				addExtendStoresChooer.setDialogTitle("请选择存储路径...");
				if (addExtendStoresChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					File newExtendStores = addExtendStoresChooer.getSelectedFile();
					if (newExtendStores.isDirectory() && newExtendStores.canRead() && newExtendStores.canWrite()) {
						if (sw.extendStores.stream().anyMatch((f) -> f.getPath().equals(newExtendStores))) {
							JOptionPane.showMessageDialog(window, "错误：该路径已被其他扩展存储区占用。", "错误",
									JOptionPane.WARNING_MESSAGE);
						} else {
							String pathName = newExtendStores.getAbsolutePath();
							if (encoder.canEncode(pathName) && pathName.indexOf("\\:") < 0
									&& pathName.indexOf("\\\\") < 0) {
								Short[] indexs = sw.extendStores.stream().map((s) -> s.getIndex())
										.toArray(Short[]::new);
								short index = 1;
								while (Arrays.binarySearch(indexs, index) >= 0) {
									index++;
								}
								FileSystemPath nfsp = new FileSystemPath();
								nfsp.setIndex(index);
								nfsp.setType(FileSystemPath.EXTEND_STORES_NAME);
								nfsp.setPath(addExtendStoresChooer.getSelectedFile());
								sw.extendStores.add(nfsp);
							} else {
								JOptionPane.showMessageDialog(window, INVALID_PATH_ALTER, "错误",
										JOptionPane.WARNING_MESSAGE);
							}
						}
					} else {
						JOptionPane.showMessageDialog(window, "错误：该路径不可用，必须选择可读写的文件夹。", "错误",
								JOptionPane.WARNING_MESSAGE);
					}
				}
			}
			enableAllButtons();
			refresh();
		});
		changeBtn.addActionListener(e -> {
			disableAllButtons();
			if (JOptionPane.showConfirmDialog(window,
					"确认要修改该存储区路径么？警告：如需保留该存储区内的全部数据，应先将该存储区原路径指定的文件夹移动到新位置，再将移动后的文件夹设置为该存储区的新路径。否则，该存储区内的数据将全部丢失。",
					"修改路径", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				short index = pathsTable.getSelectFileSystemIndex();
				if (index == 0) {
					JFileChooser mainFileSystemPathChooer = new JFileChooser();
					mainFileSystemPathChooer.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					mainFileSystemPathChooer.setPreferredSize(fileChooerSize);
					if (sw.st != null) {
						File fileSystemPath = new File(sw.st.getFileSystemPath());
						if (fileSystemPath.isDirectory()) {
							mainFileSystemPathChooer.setCurrentDirectory(fileSystemPath);
						}
					}
					mainFileSystemPathChooer.setDialogTitle("请选择主文件系统存储路径");
					if (mainFileSystemPathChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						File selectPath = mainFileSystemPathChooer.getSelectedFile();
						if (selectPath.isDirectory() && selectPath.canWrite() && selectPath.canRead()) {
							if (!sw.extendStores.stream()
									.anyMatch((f) -> f.getPath().equals(selectPath))) {
								String pathName = selectPath.getAbsolutePath();
								if (new File(ConfigureReader.instance().getInitFileSystemPath()).equals(selectPath)
										|| (encoder.canEncode(pathName) && pathName.indexOf("\\:") < 0
												&& pathName.indexOf("\\\\") < 0)) {
									sw.chooserPath = mainFileSystemPathChooer.getSelectedFile();
								} else {
									JOptionPane.showMessageDialog(window, INVALID_PATH_ALTER, "错误",
											JOptionPane.WARNING_MESSAGE);
								}
							} else {
								JOptionPane.showMessageDialog(window, "错误：该路径已被某个扩展存储区占用。", "错误",
										JOptionPane.WARNING_MESSAGE);
							}
						} else {
							JOptionPane.showMessageDialog(window, "错误：该路径不可用，必须选择可读写的文件夹。", "错误",
									JOptionPane.WARNING_MESSAGE);
						}
					}
				} else {
					JFileChooser mainFileSystemPathChooer = new JFileChooser();
					mainFileSystemPathChooer.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					mainFileSystemPathChooer.setPreferredSize(fileChooerSize);
					FileSystemPath fsp = null;
					for (int i = 0; i < sw.extendStores.size(); i++) {
						if (sw.extendStores.get(i).getIndex() == index) {
							fsp = sw.extendStores.get(i);
							mainFileSystemPathChooer.setCurrentDirectory(fsp.getPath());
							mainFileSystemPathChooer.setDialogTitle("请选择扩展存储区路径");
							if (mainFileSystemPathChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
								disableAllButtons();
								File selectPath = mainFileSystemPathChooer.getSelectedFile();
								if (selectPath.isDirectory() && selectPath.canWrite() && selectPath.canRead()) {
									if (fsp.getPath().equals(selectPath) || !sw.extendStores.stream()
											.anyMatch((f) -> f.getPath().equals(selectPath))) {
										String pathName = selectPath.getAbsolutePath();
										if (encoder.canEncode(pathName) && pathName.indexOf("\\:") < 0
												&& pathName.indexOf("\\\\") < 0) {
											fsp.setPath(mainFileSystemPathChooer.getSelectedFile());
										} else {
											JOptionPane.showMessageDialog(window, INVALID_PATH_ALTER, "错误",
													JOptionPane.WARNING_MESSAGE);
										}
									} else {
										JOptionPane.showMessageDialog(window, "错误：该路径已被其他扩展存储区占用。", "错误",
												JOptionPane.WARNING_MESSAGE);
									}
								} else {
									JOptionPane.showMessageDialog(window, "错误：该路径不可用，必须选择可读写的文件夹。", "错误",
											JOptionPane.WARNING_MESSAGE);
								}
							}
							break;
						}
					}
				}
			}
			enableAllButtons();
			refresh();
		});
		removeBtn.addActionListener(e -> {
			disableAllButtons();
			if (JOptionPane.showConfirmDialog(window,
					"确认要移除该扩展存储区么？警告：移除后，该存储区内原先存放的数据将丢失，且设置生效后不可恢复。", "移除扩展存储区",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				short index = pathsTable.getSelectFileSystemIndex();
				for (int i = 0; i < sw.extendStores.size(); i++) {
					if (sw.extendStores.get(i).getIndex() == index) {
						final int removeItemIndex = i;
						try {
							long total = FileSystemManager.getInstance().getTotalOfNodesAtExtendStore(index);
							if (total > 0) {
								int choice = JOptionPane.showConfirmDialog(window,
										"是否立即将该扩展存储区内的数据全部移出以便留档？如果您确定要移除该扩展存储区，推荐执行该操作。注意：该操作即时生效，无论是否应用新设置均无法回退。",
										"移出", JOptionPane.YES_NO_CANCEL_OPTION);
								switch (choice) {
								case 0:
									JFileChooser transferDirChooer = new JFileChooser();
									transferDirChooer.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
									transferDirChooer.setPreferredSize(fileChooerSize);
									transferDirChooer.setDialogTitle("请选择移出数据的保存路径...");
									if (transferDirChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
										File transferDir = transferDirChooer.getSelectedFile();
										worker.execute(() -> {
											FSProgressDialog fsd = FSProgressDialog.getNewInstance(window);
											Thread t = new Thread(() -> fsd.show());
											t.start();
											try {
												boolean r = FileSystemManager.getInstance()
														.transferExtendStore(index, transferDir);
												SwingUtilities.invokeLater(() -> {
													fsd.close();
													if (r) {
														sw.extendStores.remove(removeItemIndex);
														refresh();
													} else {
														JOptionPane.showMessageDialog(window,
																"移出文件时失败，该操作已被中断，未能移出全部数据。", "错误",
																JOptionPane.ERROR_MESSAGE);
													}
												});
											} catch (FileAlreadyExistsException e1) {
												SwingUtilities.invokeLater(() -> {
													fsd.close();
													JOptionPane.showMessageDialog(window,
															"目标文件夹内存在同名文件，建议选择一个空文件夹作为移出数据的保存路径。", "错误",
															JOptionPane.ERROR_MESSAGE);
												});
											} catch (Exception e1) {
												SwingUtilities.invokeLater(() -> {
													fsd.close();
													JOptionPane.showMessageDialog(window,
															"出现意外错误，无法统计扩展存储区数据，请重启应用后重试。", "错误",
															JOptionPane.ERROR_MESSAGE);
												});
											}
										});
									}
									break;
								case 1:
									sw.extendStores.remove(i);
									break;
								case 2:
								default:
									break;
								}
							} else {
								sw.extendStores.remove(i);
							}
						} catch (Exception e1) {
							JOptionPane.showMessageDialog(window, "出现意外错误，无法统计扩展存储区数据，请重启应用后重试。", "错误",
									JOptionPane.ERROR_MESSAGE);
						}
						break;
					}
				}
			}
			enableAllButtons();
			refresh();
		});
		pathsTable = new PathsTable();
		pathsTable.setRowHeight((int) (16 * proportion));
		pathsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane mianPane = new JScrollPane(pathsTable);
		pathsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int index = pathsTable.getSelectFileSystemIndex();
				if (index < 0) {
					changeBtn.setEnabled(false);
					removeBtn.setEnabled(false);
				} else {
					if (index == 0) {
						removeBtn.setEnabled(false);
					} else {
						removeBtn.setEnabled(true);
					}
					changeBtn.setEnabled(true);
				}
			}
		});
		c.add(mianPane);
		modifyComponentSize(window);
	}

	private void refresh() {
		SettingWindow sw = SettingWindow.getInstance();
		paths.clear();
		FileSystemPath mainfsp = new FileSystemPath();
		mainfsp.setType(FileSystemPath.MAIN_FILE_SYSTEM_NAME);
		mainfsp.setPath(sw.chooserPath);
		mainfsp.setIndex((short) 0);
		paths.add(mainfsp);
		if (sw.extendStores != null) {
			paths.addAll(sw.extendStores);
		}
		pathsTable.updateValues(paths);
	}

	public void show() {
		disableAllButtons();
		refresh();
		if (paths == null || paths.size() == 0) {
			Printer.instance.print("错误：无法获取文件系统设置，请手动检查配置文件并重启应用。");
		} else {
			enableAllButtons();
			window.setVisible(true);
		}
	}

	public static FileSystemPathViewer getInstance() {
		if (fsv == null) {
			fsv = new FileSystemPathViewer();
		}
		return fsv;
	}

	private void disableAllButtons() {
		addBtn.setEnabled(false);
		changeBtn.setEnabled(false);
		removeBtn.setEnabled(false);
	}

	private void enableAllButtons() {
		SettingWindow sw = SettingWindow.getInstance();
		if (sw.extendStores != null && sw.extendStores.size() < maxExtendStoresNum) {
			addBtn.setEnabled(true);
		} else {
			addBtn.setEnabled(false);
		}
		short index = pathsTable.getSelectFileSystemIndex();
		if (index < 0) {
			changeBtn.setEnabled(false);
		}
	}
}

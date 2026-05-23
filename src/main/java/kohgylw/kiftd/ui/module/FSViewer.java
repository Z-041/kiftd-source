package kohgylw.kiftd.ui.module;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.ref.Cleaner;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.exception.FilesTotalOutOfLimitException;
import kohgylw.kiftd.server.exception.FoldersTotalOutOfLimitException;
import kohgylw.kiftd.ui.util.FilesTable;
import kohgylw.kiftd.util.file_system_manager.FileSystemManager;
import kohgylw.kiftd.util.file_system_manager.pojo.Folder;
import kohgylw.kiftd.util.file_system_manager.pojo.FolderView;

/**
 * 
 * <h2>文件管理窗口</h2>
 * <p>
 * 该窗口是文件导入管理工具的图形化操作页面，应由主界面上的“文件”按钮开启。
 * 其中除了包含页面内容外，也封装了一些文件管理必要的操作过程，与文件管理器工具直接对接。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FSViewer extends KiftdDynamicWindow {

	private JDialog window;
	private JButton homeBtn;
	private JButton backToParentFolder;
	private JButton importBtn;
	private JButton exportBtn;
	private JButton deleteBtn;
	private JButton refreshBtn;
	private FilesTable filesTable;

	private static FSViewer fsv;
	private FolderView currentView;
	private ExecutorService worker;

	private static final Cleaner CLEANER = Cleaner.create();

	private static class WorkerCleanup implements Runnable {
		private final ExecutorService workerRef;

		WorkerCleanup(ExecutorService workerRef) {
			this.workerRef = workerRef;
		}

		@Override
		public void run() {
			if (workerRef != null && !workerRef.isShutdown()) {
				workerRef.shutdown();
			}
		}
	}

	private final Cleaner.Cleanable cleanable;

	private FSViewer() throws SQLException {
		setUIFont();
		worker = Executors.newSingleThreadExecutor();
		this.cleanable = CLEANER.register(this, new WorkerCleanup(worker));
		window = new JDialog(ServerUIModule.getMainWindow(), "kiftd-ROOT");
		window.setSize(750, 450);
		window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		window.setLocation(150, 150);
		window.setResizable(true);
		Container c = window.getContentPane();
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		homeBtn = new JButton("根目录[/Root]");
		homeBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (11 * proportion)));
		backToParentFolder = new JButton("上一级[^]");
		backToParentFolder.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (11 * proportion)));
		importBtn = new JButton("导入[<-]");
		importBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (11 * proportion)));
		exportBtn = new JButton("导出[->]");
		exportBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (11 * proportion)));
		deleteBtn = new JButton("删除[X]");
		deleteBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (11 * proportion)));
		refreshBtn = new JButton("刷新[*]");
		refreshBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (11 * proportion)));
		homeBtn.setMargin(new Insets(4, 10, 4, 10));
		homeBtn.setEnabled(false);
		backToParentFolder.setMargin(new Insets(4, 10, 4, 10));
		backToParentFolder.setEnabled(false);
		importBtn.setMargin(new Insets(4, 10, 4, 10));
		exportBtn.setMargin(new Insets(4, 10, 4, 10));
		exportBtn.setEnabled(false);
		deleteBtn.setMargin(new Insets(4, 10, 4, 10));
		deleteBtn.setEnabled(false);
		refreshBtn.setMargin(new Insets(4, 10, 4, 10));
		toolBar.add(homeBtn);
		toolBar.add(backToParentFolder);
		toolBar.addSeparator();
		toolBar.add(importBtn);
		toolBar.addSeparator();
		toolBar.add(exportBtn);
		toolBar.add(deleteBtn);
		toolBar.addSeparator();
		toolBar.add(refreshBtn);
		c.add(toolBar, BorderLayout.NORTH);
		homeBtn.addActionListener(e -> {
			disableAllButtons();
			try {
				getFolderView("root");
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(window, "出现意外错误：无法读取文件列表，请重试或重启应用。", "错误", JOptionPane.ERROR_MESSAGE);
			}
			enableAllButtons();
		});
		backToParentFolder.addActionListener(e -> {
			disableAllButtons();
			try {
				getFolderView(currentView.getCurrent().getFolderParent());
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(window, "出现意外错误：无法读取文件列表，请重试或重启应用。", "错误", JOptionPane.ERROR_MESSAGE);
			}
			enableAllButtons();
		});
		importBtn.addActionListener(e -> {
			disableAllButtons();
			JFileChooser importChooer = new JFileChooser();
			importChooer.setMultiSelectionEnabled(true);
			importChooer.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			importChooer.setPreferredSize(fileChooerSize);
			importChooer.setDialogTitle("请选择...");
			if (importChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				worker.execute(() -> {
					doImport(importChooer.getSelectedFiles());
					enableAllButtons();
				});
			} else {
				enableAllButtons();
			}
		});
		exportBtn.addActionListener(e -> {
			JFileChooser exportChooer = new JFileChooser();
			exportChooer.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			exportChooer.setPreferredSize(fileChooerSize);
			exportChooer.setDialogTitle("导出到...");
			if (exportChooer.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				disableAllButtons();
				worker.execute(() -> {
					File path = exportChooer.getSelectedFile();
					int[] selected = filesTable.getSelectedRows();
					List<String> selectedNodes = new ArrayList<>();
					List<String> selectedFolders = new ArrayList<>();
					int borderIndex = currentView.getFolders().size();
					for (int i : selected) {
						if (i < borderIndex) {
							selectedFolders.add(currentView.getFolders().get(i).getFolderId());
						} else {
							selectedNodes.add(currentView.getFiles().get(i - borderIndex).getFileId());
						}
					}
					String[] folders = selectedFolders.toArray(new String[0]);
					String[] nodes = selectedNodes.toArray(new String[0]);
					int exi = 0;
					try {
						exi = FileSystemManager.getInstance().hasExistsFilesOrFolders(folders, nodes, path);
					} catch (Exception e2) {
						SwingUtilities.invokeLater(() -> {
							JOptionPane.showMessageDialog(window, "出现意外错误，无法导出文件，请重试。", "错误", JOptionPane.ERROR_MESSAGE);
						});
						refresh();
						enableAllButtons();
						return;
					}
					final String type;
					if (exi > 0) {
						int choice = JOptionPane.showConfirmDialog(window,
								"该路径存在" + exi + "个同名文件或文件夹，您希望覆盖它们么？（\u201C是\u201D覆盖，\u201C否\u201D保留两者，\u201C取消\u201D终止导入）", "导入",
								JOptionPane.YES_NO_CANCEL_OPTION);
						switch (choice) {
						case JOptionPane.YES_OPTION:
							type = FileSystemManager.COVER;
							break;
						case JOptionPane.NO_OPTION:
							type = FileSystemManager.BOTH;
							break;
						case JOptionPane.CANCEL_OPTION:
						default:
							type = "CANCEL";
							enableAllButtons();
							return;
						}
					} else {
						type = null;
					}
					FSProgressDialog fsd = FSProgressDialog.getNewInstance(window);
					Thread t = new Thread(() -> fsd.show());
					t.start();
					try {
						FileSystemManager.getInstance().exportTo(folders, nodes, path, type);
						SwingUtilities.invokeLater(() -> fsd.close());
					} catch (Exception e1) {
						SwingUtilities.invokeLater(() -> {
							fsd.close();
							Printer.instance.print(e1.toString());
							JOptionPane.showMessageDialog(window, "导出文件时失败，该操作已被中断，未能全部导出。", "错误",
									JOptionPane.ERROR_MESSAGE);
						});
					}
					refresh();
					enableAllButtons();
				});
			}
		});
		deleteBtn.addActionListener(e -> {
			disableAllButtons();
			if (JOptionPane.showConfirmDialog(window, "确认要删除这些文件么？警告：该操作无法恢复。", "删除",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				int[] selected = filesTable.getSelectedRows();
				worker.execute(() -> {
					List<String> selectedNodes = new ArrayList<>();
					List<String> selectedFolders = new ArrayList<>();
					int borderIndex = currentView.getFolders().size();
					for (int i : selected) {
						if (i < borderIndex) {
							selectedFolders.add(currentView.getFolders().get(i).getFolderId());
						} else {
							selectedNodes.add(currentView.getFiles().get(i - borderIndex).getFileId());
						}
					}
					FSProgressDialog fsd = FSProgressDialog.getNewInstance(window);
					Thread t = new Thread(() -> fsd.show());
					t.start();
					try {
						FileSystemManager.getInstance().delete(selectedFolders.toArray(new String[0]),
								selectedNodes.toArray(new String[0]));
						SwingUtilities.invokeLater(() -> fsd.close());
					} catch (Exception e1) {
						SwingUtilities.invokeLater(() -> {
							fsd.close();
							Printer.instance.print(e1.toString());
							JOptionPane.showMessageDialog(window, "删除文件时失败，该操作已被中断，未能全部删除。", "错误",
									JOptionPane.ERROR_MESSAGE);
						});
					}
					refresh();
					enableAllButtons();
				});
			} else {
				enableAllButtons();
			}
		});
		refreshBtn.addActionListener(e -> {
			disableAllButtons();
			refresh();
			enableAllButtons();
		});
		filesTable = new FilesTable();
		filesTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (12 * proportion)));
		filesTable.setRowHeight((int) (20 * proportion));
		JScrollPane mianPane = new JScrollPane(filesTable);
		filesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (filesTable.getSelectedRows().length > 0) {
					exportBtn.setEnabled(true);
					deleteBtn.setEnabled(true);
				} else {
					exportBtn.setEnabled(false);
					deleteBtn.setEnabled(false);
				}
			}
		});
		filesTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				disableAllButtons();
				worker.execute(() -> {
					Object i = filesTable.getDoubleClickItem(e);
					if (i != null) {
						if (i instanceof Folder) {
							Folder f = (Folder) i;
							try {
								getFolderView(f.getFolderId());
							} catch (Exception e1) {
								Printer.instance.print(e.toString());
							}
						}
					}
					enableAllButtons();
				});
			}
		});
		DropTargetListener dtl = new DropTargetListener() {
			@Override
			public void dropActionChanged(DropTargetDragEvent dtde) {
			}

			@Override
			public void drop(DropTargetDropEvent dtde) {
				if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					try {
						Object dropTarget = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						@SuppressWarnings("unchecked")
						List<File> files = (List<File>) dropTarget;
						dtde.dropComplete(true);
						worker.execute(() -> {
							disableAllButtons();
							doImport(files.toArray(new File[0]));
							enableAllButtons();
						});
					} catch (Exception e) {
						Printer.instance.print(e.toString());
						refresh();
					}
				}
			}

			@Override
			public void dragOver(DropTargetDragEvent dtde) {
			}

			@Override
			public void dragExit(DropTargetEvent dte) {
			}

			@Override
			public void dragEnter(DropTargetDragEvent dtde) {
			}
		};
		window.setDropTarget(new DropTarget(window, DnDConstants.ACTION_COPY_OR_MOVE, dtl));
		c.add(mianPane);
		modifyComponentSize(window);
	}

	private void refresh() {
		try {
			getFolderView(currentView.getCurrent().getFolderId());
		} catch (Exception e1) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(window, "无法刷新文件列表，请重试或返回根目录。", "错误", JOptionPane.ERROR_MESSAGE);
			});
		}
	}

	public void show() {
		SwingUtilities.invokeLater(() -> {
			disableAllButtons();
			try {
				if (currentView == null) {
					getFolderView("root");
				} else {
					refresh();
				}
				enableAllButtons();
				window.setVisible(true);
			} catch (Exception e) {
				Printer.instance.print(e.toString());
				Printer.instance.print("错误：无法打开文件系统，该文件系统可能正在被另一个kiftd占用。");
			}
		});
	}

	private void getFolderView(String folderId) throws Exception {
		try {
			currentView = FileSystemManager.getInstance().getFolderView(folderId);
			long maxTotalNum = currentView.getFiles().size() + currentView.getFolders().size();
			if (maxTotalNum > FilesTable.MAX_LIST_LIMIT) {
				JOptionPane.showMessageDialog(window,
						"文件夹列表的长度已超过最大限值（" + FilesTable.MAX_LIST_LIMIT + "），只能显示前" + FilesTable.MAX_LIST_LIMIT + "行。",
						"警告", JOptionPane.WARNING_MESSAGE);
			}
			if (currentView != null && currentView.getCurrent() != null) {
				filesTable.updateValues(currentView.getFolders(), currentView.getFiles());
				window.setTitle("kiftd-" + currentView.getCurrent().getFolderName());
			} else {
				getFolderView("root");
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public static FSViewer getInstance() throws SQLException {
		if (fsv == null) {
			fsv = new FSViewer();
		}
		return fsv;
	}

	private void doImport(File[] files) {
		int exi = 0;
		String folderId = currentView.getCurrent().getFolderId();
		try {
			exi = FileSystemManager.getInstance().hasExistsFilesOrFolders(files, folderId);
		} catch (SQLException e1) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(window, "出现意外错误，无法导入文件，请刷新或重启应用后重试。", "错误", JOptionPane.ERROR_MESSAGE);
			});
			refresh();
			return;
		}
		final String type;
		if (exi > 0) {
			int choice = JOptionPane.showConfirmDialog(window,
					"该路径存在" + exi + "个同名文件或文件夹，您希望覆盖它们么？（\u201C是\u201D覆盖，\u201C否\u201D保留两者，\u201C取消\u201D终止导入）", "导入",
					JOptionPane.YES_NO_CANCEL_OPTION);
			switch (choice) {
			case 0:
				type = FileSystemManager.COVER;
				break;
			case 1:
				type = FileSystemManager.BOTH;
				break;
			case 2:
			default:
				type = "CANCEL";
				return;
			}
		} else {
			type = null;
		}
		FSProgressDialog fsd = FSProgressDialog.getNewInstance(window);
		Thread t = new Thread(() -> fsd.show());
		t.start();
		try {
			FileSystemManager.getInstance().importFrom(files, folderId, type);
		} catch (FoldersTotalOutOfLimitException e1) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(window, "导入失败，该文件夹内的文件夹数目已达上限，无法导入更多文件夹。", "错误",
						JOptionPane.ERROR_MESSAGE);
			});
		} catch (FilesTotalOutOfLimitException e2) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(window, "导入失败，该文件夹内的文件数目已达上限，无法导入更多文件。", "错误",
						JOptionPane.ERROR_MESSAGE);
			});
		} catch (Exception e3) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(window, "导入失败，无法完成导入，该操作已被中断。", "错误", JOptionPane.ERROR_MESSAGE);
			});
		}
		SwingUtilities.invokeLater(() -> {
			fsd.close();
			refresh();
		});
	}

	private void disableAllButtons() {
		homeBtn.setEnabled(false);
		backToParentFolder.setEnabled(false);
		importBtn.setEnabled(false);
		exportBtn.setEnabled(false);
		deleteBtn.setEnabled(false);
		refreshBtn.setEnabled(false);
	}

	private void enableAllButtons() {
		refreshBtn.setEnabled(true);
		importBtn.setEnabled(true);
		if (filesTable.getSelectedRows().length > 0) {
			exportBtn.setEnabled(true);
			deleteBtn.setEnabled(true);
		}
		if (currentView != null && !"null".equals(currentView.getCurrent().getFolderParent())) {
			backToParentFolder.setEnabled(true);
			homeBtn.setEnabled(true);
		}
	}

}

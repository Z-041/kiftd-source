package kohgylw.kiftd.ui.module;

import javax.imageio.*;
import java.io.*;
import java.sql.SQLException;
import java.awt.event.*;
import javax.swing.border.*;
import java.awt.*;
import javax.swing.event.*;

import javax.swing.*;
import java.text.*;
import java.util.*;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.ui.callback.*;

/**
 *
 * <h2>服务器主界面模块</h2>
 * <p>
 * 该类是kiftd服务器的主控制台窗口（Swing GUI），提供服务器启动/停止/重启、
 * 文件管理、系统设置等操作界面。使用单例模式确保同一时间只有一个控制台实例，
 * 支持系统托盘图标和后台运行。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class ServerUIModule extends KiftdDynamicWindow {

	private JFrame window;
	private SystemTray tray;
	private TrayIcon trayIcon;
	private JTextArea output;
	private static ServerUIModule instance;
	private SettingWindow sw;
	private FSViewer fsv;
	private OnCloseServer cs;
	private OnStartServer ss;
	private GetServerStatus st;
	private GetServerTime ti;
	private JButton start;
	private JButton stop;
	private JButton resatrt;
	private JButton setting;
	private JButton fileIOUtil;
	private JButton exit;
	private JLabel serverStatusLab;
	private JLabel portStatusLab;
	private JLabel logLevelLab;
	private JLabel bufferSizeLab;
	private JLabel statusLed;
	private static final String S_STOP = "停止";
	private static final String S_START = "运行中";
	private static final String S_STARTING = "启动中...";
	private static final String S_STOPPING = "停止中...";
	protected static final String L_ALL = "记录全部(ALL)";
	protected static final String L_EXCEPTION = "仅异常(EXCEPTION)";
	protected static final String L_NONE = "不记录(NONE)";
	private SimpleDateFormat sdf;
	private final int OriginSize_Width = 520;
	private final int OriginSize_Height = 600;
	private MenuItem filesViewer;
	private static final Color LED_GREEN = new Color(0x2ecc71);
	private static final Color LED_GRAY = new Color(0xbdc3c7);
	private static final Color LED_ORANGE = new Color(0xf39c12);
	private static final Color LABEL_COLOR = new Color(0x555555);
	private static final Color BUTTON_BG = new Color(0xf8f9fa);

	private ServerUIModule() throws Exception {
		setUIFont();
		window = new JFrame("Cloudflow \u2014 \u63a7\u5236\u53f0");
		window.setSize(OriginSize_Width, OriginSize_Height);
		window.setLocation(100, 100);
		window.setMinimumSize(new Dimension(450, 500));
		try {
			window.setIconImage(
					ImageIO.read(this.getClass().getResourceAsStream("/kohgylw/kiftd/ui/resource/icon.png")));
		} catch (NullPointerException ex) {
		} catch (IOException ex2) {
		}
		if (SystemTray.isSupported()) {
			window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
			tray = SystemTray.getSystemTray();
			String iconType = "/kohgylw/kiftd/ui/resource/icon_tray.png";
			if (System.getProperty("os.name").toLowerCase().indexOf("window") >= 0) {
				iconType = "/kohgylw/kiftd/ui/resource/icon_tray_w.png";
			}
			trayIcon = new TrayIcon(ImageIO.read(this.getClass().getResourceAsStream(iconType)));
			trayIcon.setToolTip("\u4e91\u6d41-Cloudflow");
			trayIcon.setImageAutoSize(true);
			final PopupMenu pMenu = new PopupMenu();
			final MenuItem exitItem = new MenuItem("\u9000\u51fa(Exit)");
			filesViewer = new MenuItem("\u6587\u4ef6...(Files)");
			final MenuItem show = new MenuItem("\u663e\u793a\u4e3b\u7a97\u53e3(Show)");
			trayIcon.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
						showWindow();
					}
				}
			});
			exitItem.addActionListener(e -> exitApp());
			filesViewer.addActionListener(e -> {
				filesViewer.setEnabled(false);
				fileIOUtil.setEnabled(false);
				Thread t = new Thread(() -> {
					try {
						fsv = FSViewer.getInstance();
						fsv.show();
					} catch (SQLException e1) {
						SwingUtilities.invokeLater(() -> {
							JOptionPane.showMessageDialog(window, "\u9519\u8bef\uff1a\u65e0\u6cd5\u6253\u5f00\u6587\u4ef6\uff0c\u6587\u4ef6\u7cfb\u7edf\u53ef\u80fd\u5df2\u635f\u574f\uff0c\u60a8\u53ef\u4ee5\u5c1d\u8bd5\u91cd\u542f\u5e94\u7528\u3002", "\u9519\u8bef",
									JOptionPane.ERROR_MESSAGE);
						});
					}
					SwingUtilities.invokeLater(() -> {
						filesViewer.setEnabled(true);
						fileIOUtil.setEnabled(true);
					});
				});
				t.start();
			});
			show.addActionListener(e -> showWindow());
			pMenu.add(exitItem);
			pMenu.addSeparator();
			pMenu.add(filesViewer);
			pMenu.add(show);
			trayIcon.setPopupMenu(pMenu);
			tray.add(trayIcon);
		} else {
			window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			window.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					exitApp();
				}
			});
		}
		buildLayout();
		bindEvents();
		modifyComponentSize(window);
	}

	private void buildLayout() {
		Container root = window.getContentPane();
		root.setLayout(new BorderLayout(0, 0));

		JPanel titleBar = buildTitleBar();
		root.add(titleBar, BorderLayout.NORTH);

		JPanel centerArea = new JPanel();
		centerArea.setLayout(new BoxLayout(centerArea, BoxLayout.Y_AXIS));
		centerArea.setBorder(new EmptyBorder(6, 10, 6, 10));

		JPanel dashboard = buildDashboard();
		dashboard.setMaximumSize(new Dimension(Integer.MAX_VALUE, dashboard.getPreferredSize().height));
		centerArea.add(dashboard);
		centerArea.add(Box.createVerticalStrut(8));

		JPanel buttonBar = buildButtonBar();
		buttonBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonBar.getPreferredSize().height));
		centerArea.add(buttonBar);
		centerArea.add(Box.createVerticalStrut(8));

		JPanel logPanel = buildLogPanel();
		centerArea.add(logPanel);

		root.add(centerArea, BorderLayout.CENTER);

		JPanel statusBar = buildStatusBar();
		root.add(statusBar, BorderLayout.SOUTH);
	}

	private JPanel buildTitleBar() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(new EmptyBorder(12, 16, 6, 16));
		panel.setBackground(Color.WHITE);

		JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		titleRow.setOpaque(false);
		JLabel title = new JLabel("Cloudflow");
		title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (26 * proportion)));
		title.setForeground(new Color(0x2c3e50));
		titleRow.add(title);
		panel.add(titleRow);

		JPanel subRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		subRow.setOpaque(false);
		JLabel subtitle = new JLabel("\u670d\u52a1\u5668\u63a7\u5236\u53f0");
		subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (12 * proportion)));
		subtitle.setForeground(LABEL_COLOR);
		subRow.add(subtitle);
		panel.add(subRow);

		JSeparator sep = new JSeparator();
		sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		panel.add(sep);

		return panel;
	}

	private JPanel buildDashboard() {
		JPanel panel = new JPanel(new GridLayout(1, 4, 4, 0));
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(
						BorderFactory.createLineBorder(new Color(0xdee2e6)),
						"\u670d\u52a1\u5668\u72b6\u6001",
						TitledBorder.LEFT,
						TitledBorder.TOP,
						new Font(Font.SANS_SERIF, Font.BOLD, (int) (11 * proportion)),
						new Color(0x495057)),
				new EmptyBorder(4, 6, 4, 6)));
		panel.setBackground(Color.WHITE);

		panel.add(createStatusCard("\u670d\u52a1\u5668", serverStatusLab = new JLabel("--")));
		panel.add(createStatusCard("\u7aef\u53e3\u53f7", portStatusLab = new JLabel("--")));
		panel.add(createStatusCard("\u65e5\u5fd7\u7b49\u7ea7", logLevelLab = new JLabel("--")));
		panel.add(createStatusCard("\u7f13\u51b2\u533a", bufferSizeLab = new JLabel("--")));

		return panel;
	}

	private JPanel createStatusCard(String label, JLabel valueLabel) {
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(Color.WHITE);
		card.setBorder(new EmptyBorder(2, 4, 2, 4));

		JLabel title = new JLabel(label);
		title.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (10 * proportion)));
		title.setForeground(LABEL_COLOR);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.add(title);

		card.add(Box.createVerticalStrut(2));

		valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (13 * proportion)));
		valueLabel.setForeground(new Color(0x2c3e50));
		valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.add(valueLabel);

		return card;
	}

	private JPanel buildButtonBar() {
		JPanel outer = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
		outer.setOpaque(false);

		start = createButton("\u25b6 \u542f\u52a8");
		stop = createButton("\u25a0 \u505c\u6b62");
		resatrt = createButton("\u21bb \u91cd\u542f");
		fileIOUtil = createButton("\uD83D\uDCC1 \u6587\u4ef6");
		setting = createButton("\u2699 \u8bbe\u7f6e");
		exit = createButton("\u2715 \u9000\u51fa");

		outer.add(start);
		outer.add(stop);
		outer.add(resatrt);
		outer.add(fileIOUtil);
		outer.add(setting);
		outer.add(exit);

		return outer;
	}

	private JButton createButton(String text) {
		JButton btn = new JButton(text);
		btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (12 * proportion)));
		btn.setFocusPainted(false);
		btn.setMargin(new Insets(4, 8, 4, 8));
		return btn;
	}

	private JPanel buildLogPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 2));
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(
						BorderFactory.createLineBorder(new Color(0xdee2e6)),
						"\u8f93\u51fa\u65e5\u5fd7",
						TitledBorder.LEFT,
						TitledBorder.TOP,
						new Font(Font.SANS_SERIF, Font.BOLD, (int) (11 * proportion)),
						new Color(0x495057)),
				new EmptyBorder(0, 2, 2, 2)));

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(0, 2, 0, 2));
		JButton clearBtn = new JButton("\u6e05\u7a7a");
		clearBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (10 * proportion)));
		clearBtn.setFocusPainted(false);
		clearBtn.setMargin(new Insets(1, 6, 1, 6));
		clearBtn.addActionListener(e -> output.setText(""));
		header.add(clearBtn, BorderLayout.EAST);
		panel.add(header, BorderLayout.NORTH);

		output = new JTextArea();
		output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, (int) (12 * proportion)));
		output.setLineWrap(true);
		output.setEditable(false);
		output.setForeground(new Color(0x333333));
		output.setBackground(new Color(0xfafafa));
		output.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				SwingUtilities.invokeLater(() -> {
					if (output.getLineCount() >= 1000) {
						int end = 0;
						try {
							end = output.getLineEndOffset(100);
						} catch (Exception exc) {
						}
						output.replaceRange("", 0, end);
					}
					output.setCaretPosition(output.getText().length());
				});
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				SwingUtilities.invokeLater(() -> {
					output.selectAll();
					output.setCaretPosition(output.getSelectedText().length());
					output.requestFocus();
				});
			}
		});
		JScrollPane scrollPane = new JScrollPane(output);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel buildStatusBar() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
		panel.setBorder(new CompoundBorder(
				new MatteBorder(1, 0, 0, 0, new Color(0xdee2e6)),
				new EmptyBorder(2, 12, 2, 12)));
		panel.setBackground(new Color(0xf8f9fa));

		statusLed = new JLabel("\u25cf");
		statusLed.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (14 * proportion)));
		statusLed.setForeground(LED_GRAY);
		panel.add(statusLed);

		JLabel ver = new JLabel("Cloudflow v1.3.0");
		ver.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) (11 * proportion)));
		ver.setForeground(LABEL_COLOR);
		panel.add(ver);

		return panel;
	}

	private void bindEvents() {
		start.setEnabled(false);
		stop.setEnabled(false);
		resatrt.setEnabled(false);
		setting.setEnabled(false);
		start.addActionListener(e -> {
			start.setEnabled(false);
			setting.setEnabled(false);
			fileIOUtil.setEnabled(false);
			if (filesViewer != null) {
				filesViewer.setEnabled(false);
			}
			printMessage("\u542f\u52a8\u670d\u52a1\u5668...");
			if (ss != null) {
				SwingUtilities.invokeLater(() -> {
					serverStatusLab.setText(S_STARTING);
					statusLed.setForeground(LED_ORANGE);
				});
				Thread t = new Thread(() -> {
					if (ss.start()) {
						printMessage("\u542f\u52a8\u5b8c\u6210\u3002\u6b63\u5728\u68c0\u67e5\u670d\u52a1\u5668\u72b6\u6001...");
						if (st.getServerStatus()) {
							printMessage("Cloudflow\u670d\u52a1\u5668\u5df2\u7ecf\u542f\u52a8\uff0c\u53ef\u4ee5\u6b63\u5e38\u8bbf\u95ee\u4e86\u3002");
						} else {
							printMessage("Cloudflow\u670d\u52a1\u5668\u672a\u80fd\u6210\u529f\u542f\u52a8\uff0c\u8bf7\u68c0\u67e5\u8bbe\u7f6e\u6216\u67e5\u770b\u5f02\u5e38\u4fe1\u606f\u3002");
						}
					} else {
						if (ConfigureReader.instance().getPropertiesStatus() != 0) {
							switch (ConfigureReader.instance().getPropertiesStatus()) {
							case ConfigureReader.INVALID_PORT:
								printMessage("Cloudflow\u65e0\u6cd5\u542f\u52a8\uff1a\u7aef\u53e3\u8bbe\u7f6e\u65e0\u6548\u3002");
								break;
							case ConfigureReader.INVALID_BUFFER_SIZE:
								printMessage("Cloudflow\u65e0\u6cd5\u542f\u52a8\uff1a\u7f13\u5b58\u8bbe\u7f6e\u65e0\u6548\u3002");
								break;
							case ConfigureReader.INVALID_FILE_SYSTEM_PATH:
								printMessage("Cloudflow\u65e0\u6cd5\u542f\u52a8\uff1a\u6587\u4ef6\u7cfb\u7edf\u8def\u5f84\u6216\u67d0\u4e00\u6269\u5c55\u5b58\u50a8\u533a\u8bbe\u7f6e\u65e0\u6548\u3002");
								break;
							case ConfigureReader.INVALID_LOG:
								printMessage("Cloudflow\u65e0\u6cd5\u542f\u52a8\uff1a\u65e5\u5fd7\u8bbe\u7f6e\u65e0\u6548\u3002");
								break;
							case ConfigureReader.INVALID_VC:
								printMessage("Cloudflow\u65e0\u6cd5\u542f\u52a8\uff1a\u767b\u5f55\u9a8c\u8bc1\u7801\u8bbe\u7f6e\u65e0\u6548\u3002");
								break;
							case ConfigureReader.INVALID_MUST_LOGIN_SETTING:
								printMessage("Cloudflow\u65e0\u6cd5\u542f\u52a8\uff1a\u5fc5\u987b\u767b\u5165\u8bbe\u7f6e\u65e0\u6548\u3002");
								break;
							case ConfigureReader.INVALID_CHANGE_PASSWORD_SETTING:
								printMessage("Cloudflow\u65e0\u6cd5\u542f\u52a8\uff1a\u7528\u6237\u4fee\u6539\u8d26\u6237\u5bc6\u7801\u8bbe\u7f6e\u65e0\u6548\u3002");
								break;
							case ConfigureReader.INVALID_FILE_CHAIN_SETTING:
								printMessage("Cloudflow\u65e0\u6cd5\u542f\u52a8\uff1a\u6c38\u4e45\u8d44\u6e90\u94fe\u63a5\u8bbe\u7f6e\u65e0\u6548\u3002");
								break;
							default:
								printMessage("Cloudflow\u65e0\u6cd5\u542f\u52a8\uff0c\u8bf7\u68c0\u67e5\u8bbe\u7f6e\u6216\u67e5\u770b\u5f02\u5e38\u4fe1\u606f\u3002");
								break;
							}
						} else {
							printMessage("Cloudflow\u65e0\u6cd5\u542f\u52a8\uff0c\u8bf7\u68c0\u67e5\u8bbe\u7f6e\u6216\u67e5\u770b\u5f02\u5e38\u4fe1\u606f\u3002");
						}
						SwingUtilities.invokeLater(() -> {
							serverStatusLab.setText(S_STOP);
							statusLed.setForeground(LED_GRAY);
						});
					}
					SwingUtilities.invokeLater(() -> updateServerStatus());
				});
				t.start();
			}
		});
		stop.addActionListener(e -> {
			stop.setEnabled(false);
			resatrt.setEnabled(false);
			fileIOUtil.setEnabled(false);
			if (filesViewer != null) {
				filesViewer.setEnabled(false);
			}
			printMessage("\u5173\u95ed\u670d\u52a1\u5668...");
			Thread t = new Thread(() -> {
				if (cs != null) {
					SwingUtilities.invokeLater(() -> {
						serverStatusLab.setText(S_STOPPING);
						statusLed.setForeground(LED_ORANGE);
					});
					if (cs.close()) {
						printMessage("\u5173\u95ed\u5b8c\u6210\u3002\u6b63\u5728\u68c0\u67e5\u670d\u52a1\u5668\u72b6\u6001...");
						if (st.getServerStatus()) {
							printMessage("Cloudflow\u670d\u52a1\u5668\u672a\u80fd\u6210\u529f\u5173\u95ed\uff0c\u5982\u6709\u9700\u8981\uff0c\u53ef\u4ee5\u5f3a\u5236\u5173\u95ed\u7a0b\u5e8f\uff08\u4e0d\u5b89\u5168\uff09\u3002");
						} else {
							printMessage("Cloudflow\u670d\u52a1\u5668\u5df2\u7ecf\u5173\u95ed\uff0c\u505c\u6b62\u6240\u6709\u8bbf\u95ee\u3002");
						}
					} else {
						printMessage("Cloudflow\u670d\u52a1\u5668\u65e0\u6cd5\u5173\u95ed\uff0c\u8bf7\u624b\u52a8\u7ed3\u675f\u672c\u7a0b\u5e8f\u3002");
					}
					SwingUtilities.invokeLater(() -> updateServerStatus());
				}
			});
			t.start();
		});
		exit.addActionListener(e -> {
			fileIOUtil.setEnabled(false);
			if (filesViewer != null) {
				filesViewer.setEnabled(false);
			}
			exitApp();
		});
		resatrt.addActionListener(e -> {
			stop.setEnabled(false);
			resatrt.setEnabled(false);
			fileIOUtil.setEnabled(false);
			if (filesViewer != null) {
				filesViewer.setEnabled(false);
			}
			Thread t = new Thread(() -> {
				printMessage("\u6b63\u5728\u91cd\u542f\u670d\u52a1\u5668...");
				if (cs.close()) {
					if (ss.start()) {
						printMessage("\u91cd\u542f\u6210\u529f\uff0c\u53ef\u4ee5\u6b63\u5e38\u8bbf\u95ee\u4e86\u3002");
					} else {
						printMessage("Cloudflow\u670d\u52a1\u5668\u672a\u80fd\u6210\u529f\u91cd\u542f\uff0c\u8bf7\u68c0\u67e5\u8bbe\u7f6e\u6216\u67e5\u770b\u5f02\u5e38\u4fe1\u606f\u3002");
					}
				} else {
					printMessage("Cloudflow\u670d\u52a1\u5668\u65e0\u6cd5\u5173\u95ed\uff0c\u8bf7\u5c1d\u8bd5\u624b\u52a8\u5173\u95ed\u3002");
				}
				SwingUtilities.invokeLater(() -> updateServerStatus());
			});
			t.start();
		});
		setting.addActionListener(e -> {
			sw = SettingWindow.getInstance();
			Thread t = new Thread(() -> sw.show());
			t.start();
		});
		fileIOUtil.addActionListener(e -> {
			fileIOUtil.setEnabled(false);
			if (filesViewer != null) {
				filesViewer.setEnabled(false);
			}
			Thread t = new Thread(() -> {
				try {
					fsv = FSViewer.getInstance();
					fsv.show();
				} catch (SQLException e1) {
					Printer.instance.print("\u9519\u8bef\uff1a\u65e0\u6cd5\u8bfb\u53d6\u6587\u4ef6\uff0c\u6587\u4ef6\u7cfb\u7edf\u53ef\u80fd\u5df2\u7ecf\u635f\u574f\uff0c\u60a8\u53ef\u4ee5\u5c1d\u8bd5\u91cd\u542f\u5e94\u7528\u3002");
				}
				SwingUtilities.invokeLater(() -> {
					fileIOUtil.setEnabled(true);
					if (filesViewer != null) {
						filesViewer.setEnabled(true);
					}
				});
			});
			t.start();
		});
	}

	public void show() {
		window.setVisible(true);
		updateServerStatus();
	}

	public JFrame getWindow() {
		return window;
	}

	public static JFrame getMainWindow() {
		try {
			return getInsatnce().getWindow();
		} catch (Exception e) {
			return null;
		}
	}

	public static void setOnCloseServer(final OnCloseServer cs) {
		try {
			ServerUIModule ui = getInsatnce();
			ui.cs = cs;
		} catch (Exception e) {
			Printer.instance.print("\u9519\u8bef\uff1a\u65e0\u6cd5\u521d\u59cb\u5316UI\u6a21\u5757\u3002");
		}
	}

	public static ServerUIModule getInsatnce() throws Exception {
		if (instance == null) {
			instance = new ServerUIModule();
		}
		return instance;
	}

	public static void setStartServer(final OnStartServer ss) {
		try {
			ServerUIModule ui = getInsatnce();
			ui.ss = ss;
		} catch (Exception e) {
			Printer.instance.print("\u9519\u8bef\uff1a\u65e0\u6cd5\u521d\u59cb\u5316UI\u6a21\u5757\u3002");
		}
	}

	public static void setGetServerStatus(final GetServerStatus st) {
		try {
			ServerUIModule ui = getInsatnce();
			ui.st = st;
			SettingWindow.setGetServerStatus(st);
		} catch (Exception e) {
			Printer.instance.print("\u9519\u8bef\uff1a\u65e0\u6cd5\u521d\u59cb\u5316UI\u6a21\u5757\u3002");
		}
	}

	public void updateServerStatus() {
		if (st != null) {
			Thread t = new Thread(() -> {
				if (st.getServerStatus()) {
					SwingUtilities.invokeLater(() -> {
						serverStatusLab.setText(S_START);
						statusLed.setForeground(LED_GREEN);
						start.setEnabled(false);
						stop.setEnabled(true);
						resatrt.setEnabled(true);
						setting.setEnabled(false);
					});
				} else {
					SwingUtilities.invokeLater(() -> {
						serverStatusLab.setText(S_STOP);
						statusLed.setForeground(LED_GRAY);
						start.setEnabled(true);
						stop.setEnabled(false);
						resatrt.setEnabled(false);
						setting.setEnabled(true);
					});
				}
				SwingUtilities.invokeLater(() -> {
					fileIOUtil.setEnabled(true);
					if (filesViewer != null) {
						filesViewer.setEnabled(true);
					}
					portStatusLab.setText(st.getPort() + "");
					if (st.getLogLevel() != null) {
						switch (st.getLogLevel()) {
						case Event: {
							logLevelLab.setText(L_ALL);
							break;
						}
						case None: {
							logLevelLab.setText(L_NONE);
							break;
						}
						case Runtime_Exception: {
							logLevelLab.setText(L_EXCEPTION);
							break;
						}
						default: {
							logLevelLab.setText("\u65e0\u6cd5\u83b7\u53d6(?)");
							break;
						}
						}
					}
					bufferSizeLab.setText(st.getBufferSize() / 1024 + " KB");
				});
			});
			t.start();
		}
	}

	private void exitApp() {
		SwingUtilities.invokeLater(() -> {
			start.setEnabled(false);
			stop.setEnabled(false);
			exit.setEnabled(false);
			resatrt.setEnabled(false);
			setting.setEnabled(false);
		});
		printMessage("\u9000\u51fa\u7a0b\u5e8f...");
		if (cs != null) {
			final Thread t = new Thread(() -> {
				if (st.getServerStatus()) {
					cs.close();
				}
				System.exit(0);
			});
			t.start();
		} else {
			System.exit(0);
		}
	}

	private void showWindow() {
		SwingUtilities.invokeLater(() -> {
			window.setVisible(true);
			window.setExtendedState(JFrame.NORMAL);
			window.requestFocus();
		});
	}

	public void printMessage(final String context) {
		SwingUtilities.invokeLater(() -> {
			output.append("[" + getFormateDate() + "]" + context + "\n");
		});
	}

	private String getFormateDate() {
		if (null == sdf) {
			sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		}
		if (ti != null) {
			final Date d = ti.get();
			return sdf.format(d);
		}
		return sdf.format(new Date());
	}

	public static void setGetServerTime(final GetServerTime ti) {
		try {
			ServerUIModule ui = getInsatnce();
			ui.ti = ti;
		} catch (Exception e) {
			Printer.instance.print("\u9519\u8bef\uff1a\u65e0\u6cd5\u521d\u59cb\u5316UI\u6a21\u5757\u3002");
		}
	}

	public static void setUpdateSetting(final UpdateSetting us) {
		SettingWindow.setUpdateSetting(us);
	}
}

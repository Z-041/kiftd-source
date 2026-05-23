package kohgylw.kiftd.ui.module;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.LogLevel;
import kohgylw.kiftd.server.enumeration.VCLevel;
import kohgylw.kiftd.server.pojo.ExtendStores;
import kohgylw.kiftd.server.pojo.ServerSetting;
import kohgylw.kiftd.ui.callback.*;
import kohgylw.kiftd.ui.pojo.FileSystemPath;

/**
 * 
 * <h2>界面模块——设置</h2>
 * <p>
 * 设置界面类，负责图形化界面下的设置界面显示。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class SettingWindow extends KiftdDynamicWindow {
	private JDialog window;
	private JTextField portinput;
	private JTextField bufferinput;
	private JComboBox<String> mlinput;
	private JComboBox<String> vcinput;
	private JComboBox<String> logLevelinput;
	private JComboBox<String> changePwdinput;
	private JComboBox<String> showChaininput;
	private JButton cancel;
	private JButton update;
	private JButton changeFileSystemPath;
	protected File chooserPath;
	protected List<FileSystemPath> extendStores;
	private static SettingWindow sw;
	private static final String ML_OPEN = "是(YES)";
	private static final String ML_CLOSE = "否(CLOSE)";
	private static final String VC_STANDARD = "标准(STANDARD)";
	private static final String VC_SIMP = "简化(SIMPLIFIED)";
	private static final String VC_CLOSE = "关闭(CLOSE)";
	private static final String CHANGE_PWD_OPEN = "启用(ALLOW)";
	private static final String CHANGE_PWD_CLOSE = "禁用(PROHIBIT)";
	private static final String SHOW_CHAIN_OPEN = "启用(OPEN)";
	private static final String SHOW_CHAIN_CLOSE = "禁用(CLOSE)";
	GetServerStatus st;
	private static UpdateSetting us;
	private FileSystemPathViewer fspv;

	private SettingWindow() {
		setUIFont();
		window = new JDialog(ServerUIModule.getMainWindow(), "Cloudflow-设置");
		window.setModal(true);
		window.setSize(420, 425);
		window.setLocation(150, 150);
		window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		window.setLayout(new BoxLayout(window.getContentPane(), BoxLayout.Y_AXIS));
		final JPanel titlebox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		titlebox.setBorder(new EmptyBorder(7, 0, 7, 0));
		final JLabel title = new JLabel("服务器设置 Server Setting");
		title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (20 * proportion)));
		titlebox.add(title);
		window.add(titlebox);
		final JPanel settingbox = new JPanel(new GridLayout(8, 1, 0, 2));
		settingbox.setBorder(new EtchedBorder());
		final int interval = 2;
		final JPanel portbox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		portbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel porttitle = new JLabel("端口(port)：");
		portinput = new JTextField();
		portinput.setPreferredSize(new Dimension((int) (120 * proportion), (int) (25 * proportion)));
		portbox.add(porttitle);
		portbox.add(portinput);
		final JPanel mlbox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		mlbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel mltitle = new JLabel("必须登入(must login)：");
		mlinput = new JComboBox<>();
		mlinput.addItem(ML_OPEN);
		mlinput.addItem(ML_CLOSE);
		mlinput.setPreferredSize(new Dimension((int) (170 * proportion), (int) (20 * proportion)));
		mlbox.add(mltitle);
		mlbox.add(mlinput);
		final JPanel vcbox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		vcbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel vctitle = new JLabel("登录验证码(VC type)：");
		vcinput = new JComboBox<>();
		vcinput.addItem(VC_STANDARD);
		vcinput.addItem(VC_SIMP);
		vcinput.addItem(VC_CLOSE);
		vcinput.setPreferredSize(new Dimension((int) (170 * proportion), (int) (20 * proportion)));
		vcbox.add(vctitle);
		vcbox.add(vcinput);
		final JPanel bufferbox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bufferbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel buffertitle = new JLabel("缓存大小(buffer)：");
		bufferinput = new JTextField();
		bufferinput.setPreferredSize(new Dimension((int) (170 * proportion), (int) (25 * proportion)));
		final JLabel bufferUnit = new JLabel("KB");
		bufferbox.add(buffertitle);
		bufferbox.add(bufferinput);
		bufferbox.add(bufferUnit);
		final JPanel logbox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		logbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel logtitle = new JLabel("日志等级(log level)：");
		logLevelinput = new JComboBox<>();
		logLevelinput.addItem("记录全部(ALL)");
		logLevelinput.addItem("仅异常(EXCEPTION)");
		logLevelinput.addItem("不记录(NONE)");
		logLevelinput.setPreferredSize(new Dimension((int) (170 * proportion), (int) (20 * proportion)));
		logbox.add(logtitle);
		logbox.add(logLevelinput);
		final JPanel cpbox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		cpbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel cptitle = new JLabel("用户修改密码(change password)：");
		changePwdinput = new JComboBox<>();
		changePwdinput.addItem(CHANGE_PWD_CLOSE);
		changePwdinput.addItem(CHANGE_PWD_OPEN);
		changePwdinput.setPreferredSize(new Dimension((int) (170 * proportion), (int) (20 * proportion)));
		cpbox.add(cptitle);
		cpbox.add(changePwdinput);
		final JPanel scbox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		scbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel sctitle = new JLabel("永久资源链接(file chain)：");
		showChaininput = new JComboBox<>();
		showChaininput.addItem(SHOW_CHAIN_CLOSE);
		showChaininput.addItem(SHOW_CHAIN_OPEN);
		showChaininput.setPreferredSize(new Dimension((int) (170 * proportion), (int) (20 * proportion)));
		scbox.add(sctitle);
		scbox.add(showChaininput);
		final JPanel filePathBox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		filePathBox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel filePathtitle = new JLabel("文件系统路径(file system path)：");
		changeFileSystemPath = new JButton("管理(Manage)");
		changeFileSystemPath.setPreferredSize(new Dimension((int) (170 * proportion), (int) (32 * proportion)));
		filePathBox.add(filePathtitle);
		filePathBox.add(changeFileSystemPath);
		settingbox.add(portbox);
		settingbox.add(mlbox);
		settingbox.add(vcbox);
		settingbox.add(bufferbox);
		settingbox.add(logbox);
		settingbox.add(cpbox);
		settingbox.add(scbox);
		settingbox.add(filePathBox);
		window.add(settingbox);
		final JPanel buttonbox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonbox.setBorder(new EmptyBorder(0, 0, 5, 0));
		update = new JButton("应用(Update)");
		cancel = new JButton("取消(Cancel)");
		update.setPreferredSize(new Dimension((int) (155 * proportion), (int) (32 * proportion)));
		cancel.setPreferredSize(new Dimension((int) (155 * proportion), (int) (32 * proportion)));
		buttonbox.add(update);
		buttonbox.add(cancel);
		window.add(buttonbox);
		cancel.addActionListener(e -> window.setVisible(false));
		update.addActionListener(e -> {
			if (st != null && st.getServerStatus()) {
				getServerStatus();
			} else {
				Thread t = new Thread(() -> {
					if (us != null) {
						try {
							ServerSetting ss = new ServerSetting();
							ss.setPort(Integer.parseInt(portinput.getText()));
							ss.setBuffSize(Integer.parseInt(bufferinput.getText()) * 1024);
							ss.setFsPath(chooserPath.getAbsolutePath());
							List<ExtendStores> ess = new ArrayList<>();
							for (FileSystemPath fsp : extendStores) {
								ExtendStores es = new ExtendStores();
								es.setIndex(fsp.getIndex());
								es.setPath(fsp.getPath());
								ess.add(es);
							}
							ss.setExtendStores(ess);
							switch (logLevelinput.getSelectedIndex()) {
							case 0:
								ss.setLog(LogLevel.Event);
								break;
							case 1:
								ss.setLog(LogLevel.Runtime_Exception);
								break;
							case 2:
								ss.setLog(LogLevel.None);
								break;
							default:
								break;
							}
							switch (mlinput.getSelectedIndex()) {
							case 0:
								ss.setMustLogin(true);
								break;
							case 1:
								ss.setMustLogin(false);
								break;
							default:
								break;
							}
							switch (changePwdinput.getSelectedIndex()) {
							case 0:
								ss.setChangePassword(false);
								break;
							case 1:
								ss.setChangePassword(true);
								break;
							default:
								break;
							}
							switch (showChaininput.getSelectedIndex()) {
							case 0:
								ss.setFileChain(false);
								break;
							case 1:
								ss.setFileChain(true);
								break;
							default:
								break;
							}
							switch (vcinput.getSelectedIndex()) {
							case 0: {
								ss.setVc(VCLevel.Standard);
								break;
							}
							case 1: {
								ss.setVc(VCLevel.Simplified);
								break;
							}
							case 2: {
								ss.setVc(VCLevel.Close);
								break;
							}
							default:
								break;
							}
							if (us.update(ss)) {
								try {
									ServerUIModule.getInsatnce().updateServerStatus();
								} catch (Exception exc) {
									Printer.instance.print(exc.getMessage());
								}
								SwingUtilities.invokeLater(() -> window.setVisible(false));
							}
						} catch (Exception exc) {
							Printer.instance.print(exc.getMessage());
							Printer.instance.print("错误：无法更新服务器设置");
						}
					} else {
						SwingUtilities.invokeLater(() -> window.setVisible(false));
					}
				});
				t.start();
			}
		});
		changeFileSystemPath.addActionListener(e -> {
			fspv = FileSystemPathViewer.getInstance();
			fspv.show();
		});
		modifyComponentSize(window);
	}

	protected void show() {
		getServerStatus();
		window.setVisible(true);
	}

	public JDialog getWindow() {
		return window;
	}

	private void getServerStatus() {
		final Thread t = new Thread(() -> {
			if (st != null) {
				bufferinput.setText(st.getBufferSize() == 0 ? st.getInitBufferSize()
						: st.getBufferSize() / 1024 + "");
				portinput.setText(st.getPort() == 0 ? st.getInitProt() + ""
						: st.getPort() + "");
				if (st.getFileSystemPath() != null) {
					chooserPath = new File(st.getFileSystemPath());
				} else {
					chooserPath = new File(st.getInitFileSystemPath());
				}
				extendStores = st.getExtendStores();
				if (st.getLogLevel() != null) {
					SwingUtilities.invokeLater(() -> {
						switch (st.getLogLevel()) {
						case Event: {
							logLevelinput.setSelectedIndex(0);
							break;
						}
						case Runtime_Exception: {
							logLevelinput.setSelectedIndex(1);
							break;
						}
						case None: {
							logLevelinput.setSelectedIndex(2);
							break;
						}
						}
					});
				} else {
					SwingUtilities.invokeLater(() -> {
						switch (st.getInitLogLevel()) {
						case Event: {
							logLevelinput.setSelectedIndex(0);
							break;
						}
						case Runtime_Exception: {
							logLevelinput.setSelectedIndex(1);
							break;
						}
						case None: {
							logLevelinput.setSelectedIndex(2);
							break;
						}
						}
					});
				}
				SwingUtilities.invokeLater(() -> {
					if (st.getMustLogin()) {
						mlinput.setSelectedIndex(0);
					} else {
						mlinput.setSelectedIndex(1);
					}
					if (st.isAllowChangePassword()) {
						changePwdinput.setSelectedIndex(1);
					} else {
						changePwdinput.setSelectedIndex(0);
					}
					if (st.isOpenFileChain()) {
						showChaininput.setSelectedIndex(1);
					} else {
						showChaininput.setSelectedIndex(0);
					}
					if (st.getVCLevel() != null) {
						switch (st.getVCLevel()) {
						case Standard: {
							vcinput.setSelectedIndex(0);
							break;
						}
						case Simplified: {
							vcinput.setSelectedIndex(1);
							break;
						}
						case Close: {
							vcinput.setSelectedIndex(2);
							break;
						}
						}
					} else {
						switch (st.getInitVCLevel()) {
						case Standard: {
							vcinput.setSelectedIndex(0);
							break;
						}
						case Simplified: {
							vcinput.setSelectedIndex(1);
							break;
						}
						case Close: {
							vcinput.setSelectedIndex(2);
							break;
						}
						}
					}
				});
			}
		});
		t.start();
	}

	protected static SettingWindow getInstance() {
		if (sw == null) {
			sw = new SettingWindow();
		}
		return sw;
	}

	public static void setGetServerStatus(final GetServerStatus st) {
		if (sw == null) {
			sw = new SettingWindow();
		}
		sw.st = st;
	}

	public static void setUpdateSetting(final UpdateSetting us) {
		SettingWindow.us = us;
	}
}

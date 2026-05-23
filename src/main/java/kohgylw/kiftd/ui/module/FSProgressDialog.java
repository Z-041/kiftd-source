package kohgylw.kiftd.ui.module;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import kohgylw.kiftd.util.file_system_manager.FileSystemManager;

public class FSProgressDialog extends KiftdDynamicWindow {

	private JDialog window;
	private JLabel message;
	private JProgressBar pBar;
	private JButton cancel;
	private boolean listen;

	private FSProgressDialog(JDialog parentWindow) {
		setUIFont();
		window = new JDialog(parentWindow, "执行中...");
		window.setModal(true);
		window.setSize(380, 120);
		window.setLocation(200, 200);
		window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cancelOp();
			}
		});
		window.setResizable(false);
		window.setLayout(new BoxLayout(window.getContentPane(), BoxLayout.Y_AXIS));
		JPanel messageBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
		message = new JLabel("请稍候...");
		messageBox.add(message);
		pBar = new JProgressBar(0, 100);
		pBar.setStringPainted(false);
		JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		cancel = new JButton("终止");
		cancel.setPreferredSize(new Dimension((int) (90 * proportion), (int) (27 * proportion)));
		cancel.addActionListener(e -> cancelOp());
		btnBox.add(cancel);
		window.add(messageBox);
		window.add(pBar);
		window.add(btnBox);
		modifyComponentSize(window);
	}

	protected void show() {
		listen = true;
		pBar.setValue(0);
		message.setText("请稍候...");
		Thread lt = new Thread(() -> {
			while (listen) {
				SwingUtilities.invokeLater(() -> {
					pBar.setValue(FileSystemManager.per);
					message.setText(FileSystemManager.message);
				});
				try {
					Thread.sleep(16);
				} catch (InterruptedException e) {
					listen = false;
				}
			}
			SwingUtilities.invokeLater(() -> window.dispose());
		});
		lt.start();
		window.setVisible(true);
	}

	protected void close() {
		listen = false;
	}

	private void cancelOp() {
		if (JOptionPane.showConfirmDialog(window, "操作仍在进行中，确认要立即终止？", "警告", JOptionPane.YES_NO_OPTION) == 0) {
			FileSystemManager.getInstance().cannel();
			window.dispose();
		}
	}

	protected static FSProgressDialog getNewInstance(JDialog parentWindow) {
		return new FSProgressDialog(parentWindow);
	}

}

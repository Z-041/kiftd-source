package kohgylw.kiftd.ui.module;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.swing.UIManager;

/**
 * 
 * <h2>kiftd窗口父类，所有窗口均应继承自该父类</h2>
 * <p>
 * 该类中定义了动态窗口绘制的相应操作。以便于所有继承自该类的窗口对象均能够使用动态绘制方法。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class KiftdDynamicWindow {

	private final int OriginResolution_W = 1440;
	private final int OriginResolution_H = 900;
	private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private double proportionW = screenSize.getWidth() / (double) OriginResolution_W;
	private double proportionH = screenSize.getHeight() / (double) OriginResolution_H;
	protected double proportion = proportionW > proportionH ? proportionH : proportionW;

	protected Dimension fileChooerSize;

	public KiftdDynamicWindow() {
		String path = System.getProperty("user.dir");
		String classPath = System.getProperty("java.class.path");
		if (classPath.indexOf(File.pathSeparator) < 0) {
			File f = new File(classPath);
			classPath = f.getAbsolutePath();
			if (classPath.endsWith(".jar")) {
				path = classPath.substring(0, classPath.lastIndexOf(File.separator));
			}
		}
		String confdir = path + File.separator + "conf" + File.separator;
		File settingFile = new File(confdir, "init.txt");
		Properties settingp = new Properties();
		try (FileInputStream fis = new FileInputStream(settingFile)) {
			settingp.load(fis);
			String udp = settingp.getProperty("scale");
			if (udp != null) {
				double udpi = Double.parseDouble(udp);
				if (udpi > 10) {
					udpi = 10;
				}
				proportion = udpi;
			}
		} catch (Exception e1) {

		}
		if (proportion < 1.0) {
			proportion = 1.0;
		}
		fileChooerSize = new Dimension((int) (570 * proportion), (int) (300 * proportion));
	}

	protected void modifyComponentSize(Container c) {
		c.setSize((int) (c.getWidth() * proportion), (int) (c.getHeight() * proportion));
	}

	public int getOriginResolution_W() {
		return OriginResolution_W;
	}

	public int getOriginResolution_H() {
		return OriginResolution_H;
	}

	protected void setUIFont() {
		Font baseFont = UIManager.getFont("Label.font");
		String fontName = (baseFont != null) ? baseFont.getFamily() : Font.SANS_SERIF;
		int baseSize = (baseFont != null) ? baseFont.getSize() : 13;
		Font f = new Font(fontName, Font.PLAIN, (int) (baseSize * proportion));
		String names[] = { "Label", "CheckBox", "PopupMenu", "MenuItem", "CheckBoxMenuItem", "JRadioButtonMenuItem",
				"ComboBox", "Button", "Tree", "ScrollPane", "TabbedPane", "EditorPane", "TitledBorder", "Menu",
				"TextArea", "OptionPane", "MenuBar", "ToolBar", "ToggleButton", "ToolTip", "ProgressBar", "TableHeader",
				"Panel", "List", "ColorChooser", "PasswordField", "TextField", "Table", "Label", "Viewport",
				"RadioButtonMenuItem", "RadioButton", "DesktopPane", "InternalFrame" };
		for (String item : names) {
			UIManager.put(item + ".font", f);
		}
	}

}

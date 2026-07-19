import javax.swing.*;
import java.awt.*;

public class SwingTest {
    public static void main(String[] args) {
        System.out.println("开始创建窗口...");
        JFrame frame = new JFrame("Swing 渲染测试");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("如果你能看到这行文字，说明 Swing 渲染正常", SwingConstants.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea textArea = new JTextArea("这是一个测试文本区域。\n如果你能看到这段文字，说明文本组件也正常。");
        textArea.setEditable(false);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        btnPanel.add(new JButton("测试按钮 A"));
        btnPanel.add(new JButton("测试按钮 B"));
        btnPanel.add(new JButton("测试按钮 C"));
        panel.add(btnPanel, BorderLayout.SOUTH);

        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        System.out.println("窗口已显示。请检查是否能看到内容。");

        try { Thread.sleep(30000); } catch (InterruptedException e) {}
        System.out.println("30秒后自动退出");
        System.exit(0);
    }
}

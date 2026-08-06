package kohgylw.kiftd.ui.util;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import kohgylw.kiftd.ui.pojo.FileSystemPath;

/**
 *
 * <h2>文件系统路径表格组件</h2>
 * <p>
 * 该类继承自JTable，用于在设置窗口中显示和选择文件系统路径列表。
 * 支持显示主文件系统和扩展存储区扩展存储区的路径信息，包括类型（编号）和绝对路径两列，
 * 并可根据窗口大小自动调整列宽。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class PathsTable extends JTable {

	private static final String[] columns = new String[] { "类型（编号）", "路径" };
	float[] columnWidthPercentage = { 20.0f, 80.0f };
	private Map<Integer, Short> shownFileSystemPath = new HashMap<>();

	private static final long serialVersionUID = -3436472714356711024L;

	public PathsTable() {
		super(new Object[][] {}, columns);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resizeColumns();
			}
		});
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public void updateValues(List<FileSystemPath> paths) {
		Runnable doUpdate = new Runnable() {
			@Override
			public void run() {
				shownFileSystemPath.clear();
				try {
					setModel(new TableModel() {
						@Override
						public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
						}

						@Override
						public void removeTableModelListener(javax.swing.event.TableModelListener l) {
						}

						@Override
						public boolean isCellEditable(int rowIndex, int columnIndex) {
							return false;
						}

						@Override
						public Object getValueAt(int rowIndex, int columnIndex) {
							switch (columnIndex) {
							case 0:
								shownFileSystemPath.put(rowIndex, paths.get(rowIndex).getIndex());
								return paths.get(rowIndex).getType() + "（" + paths.get(rowIndex).getIndex() + "）";
							case 1:
								return paths.get(rowIndex).getPath().getAbsoluteFile();
							default:
								return "--";
							}
						}

						@Override
						public int getRowCount() {
							return paths.size();
						}

						@Override
						public String getColumnName(int columnIndex) {
							return columns[columnIndex];
						}

						@Override
						public int getColumnCount() {
							return columns.length;
						}

						@Override
						public Class<?> getColumnClass(int columnIndex) {
							return String.class;
						}

						@Override
						public void addTableModelListener(javax.swing.event.TableModelListener l) {
						}
					});
					resizeColumns();
					validate();
				} catch (Exception e) {
				// 刷新失败时保留旧布局，避免界面异常
				}
			}
		};
		Thread t = new Thread(() -> SwingUtilities.invokeLater(doUpdate));
		t.start();
	}

	public short getSelectFileSystemIndex() {
		if (getSelectedRow() >= 0) {
			return shownFileSystemPath.get(getSelectedRow());
		}
		return -1;
	}

	private void resizeColumns() {
		int tW = getWidth();
		TableColumn column;
		TableColumnModel jTableColumnModel = getColumnModel();
		int cantCols = jTableColumnModel.getColumnCount();
		for (int i = 0; i < cantCols; i++) {
			column = jTableColumnModel.getColumn(i);
			int pWidth = Math.round(columnWidthPercentage[i] * tW);
			column.setPreferredWidth(pWidth);
		}
	}
}

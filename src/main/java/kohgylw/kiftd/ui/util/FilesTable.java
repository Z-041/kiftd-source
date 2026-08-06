package kohgylw.kiftd.ui.util;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.util.file_system_manager.pojo.FolderTreeNode;

/**
 * 
 * <h2>文件列表表格</h2>
 * <p>
 * 一个基于Swing默认JTable的表格工具，用于显示文件列表。该工具支持选中行，并会以蓝色和黑色区分文件夹和文件。
 * 需要使用，请直接创建该类的实例，默认显示空表格。如需更新数据，请使用updateValues方法。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FilesTable extends JTable {

	private static final String[] columns = new String[] { "名称", "创建日期", "大小", "创建者" };
	private List<FolderTreeNode> folders;
	private List<Node> files;
	public static final int MAX_LIST_LIMIT = Integer.MAX_VALUE;

	private int sortByName = 1;
	private int sortByDate = 1;
	private static final ThreadLocal<SimpleDateFormat> CDF = ThreadLocal
			.withInitial(() -> new SimpleDateFormat("yyyy年MM月dd日"));
	private int sortBySize = 1;
	private int sortByCreator = 1;

	private static final long serialVersionUID = -3436472714356711024L;

	public FilesTable() {
		super(new Object[][] {}, columns);
		JTableHeader filesTableHeader = getTableHeader();
		filesTableHeader.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int col = filesTableHeader.columnAtPoint(e.getPoint());
				switch (col) {
				case 0:
					if (files != null) {
						files.sort((e1, e2) -> sortByName * e1.getFileName().compareTo(e2.getFileName()));
					}
					if (folders != null) {
						folders.sort((e1, e2) -> sortByName * e1.getFolderName().compareTo(e2.getFolderName()));
					}
					sortByName = sortByName * -1;
					sortByDate = 1;
					sortBySize = 1;
					sortByCreator = 1;
					break;
				case 1:
					if (files != null) {
						files.sort((e1, e2) -> {
							try {
								return sortByDate
										* CDF.get().parse(e1.getFileCreationDate()).compareTo(CDF.get().parse(e2.getFileCreationDate()));
							} catch (ParseException e3) {
								Printer.instance.print(e3.toString());
								return 0;
							}
						});
					}
					if (folders != null) {
						folders.sort((e1, e2) -> {
							try {
								return sortByDate * CDF.get().parse(e1.getFolderCreationDate())
										.compareTo(CDF.get().parse(e2.getFolderCreationDate()));
							} catch (ParseException e3) {
								Printer.instance.print(e3.toString());
								return 0;
							}
						});
					}
					sortByDate = sortByDate * -1;
					sortByName = 1;
					sortBySize = 1;
					sortByCreator = 1;
					break;
				case 2:
					if (files != null) {
						files.sort((e1, e2) -> sortBySize
								* Long.compare(Long.parseLong(e1.getFileSize()), Long.parseLong(e2.getFileSize())));
					}
					sortBySize = sortBySize * -1;
					sortByName = 1;
					sortByDate = 1;
					sortByCreator = 1;
					break;
				case 3:
					if (files != null) {
						files.sort((e1, e2) -> sortByCreator * e1.getFileCreator().compareTo(e2.getFileCreator()));
					}
					if (folders != null) {
						folders.sort((e1, e2) -> sortByCreator * e1.getFolderCreator().compareTo(e2.getFolderCreator()));
					}
					sortByCreator = sortByCreator * -1;
					sortByName = 1;
					sortByDate = 1;
					sortBySize = 1;
					break;
				default:
					break;
				}
				updateValues(folders, files);
			}
		});
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public void updateValues(List<FolderTreeNode> folders, List<Node> files) {
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
						return rowIndex < folders.size() ? "/" + folders.get(rowIndex).getFolderName()
								: files.get(rowIndex - folders.size()).getFileName();
					case 1:
						return rowIndex < folders.size() ? folders.get(rowIndex).getFolderCreationDate()
								: files.get(rowIndex - folders.size()).getFileCreationDate();
					case 2:
						return rowIndex < folders.size() ? "--" : files.get(rowIndex - folders.size()).getFileSize();
					case 3:
						return rowIndex < folders.size() ? folders.get(rowIndex).getFolderCreator()
								: files.get(rowIndex - folders.size()).getFileCreator();
					default:
						return "--";
					}
				}

				@Override
				public int getRowCount() {
					long totalSize = (long) folders.size() + files.size();
					return totalSize > MAX_LIST_LIMIT ? MAX_LIST_LIMIT : (int) totalSize;
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
					return Object.class;
				}

				@Override
				public void addTableModelListener(javax.swing.event.TableModelListener l) {
				}
			});
			setRowFontColor();
			validate();
			this.folders = folders;
			this.files = files;
		} catch (Exception e) {
			Printer.instance.print(e.toString());
		}
	}

	private void setRowFontColor() {
		DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 5132133158132959506L;

			@Override
			protected void setValue(Object value) {
				if (value instanceof String && ((String) value).startsWith("/")) {
					setForeground(Color.BLUE);
				} else {
					setForeground(Color.black);
				}
				setText((String) value);
			}
		};
		DefaultTableCellRenderer dtcr2 = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 5132133158132959506L;

			@Override
			protected void setValue(Object value) {
				if (!"--".equals(value)) {
					if (value instanceof String) {
						double convertSize;
						String unit;
						long size = Long.parseLong((String) value);
						if (size < 1024) {
							convertSize = (double) size;
							unit = "B";
						} else if (size < 1048576L) {
							convertSize = ((double) size / 1024.0);
							unit = "KB";
						} else if (size < 1073741824L) {
							convertSize = ((double) size / 1048576.0);
							unit = "MB";
						} else if (size < 1099511627776L) {
							convertSize = ((double) size / 1073741824.0);
							unit = "GB";
						} else {
							convertSize = ((double) size / 1099511627776.0);
							unit = "TB";
						}
						setText(String.format("%.1f", convertSize) + " " + unit);
						return;
					}
				}
				setText((String) value);
			}
		};
		getColumn(columns[0]).setCellRenderer(dtcr);
		getColumn(columns[2]).setCellRenderer(dtcr2);
	}

	public Object getDoubleClickItem(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
			int row = rowAtPoint(e.getPoint());
			if (row >= 0 && folders != null && row < folders.size()) {
				return folders.get(row);
			}
			if (folders != null && files != null && row >= folders.size() && row < folders.size() + files.size()) {
				return files.get(row - folders.size());
			}
		}
		return null;
	}
}

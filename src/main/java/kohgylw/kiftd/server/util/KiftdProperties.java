package kohgylw.kiftd.server.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 
 * <h2>Kiftd特用版Properties类</h2>
 * <p>
 * 该类用于替代标准的Properties类作为kiftd各项设置参数的数据存储工具。
 * 相较于原始的Properties类，该特用版在查询性能上相近（或稍快），但在读取性能上更慢。
 * 同时能够确保在写入文件时保留原始的文本结构（包括顺序及注释内容）。
 * </p>
 * <p>
 * 底层采用 {@link ConcurrentHashMap} 与 {@link CopyOnWriteArrayList}，
 * 支持配置热加载（WatchService）与业务写入并发时的高频无锁读，避免数据竞争。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class KiftdProperties {

	private List<LineContext> contexts = new CopyOnWriteArrayList<>();// 保存载入的整个文本信息
	private Map<String, String> properties = new ConcurrentHashMap<>();// 仅保存配置信息，用于提高查询效率

	// 用于存储每一行文本信息的包装类
	private class LineContext {
		private String key;// 键
		private String value;// 值
		private String text;// 文本

		private LineContext(String key, String value, String text) {
			this.key = key;
			this.value = value;
			this.text = text;
		}

		@Override
		public boolean equals(Object obj) {
			if (key == null) {
				return false;
			}
			return key.equals(obj);
		}
	}

	/**
	 * 
	 * <h2>获取参数</h2>
	 * <p>
	 * 该功能用于获取指定键对应的配置参数。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param key
	 *            java.lang.String 键名
	 * @return 查询到的值，若传入的键无对应值或键为null则返回null
	 */
	public String getProperty(String key) {
		if (key != null) {
			return properties.get(key);
		}
		return null;// 否则返回null
	}

	/**
	 * 
	 * <h2>获取参数</h2>
	 * <p>
	 * 该功能用于获取指定键对应的配置参数。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param key
	 *            java.lang.String 键名
	 * @param defaultValue
	 *            java.lang.String 若无该配置时返回的替代参数
	 * @return 查询到的值，若传入的键无对应值或键为null则返回null
	 */
	public String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		return value == null ? defaultValue : value;
	}

	/**
	 * 
	 * <h2>新增一个配置或修改已有的配置</h2>
	 * <p>
	 * 当传入的key已经存在时，修改该key对应的配置值，否则新增一个配置。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param key
	 *            java.lang.String 键名
	 * @param value
	 *            java.lang.String 新配置值
	 */
	public void setProperty(String key, String value) {
		if (key != null) {
			properties.put(key, value);
			for (LineContext lc : contexts) {
				if (key.equals(lc.key)) {
					lc.value = value;
					return;
				}
			}
			contexts.add(new LineContext(key, value, null));
		}
	}

	/**
	 * 
	 * <h2>从文本文件中载入配置项</h2>
	 * <p>
	 * 该功能用于清空旧的配置项并从文本流中载入新的配置项。文本中每项配置均应独占一行，
	 * 且使用“=”或“:”作为键值对的分隔符，当存在多个分隔符时，以第一个为准。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param in
	 *            java.io.InputStream 输入流，必须为文本输入流
	 */
	public void load(InputStream in) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1))) {
			String lineStr = null;
			// 按行读取文本
			clear();
			while ((lineStr = reader.readLine()) != null) {
				if (lineStr.startsWith("#")) {
					contexts.add(new LineContext(null, null, lineStr));// 保存为注释
				} else {
					int delimit0 = lineStr.indexOf("=");
					int delimit1 = lineStr.indexOf(":");// 兼容Properties的“:”分割规则，但保存时将统一改为“=”
					int delimitIndex = -1;// 判断第一个出现的分隔符的位置
					if (delimit0 >= 0) {
						delimitIndex = delimit0;
					}
					if (delimit1 >= 0 && delimit1 < delimit0) {
						delimitIndex = delimit1;
					}
					if (delimitIndex >= 0) {
						// 键值对保存前须还原标准 Properties 的转义序列（如 \= 表示字面等号），
						// 否则由 java.util.Properties.store 写入的配置（如 PBKDF2 哈希中的 \=）会被污染
						setProperty(unescape(lineStr.substring(0, delimitIndex)),
								unescape(lineStr.substring(delimitIndex + 1)));// 保存为键值对
					} else {
						contexts.add(new LineContext(null, null, lineStr));// 保存为其他文本
					}
				}
			}
		}
	}

	/**
	 * 
	 * <h2>覆盖并保存配置</h2>
	 * <p>
	 * 将全部配置以文本流的形式写出，若写处至文件则会覆盖原有的内容。当添加标题头时，将会在文本流的开头处增加标题头文字及日期。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param out
	 *            java.io.OutputStream 输出流，能够接收文本流
	 * @param header
	 *            java.lang.String 标题头，若传入null则不添加此项
	 */
	public void store(OutputStream out, String header) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.ISO_8859_1))) {
			if (header != null) {
				writer.write("#" + header);
				writer.newLine();
				writer.write("#" + new Date().toString());
				writer.newLine();
			}
			for (LineContext line : contexts) {
				if (line.key != null) {
					// 写出前须按标准 Properties 规则转义特殊字符（= : \ 控制字符 非ASCII），
					// 与 load() 的转义还原对称，保证 java.util.Properties 与本类读写结果一致
					writer.write(escape(line.key) + "=" + escape(line.value));
					writer.newLine();
				} else {
					writer.write(line.text);
					writer.newLine();
				}
			}
		}
	}

	/**
	 * 
	 * <h2>获得所有配置项</h2>
	 * <p>
	 * 该方法用于获得目前存在的所有配置项，并以List的形式返回。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return java.util.List<java.lang.String> 所有的配置项
	 */
	public Set<String> stringPropertieNames() {
		return properties.keySet();
	}

	/**
	 * 
	 * <h2>清除某项配置</h2>
	 * <p>
	 * 根据传入的键名清除一项配置，若无该键名对应的配置或键名为null则不执行任何操作。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param key
	 *            java.lang.String 键名
	 */
	public void removeProperty(String key) {
		if (key != null) {
			properties.remove(key);
			contexts.removeIf(lc -> key.equals(lc.key));
		}
	}

	/**
	 * 
	 * <h2>清空所有配置项</h2>
	 * <p>
	 * 该功能用于清空所有的配置项。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public void clear() {
		contexts.clear();
		properties.clear();
	}

	// 按标准 java.util.Properties 规则转义键/值中的特殊字符，
	// 保证通过本类写入的文件也可被标准 Properties 工具正确读取
	private static String escape(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '\\':
				sb.append("\\\\");
				break;
			case '=':
				sb.append("\\=");
				break;
			case ':':
				sb.append("\\:");
				break;
			case '#':
				sb.append("\\#");
				break;
			case '!':
				sb.append("\\!");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			default:
				// 非 ASCII 与控制字符转义为 uXXXX 形式，避免 ISO-8859-1 写出时被替换为乱码
				if (c < 0x20 || c > 0x7E) {
					sb.append('\\');
					sb.append('u');
					sb.append(String.format("%04x", (int) c));
				} else {
					sb.append(c);
				}
				break;
			}
		}
		return sb.toString();
	}

	// 还原标准 Properties 的转义序列（\= \: \\ \t \n \r uXXXX 等）
	private static String unescape(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\' && i + 1 < s.length()) {
				char n = s.charAt(++i);
				switch (n) {
				case 't':
					sb.append('\t');
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'r':
					sb.append('\r');
					break;
				case 'f':
					sb.append('\f');
					break;
				case 'u':
					if (i + 4 < s.length()) {
						try {
							sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
							i += 4;
						} catch (NumberFormatException e) {
							sb.append('u');
						}
					} else {
						sb.append('u');
					}
					break;
				default:
					// \= 还原为 =、\: 还原为 :、\\ 还原为 \、\# 还原为 #、\! 还原为 !
					sb.append(n);
					break;
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

}

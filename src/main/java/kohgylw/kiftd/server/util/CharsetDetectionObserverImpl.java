package kohgylw.kiftd.server.util;

import org.mozilla.intl.chardet.nsICharsetDetectionObserver;

/**
 * 
 * <h2>判断文本编码格式回调封装类</h2>
 * <p>该类用于配合jchardet插件用于自动辨别文本的编码格式。其中getCharset()方法用于获得最终识别的编码集，如为null则未能找到。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class CharsetDetectionObserverImpl implements nsICharsetDetectionObserver{
	
	// Notify 由检测线程调用，getCharset 由主线程读取，跨线程可见性用 volatile 保证
	private volatile String charset;

	@Override
	public void Notify(String arg0) {
		charset=arg0;
	}

	public String getCharset() {
		return charset;
	}
	
}

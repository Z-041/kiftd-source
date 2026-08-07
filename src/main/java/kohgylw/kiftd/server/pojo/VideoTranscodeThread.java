package kohgylw.kiftd.server.pojo;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigurationManager;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.progress.EncoderProgressListener;

/**
 * 
 * <h2>视频转码信息</h2>
 * <p>
 * 其中存放了视频的转码信息。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class VideoTranscodeThread {

	private volatile String md5;
	private volatile String progress;
	private volatile String outputFileName;
	// MD5 校验缓存：首次校验通过后记录源文件长度与最后修改时间，
	// 后续轮询仅比较文件元数据即可判断源文件是否变化，避免反复全文件哈希
	private volatile boolean md5Verified;
	private volatile long sourceLength;
	private volatile long sourceLastModified;
	// 转码完成时间戳，用于清理长期未被消费的 FIN 记录及其输出文件
	private volatile long finishTime;

	public VideoTranscodeThread(File f, EncodingAttributes ea,ProcessLocator fl) throws Exception {
		try (FileInputStream fis = new FileInputStream(f)) {
			md5 = DigestUtils.md5Hex(fis);
		}
		progress = "0.0";
		MultimediaObject mo = new MultimediaObject(f,fl);
		final Encoder encoder = new Encoder(fl);
		// 转码为长耗时任务，置为 daemon 线程避免阻塞服务器正常关闭，并命名便于排查
		Thread t = new Thread(() -> {
			try {
				outputFileName="video_"+UUID.randomUUID().toString()+".mp4";
				encoder.encode(mo, new File(ConfigurationManager.instance().getTemporaryfilePath(), outputFileName),
						ea, new EncoderProgressListener() {
							public void progress(int arg0) {
								progress = (arg0 / 10.00) + "";
							}

							public void message(String arg0) {
							}

							public void sourceInfo(MultimediaInfo info) {
							}
						});
				progress = "FIN";
				finishTime = System.currentTimeMillis();
			} catch (Exception e) {
				// 转码失败时置为明确的失败态，避免前端永远轮询不到结束
				progress = "ERROR";
				Printer.instance.print("警告：在线转码功能出现意外错误。详细信息："+e.getMessage());
			}
		}, "kiftd-video-transcode");
		t.setDaemon(true);
		t.start();
	}

	public String getMd5() {
		return md5;
	}

	public String getProgress() {
		return progress;
	}

	public String getOutputFileName() {
		return outputFileName;
	}

	/**
	 * 
	 * <h2>标记 MD5 校验已通过</h2>
	 * <p>
	 * 记录校验时的源文件长度与最后修改时间，供后续轮询做廉价元数据比对。
	 * </p>
	 */
	public void markMd5Verified(long length, long lastModified) {
		this.sourceLength = length;
		this.sourceLastModified = lastModified;
		this.md5Verified = true;
	}

	public boolean isMd5Verified() {
		return md5Verified;
	}

	public long getSourceLength() {
		return sourceLength;
	}

	public long getSourceLastModified() {
		return sourceLastModified;
	}

	public long getFinishTime() {
		return finishTime;
	}

}

package kohgylw.kiftd.server.util;


import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.VideoTranscodeThread;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

/**
 * 
 * <h2>视频转码工具</h2>
 * <p>
 * 该工具用于进行视频转码操作，使用Spring IOC容器管理。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class VideoTranscodeUtil {

	private EncodingAttributes ea;// 转码设定
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private NodeMapper nm;
	@Resource
	private KiftdFFMPEGLocator kfl;

	public static Map<String, VideoTranscodeThread> videoTranscodeThreads = new HashMap<>();

	private static final int MAX_CONCURRENT_TRANSCODES = 3;
	private static final Set<String> SUPPORTED_VIDEO_SUFFIXES = Set.of("mp4", "webm", "mov", "avi", "wmv", "mkv", "flv");
	// 已完成转码记录（含临时输出文件）的有效期，超过后自动清理
	private static final long TRANSCODE_RESULT_TTL_MS = 10 * 60 * 1000L;

	{
		AudioAttributes audio = new AudioAttributes();
		audio.setCodec("libmp3lame");
		audio.setBitRate(128000);
		audio.setChannels(2);
		audio.setSamplingRate(44100);
		VideoAttributes video = new VideoAttributes();
		video.setCodec("libx264");
		ea = new EncodingAttributes();
		ea.setOutputFormat("MP4");
		ea.setVideoAttributes(video);
		ea.setAudioAttributes(audio);
	}

	/**
	 * 
	 * <h2>获取指定视频转码进度</h2>
	 * <p>
	 * 以百分制返回指定ID的视频转码进度，如若未开始则自动开始。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param fId
	 *            java.lang.String 转码视频的ID
	 * @return java.lang.String 转码进度，按百分制返回，例如“1.2”代表完成1.2%，返回null则参数不正确。
	 * @throws Exception
	 *             获取失败
	 */
	public String getTranscodeProcess(String fId) throws Exception {
		Node n;
		File f;
		// 阶段1：锁内快速路径——非完成状态的进度直接返回
		synchronized (videoTranscodeThreads) {
			VideoTranscodeThread vtt = videoTranscodeThreads.get(fId);
			n = nm.selectById(fId);
			if (n == null) {
				// 文件节点已被删除，清理残留转码记录并返回
				if (vtt != null) {
					videoTranscodeThreads.remove(fId);
				}
				return null;
			}
			f = fbu.getFileFromBlocks(n);
			if (f == null) {
				return null;
			}
			if (vtt != null) {
				String progress = vtt.getProgress();
				if (!"FIN".equals(progress)) {
					return progress;
				}
			}
		}
		// 阶段2：锁外校验已完成的转码结果（MD5 全文件读取不占用全局锁）
		VideoTranscodeThread vtt;
		synchronized (videoTranscodeThreads) {
			vtt = videoTranscodeThreads.get(fId);
		}
		if (vtt != null && "FIN".equals(vtt.getProgress())) {
			boolean valid = new File(ConfigurationManager.instance().getTemporaryfilePath(),
					vtt.getOutputFileName()).isFile();
			if (valid) {
				if (vtt.isMd5Verified()) {
					// 已校验通过：仅比较源文件长度与最后修改时间（廉价校验），避免反复全文件哈希
					valid = f.length() == vtt.getSourceLength() && f.lastModified() == vtt.getSourceLastModified();
				} else {
					// 首次校验：对源文件计算一次完整 MD5，通过后缓存文件元数据
					try (FileInputStream fis = new FileInputStream(f)) {
						valid = DigestUtils.md5Hex(fis).equals(vtt.getMd5());
					}
					if (valid) {
						vtt.markMd5Verified(f.length(), f.lastModified());
					}
				}
			}
			if (valid) {
				// 已完成且仍有效的记录：若长期未被消费则清理，避免转码记录与临时输出文件无限堆积
				long finishTime = vtt.getFinishTime();
				if (finishTime > 0 && System.currentTimeMillis() - finishTime > TRANSCODE_RESULT_TTL_MS) {
					synchronized (videoTranscodeThreads) {
						if (videoTranscodeThreads.get(fId) == vtt) {
							videoTranscodeThreads.remove(fId);
						}
					}
					new File(ConfigurationManager.instance().getTemporaryfilePath(),
							vtt.getOutputFileName()).delete();
					return null;
				}
				return vtt.getProgress();
			}
			// 输出文件缺失或源文件已变化，移除失效记录（仅在记录未被替换时移除）
			synchronized (videoTranscodeThreads) {
				if (videoTranscodeThreads.get(fId) == vtt) {
					videoTranscodeThreads.remove(fId);
				}
			}
		}
		// 阶段3：无有效转码记录时启动新的转码
		synchronized (videoTranscodeThreads) {
			if (videoTranscodeThreads.containsKey(fId)) {
				// 并发下已有线程正在建立转码任务，直接返回其初始进度
				return "0.0";
			}
			int dotIndex = n.getFileName().lastIndexOf(".");
			if (dotIndex < 0 || dotIndex >= n.getFileName().length() - 1) {
				return null;
			}
			String suffix = n.getFileName().substring(dotIndex + 1).toLowerCase();
			if (!SUPPORTED_VIDEO_SUFFIXES.contains(suffix)) {
				throw new IllegalArgumentException();
			}
			long activeCount = videoTranscodeThreads.values().stream()
					.filter(t -> !"FIN".equals(t.getProgress()))
					.count();
			if (activeCount >= MAX_CONCURRENT_TRANSCODES) {
				return "WAIT";
			}
			videoTranscodeThreads.put(fId, new VideoTranscodeThread(f, ea, kfl));
			return "0.0";
		}
	}

}

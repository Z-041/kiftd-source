package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;

import com.google.gson.Gson;

import kohgylw.kiftd.server.mapper.*;

import java.io.File;

import jakarta.annotation.*;
import jakarta.servlet.http.*;
import kohgylw.kiftd.server.model.*;
import kohgylw.kiftd.server.pojo.VideoInfo;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.util.*;
import ws.schild.jave.MultimediaObject;

/**
 *
 * <h2>视频播放服务实现类</h2>
 * <p>
 * 该类实现了 PlayVideoService 接口中定义的视频播放相关业务逻辑，
 * 包括检测视频文件格式、判断是否需要转码处理以及返回视频播放信息等。
 * 对于非H264编码的MP4文件以及其他视频格式（如MKV、MOV、WebM等），
 * 如果系统启用了FFmpeg，则会标记为需要转码后再进行播放。
 * </p>
 *
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 * @see kohgylw.kiftd.server.service.PlayVideoService
 */
@Service
public class PlayVideoServiceImpl implements PlayVideoService {
	@Resource
	private NodeMapper fm;
	@Resource
	private Gson gson;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private LogUtil lu;
	@Resource
	private FolderMapper flm;
	@Resource
	private FolderUtil fu;
	@Resource
	private KiftdFFMPEGLocator kfl;

	private VideoInfo foundVideo(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		if (fileId != null && fileId.length() > 0) {
			final Node f = this.fm.selectById(fileId);
			if (f != null) {
				final VideoInfo vi = new VideoInfo(f);
				final String account = (String) request.getSession().getAttribute("ACCOUNT");
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(f.getFileParentFolder()))
						&& ConfigureReader.instance().accessFolder(flm.selectById(f.getFileParentFolder()), account)) {
					final String fileName = f.getFileName();
					// 检查视频格式
					final String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
					switch (suffix) {
					case "mp4":
						if (kfl.isEnableFFmpeg()) {
							// 因此对于mp4后缀的视频，进一步检查其编码是否为h264，如果是，则允许直接播放
							File target = fbu.getFileFromBlocks(f);
							if (target == null || !target.isFile()) {
								return null;
							}
							MultimediaObject mo = new MultimediaObject(target, kfl);
							try {
								if (mo.getInfo().getVideo().getDecoder().indexOf("h264") >= 0) {
									vi.setNeedEncode("N");
									return vi;
								}
							} catch (Exception e) {
								Printer.instance
										.print("错误：视频文件“" + f.getFileName() + "”在解析时出现意外错误。详细信息：" + e.getMessage());
								lu.writeException(e);
							}
							// 对于其他编码格式，则设定需要转码
							vi.setNeedEncode("Y");
						} else {
							vi.setNeedEncode("N");// 如果禁用了ffmpeg，那么怎么都不需要转码
						}
						return vi;
					case "mkv":
					case "mov":
					case "webm":
					case "avi":
					case "wmv":
					case "flv":
						if (kfl.isEnableFFmpeg()) {
							vi.setNeedEncode("Y");
						} else {
							vi.setNeedEncode("N");
						}
						return vi;
					default:
						break;
					}
				}
			}
		}
		return null;
	}

	@Override
	public String getPlayVideoJson(final HttpServletRequest request) {
		final VideoInfo v = this.foundVideo(request);
		if (v != null) {
			return gson.toJson((Object) v);
		}
		return "ERROR";
	}
}

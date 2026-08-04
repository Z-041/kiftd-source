package kohgylw.kiftd.mc;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import kohgylw.kiftd.newcore.KiftdApplication;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.LogLevel;
import kohgylw.kiftd.server.enumeration.VCLevel;
import kohgylw.kiftd.server.pojo.ExtendStores;
import kohgylw.kiftd.server.pojo.ServerSetting;
import kohgylw.kiftd.server.util.ConfigurationManager;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.ui.callback.GetServerStatus;
import kohgylw.kiftd.ui.callback.UpdateSetting;
import kohgylw.kiftd.ui.module.ServerUIModule;
import kohgylw.kiftd.ui.pojo.FileSystemPath;

public class UIRunner {

	private static UIRunner ui;

	private UIRunner() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			try {
				Printer.init(true);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		final ServerUIModule ui = ServerUIModule.getInsatnce();
		KiftdApplication app = new KiftdApplication();
		ServerUIModule.setStartServer(() -> app.start());
		ServerUIModule.setOnCloseServer(() -> app.stop());
		ServerUIModule.setGetServerTime(() -> ServerTimeUtil.getServerTime());
		ServerUIModule.setGetServerStatus(new GetServerStatus() {

			@Override
			public boolean getServerStatus() {
				return app.isRunning();
			}

			@Override
			public int getPropertiesStatus() {
				return ConfigurationManager.instance().getStatus();
			}

			@Override
			public int getPort() {
				return ConfigurationManager.instance().getPort();
			}

			@Override
			public boolean getMustLogin() {
				return ConfigurationManager.instance().mustLogin();
			}

			@Override
			public LogLevel getLogLevel() {
				return ConfigurationManager.instance().getLogLevel();
			}

			@Override
			public String getFileSystemPath() {
				return ConfigurationManager.instance().getFileSystemPath();
			}

			@Override
			public int getBufferSize() {
				return ConfigurationManager.instance().getBuffSize();
			}

			@Override
			public VCLevel getVCLevel() {
				return ConfigurationManager.instance().getVCLevel();
			}

			@Override
			public List<FileSystemPath> getExtendStores() {
				List<FileSystemPath> fsps = new ArrayList<FileSystemPath>();
				for (ExtendStores es : ConfigurationManager.instance().getExtendStores()) {
					FileSystemPath fsp = new FileSystemPath();
					fsp.setIndex(es.getIndex());
					fsp.setPath(es.getPath());
					fsp.setType(FileSystemPath.EXTEND_STORES_NAME);
					fsps.add(fsp);
				}
				return fsps;
			}

			@Override
			public LogLevel getInitLogLevel() {
				return ConfigurationManager.instance().getInitLogLevel();
			}

			@Override
			public VCLevel getInitVCLevel() {
				return ConfigurationManager.instance().getInitVCLevel();
			}

			@Override
			public String getInitFileSystemPath() {
				return ConfigurationManager.instance().getInitFileSystemPath();
			}

			@Override
			public String getInitProt() {
				return ConfigurationManager.instance().getInitPort();
			}

			@Override
			public String getInitBufferSize() {
				return ConfigurationManager.instance().getInitBuffSize();
			}

			@Override
			public boolean isAllowChangePassword() {
				return ConfigurationManager.instance().isAllowChangePassword();
			}

			@Override
			public boolean isOpenFileChain() {
				return ConfigurationManager.instance().isOpenFileChain();
			}

			@Override
			public int getMaxExtendStoresNum() {
				return ConfigurationManager.instance().getMaxExtendstoresNum();
			}
		});
		ServerUIModule.setUpdateSetting(new UpdateSetting() {

			@Override
			public boolean update(ServerSetting s) {
				return ConfigurationManager.instance().doUpdate(s);
			}
		});
		SwingUtilities.invokeLater(() -> {
			ui.show();
		});
	}

	public static UIRunner build() throws Exception {
		if (UIRunner.ui == null) {
			UIRunner.ui = new UIRunner();
		}
		return UIRunner.ui;
	}
}

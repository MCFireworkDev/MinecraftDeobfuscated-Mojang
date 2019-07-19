package com.mojang.realmsclient.gui.screens;

import com.google.common.util.concurrent.RateLimiter;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.realmsclient.client.FileUpload;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.UploadStatus;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.util.UploadTokenCache;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.realms.Realms;
import net.minecraft.realms.RealmsButton;
import net.minecraft.realms.RealmsDefaultVertexFormat;
import net.minecraft.realms.RealmsLevelSummary;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.realms.Tezzelator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsUploadScreen extends RealmsScreen {
	private static final Logger LOGGER = LogManager.getLogger();
	private final RealmsResetWorldScreen lastScreen;
	private final RealmsLevelSummary selectedLevel;
	private final long worldId;
	private final int slotId;
	private final UploadStatus uploadStatus;
	private final RateLimiter narrationRateLimiter;
	private volatile String errorMessage;
	private volatile String status;
	private volatile String progress;
	private volatile boolean cancelled;
	private volatile boolean uploadFinished;
	private volatile boolean showDots = true;
	private volatile boolean uploadStarted;
	private RealmsButton backButton;
	private RealmsButton cancelButton;
	private int animTick;
	private static final String[] DOTS = new String[]{"", ".", ". .", ". . ."};
	private int dotIndex;
	private Long previousWrittenBytes;
	private Long previousTimeSnapshot;
	private long bytesPersSecond;
	private static final ReentrantLock uploadLock = new ReentrantLock();

	public RealmsUploadScreen(long l, int i, RealmsResetWorldScreen realmsResetWorldScreen, RealmsLevelSummary realmsLevelSummary) {
		this.worldId = l;
		this.slotId = i;
		this.lastScreen = realmsResetWorldScreen;
		this.selectedLevel = realmsLevelSummary;
		this.uploadStatus = new UploadStatus();
		this.narrationRateLimiter = RateLimiter.create(0.1F);
	}

	@Override
	public void init() {
		this.setKeyboardHandlerSendRepeatsToGui(true);
		this.backButton = new RealmsButton(1, this.width() / 2 - 100, this.height() - 42, 200, 20, getLocalizedString("gui.back")) {
			@Override
			public void onPress() {
				RealmsUploadScreen.this.onBack();
			}
		};
		this.buttonsAdd(this.cancelButton = new RealmsButton(0, this.width() / 2 - 100, this.height() - 42, 200, 20, getLocalizedString("gui.cancel")) {
			@Override
			public void onPress() {
				RealmsUploadScreen.this.onCancel();
			}
		});
		if (!this.uploadStarted) {
			if (this.lastScreen.slot == -1) {
				this.upload();
			} else {
				this.lastScreen.switchSlot(this);
			}
		}
	}

	@Override
	public void confirmResult(boolean bl, int i) {
		if (bl && !this.uploadStarted) {
			this.uploadStarted = true;
			Realms.setScreen(this);
			this.upload();
		}
	}

	@Override
	public void removed() {
		this.setKeyboardHandlerSendRepeatsToGui(false);
	}

	private void onBack() {
		this.lastScreen.confirmResult(true, 0);
	}

	private void onCancel() {
		this.cancelled = true;
		Realms.setScreen(this.lastScreen);
	}

	@Override
	public boolean keyPressed(int i, int j, int k) {
		if (i == 256) {
			if (this.showDots) {
				this.onCancel();
			} else {
				this.onBack();
			}

			return true;
		} else {
			return super.keyPressed(i, j, k);
		}
	}

	@Override
	public void render(int i, int j, float f) {
		this.renderBackground();
		if (!this.uploadFinished && this.uploadStatus.bytesWritten != 0L && this.uploadStatus.bytesWritten == this.uploadStatus.totalBytes) {
			this.status = getLocalizedString("mco.upload.verifying");
			this.cancelButton.active(false);
		}

		this.drawCenteredString(this.status, this.width() / 2, 50, 16777215);
		if (this.showDots) {
			this.drawDots();
		}

		if (this.uploadStatus.bytesWritten != 0L && !this.cancelled) {
			this.drawProgressBar();
			this.drawUploadSpeed();
		}

		if (this.errorMessage != null) {
			String[] strings = this.errorMessage.split("\\\\n");

			for(int k = 0; k < strings.length; ++k) {
				this.drawCenteredString(strings[k], this.width() / 2, 110 + 12 * k, 16711680);
			}
		}

		super.render(i, j, f);
	}

	private void drawDots() {
		int i = this.fontWidth(this.status);
		if (this.animTick % 10 == 0) {
			++this.dotIndex;
		}

		this.drawString(DOTS[this.dotIndex % DOTS.length], this.width() / 2 + i / 2 + 5, 50, 16777215);
	}

	private void drawProgressBar() {
		double d = this.uploadStatus.bytesWritten.doubleValue() / this.uploadStatus.totalBytes.doubleValue() * 100.0;
		if (d > 100.0) {
			d = 100.0;
		}

		this.progress = String.format(Locale.ROOT, "%.1f", d);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.disableTexture();
		double e = (double)(this.width() / 2 - 100);
		double f = 0.5;
		Tezzelator tezzelator = Tezzelator.instance;
		tezzelator.begin(7, RealmsDefaultVertexFormat.POSITION_COLOR);
		tezzelator.vertex(e - 0.5, 95.5, 0.0).color(217, 210, 210, 255).endVertex();
		tezzelator.vertex(e + 200.0 * d / 100.0 + 0.5, 95.5, 0.0).color(217, 210, 210, 255).endVertex();
		tezzelator.vertex(e + 200.0 * d / 100.0 + 0.5, 79.5, 0.0).color(217, 210, 210, 255).endVertex();
		tezzelator.vertex(e - 0.5, 79.5, 0.0).color(217, 210, 210, 255).endVertex();
		tezzelator.vertex(e, 95.0, 0.0).color(128, 128, 128, 255).endVertex();
		tezzelator.vertex(e + 200.0 * d / 100.0, 95.0, 0.0).color(128, 128, 128, 255).endVertex();
		tezzelator.vertex(e + 200.0 * d / 100.0, 80.0, 0.0).color(128, 128, 128, 255).endVertex();
		tezzelator.vertex(e, 80.0, 0.0).color(128, 128, 128, 255).endVertex();
		tezzelator.end();
		GlStateManager.enableTexture();
		this.drawCenteredString(this.progress + " %", this.width() / 2, 84, 16777215);
	}

	private void drawUploadSpeed() {
		if (this.animTick % 20 == 0) {
			if (this.previousWrittenBytes != null) {
				long l = System.currentTimeMillis() - this.previousTimeSnapshot;
				if (l == 0L) {
					l = 1L;
				}

				this.bytesPersSecond = 1000L * (this.uploadStatus.bytesWritten - this.previousWrittenBytes) / l;
				this.drawUploadSpeed0(this.bytesPersSecond);
			}

			this.previousWrittenBytes = this.uploadStatus.bytesWritten;
			this.previousTimeSnapshot = System.currentTimeMillis();
		} else {
			this.drawUploadSpeed0(this.bytesPersSecond);
		}
	}

	private void drawUploadSpeed0(long l) {
		if (l > 0L) {
			int i = this.fontWidth(this.progress);
			String string = "(" + humanReadableByteCount(l) + ")";
			this.drawString(string, this.width() / 2 + i / 2 + 15, 84, 16777215);
		}
	}

	public static String humanReadableByteCount(long l) {
		int i = 1024;
		if (l < 1024L) {
			return l + " B";
		} else {
			int j = (int)(Math.log((double)l) / Math.log(1024.0));
			String string = "KMGTPE".charAt(j - 1) + "";
			return String.format(Locale.ROOT, "%.1f %sB/s", (double)l / Math.pow(1024.0, (double)j), string);
		}
	}

	@Override
	public void tick() {
		super.tick();
		++this.animTick;
		if (this.status != null && this.narrationRateLimiter.tryAcquire(1)) {
			ArrayList<String> arrayList = new ArrayList();
			arrayList.add(this.status);
			if (this.progress != null) {
				arrayList.add(this.progress + "%");
			}

			if (this.errorMessage != null) {
				arrayList.add(this.errorMessage);
			}

			Realms.narrateNow(String.join(System.lineSeparator(), arrayList));
		}
	}

	public static RealmsUploadScreen.Unit getLargestUnit(long l) {
		if (l < 1024L) {
			return RealmsUploadScreen.Unit.B;
		} else {
			int i = (int)(Math.log((double)l) / Math.log(1024.0));
			String string = "KMGTPE".charAt(i - 1) + "";

			try {
				return RealmsUploadScreen.Unit.valueOf(string + "B");
			} catch (Exception var5) {
				return RealmsUploadScreen.Unit.GB;
			}
		}
	}

	public static double convertToUnit(long l, RealmsUploadScreen.Unit unit) {
		return unit.equals(RealmsUploadScreen.Unit.B) ? (double)l : (double)l / Math.pow(1024.0, (double)unit.ordinal());
	}

	public static String humanReadableSize(long l, RealmsUploadScreen.Unit unit) {
		return String.format("%." + (unit.equals(RealmsUploadScreen.Unit.GB) ? "1" : "0") + "f %s", convertToUnit(l, unit), unit.name());
	}

	private void upload() {
		this.uploadStarted = true;
		(new Thread() {
				public void run() {
					File file = null;
					RealmsClient realmsClient = RealmsClient.createRealmsClient();
					long l = RealmsUploadScreen.this.worldId;
	
					try {
						UploadInfo uploadInfo;
						try {
							if (RealmsUploadScreen.uploadLock.tryLock(1L, TimeUnit.SECONDS)) {
								RealmsUploadScreen.this.status = RealmsScreen.getLocalizedString("mco.upload.preparing");
								uploadInfo = null;
	
								int i;
								for(i = 0; i < 20; ++i) {
									try {
										if (RealmsUploadScreen.this.cancelled) {
											RealmsUploadScreen.this.uploadCancelled();
											return;
										}
	
										uploadInfo = realmsClient.upload(l, UploadTokenCache.get(l));
										break;
									} catch (RetryCallException var20) {
										Thread.sleep((long)(var20.delaySeconds * 1000));
									}
								}
	
								if (uploadInfo == null) {
									RealmsUploadScreen.this.status = RealmsScreen.getLocalizedString("mco.upload.close.failure");
								} else {
									UploadTokenCache.put(l, uploadInfo.getToken());
									if (!uploadInfo.isWorldClosed()) {
										RealmsUploadScreen.this.status = RealmsScreen.getLocalizedString("mco.upload.close.failure");
									} else if (RealmsUploadScreen.this.cancelled) {
										RealmsUploadScreen.this.uploadCancelled();
									} else {
										i = (int)(new File(Realms.getGameDirectoryPath(), "saves"));
										file = RealmsUploadScreen.this.tarGzipArchive(new File(i, RealmsUploadScreen.this.selectedLevel.getLevelId()));
										if (RealmsUploadScreen.this.cancelled) {
											RealmsUploadScreen.this.uploadCancelled();
										} else if (RealmsUploadScreen.this.verify(file)) {
											RealmsUploadScreen.this.status = RealmsScreen.getLocalizedString("mco.upload.uploading", RealmsUploadScreen.this.selectedLevel.getLevelName());
											FileUpload fileUpload = new FileUpload(
												file,
												RealmsUploadScreen.this.worldId,
												RealmsUploadScreen.this.slotId,
												uploadInfo,
												Realms.getSessionId(),
												Realms.getName(),
												Realms.getMinecraftVersionString(),
												RealmsUploadScreen.this.uploadStatus
											);
											fileUpload.upload(uploadResult -> {
												if (uploadResult.statusCode >= 200 && uploadResult.statusCode < 300) {
													RealmsUploadScreen.this.uploadFinished = true;
													RealmsUploadScreen.this.status = RealmsScreen.getLocalizedString("mco.upload.done");
													RealmsUploadScreen.this.backButton.setMessage(RealmsScreen.getLocalizedString("gui.done"));
													UploadTokenCache.invalidate(l);
												} else if (uploadResult.statusCode == 400 && uploadResult.errorMessage != null) {
													RealmsUploadScreen.this.errorMessage = RealmsScreen.getLocalizedString("mco.upload.failed", uploadResult.errorMessage);
												} else {
													RealmsUploadScreen.this.errorMessage = RealmsScreen.getLocalizedString("mco.upload.failed", uploadResult.statusCode);
												}
											});
	
											while(!fileUpload.isFinished()) {
												if (RealmsUploadScreen.this.cancelled) {
													fileUpload.cancel();
													RealmsUploadScreen.this.uploadCancelled();
													return;
												}
	
												try {
													Thread.sleep(500L);
												} catch (InterruptedException var19) {
													RealmsUploadScreen.LOGGER.error("Failed to check Realms file upload status");
												}
											}
										} else {
											long m = file.length();
											RealmsUploadScreen.Unit unit = RealmsUploadScreen.getLargestUnit(m);
											RealmsUploadScreen.Unit unit2 = RealmsUploadScreen.getLargestUnit(5368709120L);
											if (RealmsUploadScreen.humanReadableSize(m, unit).equals(RealmsUploadScreen.humanReadableSize(5368709120L, unit2))
												&& unit != RealmsUploadScreen.Unit.B) {
												RealmsUploadScreen.Unit unit3 = RealmsUploadScreen.Unit.values()[unit.ordinal() - 1];
												RealmsUploadScreen.this.errorMessage = RealmsScreen.getLocalizedString(
														"mco.upload.size.failure.line1", RealmsUploadScreen.this.selectedLevel.getLevelName()
													)
													+ "\\n"
													+ RealmsScreen.getLocalizedString(
														"mco.upload.size.failure.line2", RealmsUploadScreen.humanReadableSize(m, unit3), RealmsUploadScreen.humanReadableSize(5368709120L, unit3)
													);
											} else {
												RealmsUploadScreen.this.errorMessage = RealmsScreen.getLocalizedString(
														"mco.upload.size.failure.line1", RealmsUploadScreen.this.selectedLevel.getLevelName()
													)
													+ "\\n"
													+ RealmsScreen.getLocalizedString(
														"mco.upload.size.failure.line2", RealmsUploadScreen.humanReadableSize(m, unit), RealmsUploadScreen.humanReadableSize(5368709120L, unit2)
													);
											}
										}
									}
								}
							}
						} catch (IOException var21) {
							uploadInfo = var21;
							RealmsUploadScreen.this.errorMessage = RealmsScreen.getLocalizedString("mco.upload.failed", var21.getMessage());
						} catch (RealmsServiceException var22) {
							uploadInfo = var22;
							RealmsUploadScreen.this.errorMessage = RealmsScreen.getLocalizedString("mco.upload.failed", var22.toString());
						} catch (InterruptedException var23) {
							uploadInfo = var23;
							RealmsUploadScreen.LOGGER.error("Could not acquire upload lock");
						}
					} finally {
						RealmsUploadScreen.this.uploadFinished = true;
						if (RealmsUploadScreen.uploadLock.isHeldByCurrentThread()) {
							RealmsUploadScreen.uploadLock.unlock();
							RealmsUploadScreen.this.showDots = false;
							RealmsUploadScreen.this.childrenClear();
							RealmsUploadScreen.this.buttonsAdd(RealmsUploadScreen.this.backButton);
							if (file != null) {
								RealmsUploadScreen.LOGGER.debug("Deleting file " + file.getAbsolutePath());
								file.delete();
							}
						} else {
							return;
						}
					}
				}
			})
			.start();
	}

	private void uploadCancelled() {
		this.status = getLocalizedString("mco.upload.cancelled");
		LOGGER.debug("Upload was cancelled");
	}

	private boolean verify(File file) {
		return file.length() < 5368709120L;
	}

	private File tarGzipArchive(File file) throws IOException {
		TarArchiveOutputStream tarArchiveOutputStream = null;

		File var4;
		try {
			File file2 = File.createTempFile("realms-upload-file", ".tar.gz");
			tarArchiveOutputStream = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(file2)));
			tarArchiveOutputStream.setLongFileMode(3);
			this.addFileToTarGz(tarArchiveOutputStream, file.getAbsolutePath(), "world", true);
			tarArchiveOutputStream.finish();
			var4 = file2;
		} finally {
			if (tarArchiveOutputStream != null) {
				tarArchiveOutputStream.close();
			}
		}

		return var4;
	}

	private void addFileToTarGz(TarArchiveOutputStream tarArchiveOutputStream, String string, String string2, boolean bl) throws IOException {
		if (!this.cancelled) {
			File file = new File(string);
			String string3 = bl ? string2 : string2 + file.getName();
			TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(file, string3);
			tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
			if (file.isFile()) {
				IOUtils.copy(new FileInputStream(file), tarArchiveOutputStream);
				tarArchiveOutputStream.closeArchiveEntry();
			} else {
				tarArchiveOutputStream.closeArchiveEntry();
				File[] files = file.listFiles();
				if (files != null) {
					for(File file2 : files) {
						this.addFileToTarGz(tarArchiveOutputStream, file2.getAbsolutePath(), string3 + "/", false);
					}
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	static enum Unit {
		B,
		KB,
		MB,
		GB;
	}
}

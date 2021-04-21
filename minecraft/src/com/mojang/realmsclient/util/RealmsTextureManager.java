package com.mojang.realmsclient.util;

import com.google.common.collect.Maps;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.util.UUIDTypeAdapter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsTextureManager {
	private static final Map<String, RealmsTextureManager.RealmsTexture> TEXTURES = Maps.newHashMap();
	private static final Map<String, Boolean> SKIN_FETCH_STATUS = Maps.newHashMap();
	private static final Map<String, String> FETCHED_SKINS = Maps.newHashMap();
	private static final Logger LOGGER = LogManager.getLogger();
	private static final ResourceLocation TEMPLATE_ICON_LOCATION = new ResourceLocation("textures/gui/presets/isles.png");

	public static void bindWorldTemplate(String string, @Nullable String string2) {
		if (string2 == null) {
			RenderSystem.setShaderTexture(0, TEMPLATE_ICON_LOCATION);
		} else {
			int i = getTextureId(string, string2);
			RenderSystem.setShaderTexture(0, i);
		}
	}

	public static void withBoundFace(String string, Runnable runnable) {
		bindFace(string);
		runnable.run();
	}

	private static void bindDefaultFace(UUID uUID) {
		RenderSystem.setShaderTexture(0, DefaultPlayerSkin.getDefaultSkin(uUID));
	}

	private static void bindFace(String string) {
		UUID uUID = UUIDTypeAdapter.fromString(string);
		if (TEXTURES.containsKey(string)) {
			int i = ((RealmsTextureManager.RealmsTexture)TEXTURES.get(string)).textureId;
			RenderSystem.setShaderTexture(0, i);
		} else if (SKIN_FETCH_STATUS.containsKey(string)) {
			if (!SKIN_FETCH_STATUS.get(string)) {
				bindDefaultFace(uUID);
			} else if (FETCHED_SKINS.containsKey(string)) {
				int i = getTextureId(string, (String)FETCHED_SKINS.get(string));
				RenderSystem.setShaderTexture(0, i);
			} else {
				bindDefaultFace(uUID);
			}
		} else {
			SKIN_FETCH_STATUS.put(string, false);
			bindDefaultFace(uUID);
			Thread thread = new Thread("Realms Texture Downloader") {
				public void run() {
					Map<Type, MinecraftProfileTexture> map = RealmsUtil.getTextures(string);
					if (map.containsKey(Type.SKIN)) {
						MinecraftProfileTexture minecraftProfileTexture = (MinecraftProfileTexture)map.get(Type.SKIN);
						String stringx = minecraftProfileTexture.getUrl();
						HttpURLConnection httpURLConnection = null;
						RealmsTextureManager.LOGGER.debug("Downloading http texture from {}", stringx);

						try {
							try {
								httpURLConnection = (HttpURLConnection)new URL(stringx).openConnection(Minecraft.getInstance().getProxy());
								httpURLConnection.setDoInput(true);
								httpURLConnection.setDoOutput(false);
								httpURLConnection.connect();
								if (httpURLConnection.getResponseCode() / 100 != 2) {
									RealmsTextureManager.SKIN_FETCH_STATUS.remove(string);
									return;
								}

								BufferedImage bufferedImage;
								try {
									bufferedImage = ImageIO.read(httpURLConnection.getInputStream());
								} catch (Exception var17) {
									RealmsTextureManager.SKIN_FETCH_STATUS.remove(string);
									return;
								} finally {
									IOUtils.closeQuietly(httpURLConnection.getInputStream());
								}

								bufferedImage = new SkinProcessor().process(bufferedImage);
								ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
								ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
								RealmsTextureManager.FETCHED_SKINS.put(string, new Base64().encodeToString(byteArrayOutputStream.toByteArray()));
								RealmsTextureManager.SKIN_FETCH_STATUS.put(string, true);
							} catch (Exception var19) {
								RealmsTextureManager.LOGGER.error("Couldn't download http texture", var19);
								RealmsTextureManager.SKIN_FETCH_STATUS.remove(string);
							}
						} finally {
							if (httpURLConnection != null) {
								httpURLConnection.disconnect();
							}
						}
					} else {
						RealmsTextureManager.SKIN_FETCH_STATUS.put(string, true);
					}
				}
			};
			thread.setDaemon(true);
			thread.start();
		}
	}

	private static int getTextureId(String string, String string2) {
		RealmsTextureManager.RealmsTexture realmsTexture = (RealmsTextureManager.RealmsTexture)TEXTURES.get(string);
		if (realmsTexture != null && realmsTexture.image.equals(string2)) {
			return realmsTexture.textureId;
		} else {
			int i;
			if (realmsTexture != null) {
				i = realmsTexture.textureId;
			} else {
				i = GlStateManager._genTexture();
			}

			IntBuffer intBuffer = null;
			int j = 0;
			int k = 0;

			try {
				InputStream inputStream = new ByteArrayInputStream(new Base64().decode(string2));

				BufferedImage bufferedImage;
				try {
					bufferedImage = ImageIO.read(inputStream);
				} finally {
					IOUtils.closeQuietly(inputStream);
				}

				j = bufferedImage.getWidth();
				k = bufferedImage.getHeight();
				int[] is = new int[j * k];
				bufferedImage.getRGB(0, 0, j, k, is, 0, j);
				intBuffer = ByteBuffer.allocateDirect(4 * j * k).order(ByteOrder.nativeOrder()).asIntBuffer();
				intBuffer.put(is);
				intBuffer.flip();
			} catch (IOException var13) {
				var13.printStackTrace();
			}

			RenderSystem.activeTexture(33984);
			RenderSystem.bindTextureForSetup(i);
			TextureUtil.initTexture(intBuffer, j, k);
			TEXTURES.put(string, new RealmsTextureManager.RealmsTexture(string2, i));
			return i;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class RealmsTexture {
		private final String image;
		private final int textureId;

		public RealmsTexture(String string, int i) {
			this.image = string;
			this.textureId = i;
		}
	}
}

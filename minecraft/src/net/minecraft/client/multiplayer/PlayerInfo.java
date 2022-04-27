package net.minecraft.client.multiplayer;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.logging.LogUtils;
import java.util.Map;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CryptException;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class PlayerInfo {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final GameProfile profile;
	private final Map<Type, ResourceLocation> textureLocations = Maps.newEnumMap(Type.class);
	private GameType gameMode;
	private int latency;
	private boolean pendingTextures;
	@Nullable
	private String skinModel;
	@Nullable
	private Component tabListDisplayName;
	private int lastHealth;
	private int displayHealth;
	private long lastHealthTime;
	private long healthBlinkTime;
	private long renderVisibilityId;
	@Nullable
	private final ProfilePublicKey.Trusted profilePublicKey;

	public PlayerInfo(ClientboundPlayerInfoPacket.PlayerUpdate playerUpdate, MinecraftSessionService minecraftSessionService) {
		this.profile = playerUpdate.getProfile();
		this.gameMode = playerUpdate.getGameMode();
		this.latency = playerUpdate.getLatency();
		this.tabListDisplayName = playerUpdate.getDisplayName();
		ProfilePublicKey.Trusted trusted = null;

		try {
			ProfilePublicKey profilePublicKey = (ProfilePublicKey)ProfilePublicKey.parseFromGameProfile(this.profile).orElse(null);
			if (profilePublicKey != null) {
				trusted = profilePublicKey.verify(minecraftSessionService);
			}
		} catch (InsecurePublicKeyException | CryptException var5) {
			LOGGER.error("Failed to retrieve publicKey property for profile {}", this.profile.getId(), var5);
		}

		this.profilePublicKey = trusted;
	}

	public GameProfile getProfile() {
		return this.profile;
	}

	@Nullable
	public ProfilePublicKey.Trusted getProfilePublicKey() {
		return this.profilePublicKey;
	}

	@Nullable
	public GameType getGameMode() {
		return this.gameMode;
	}

	protected void setGameMode(GameType gameType) {
		this.gameMode = gameType;
	}

	public int getLatency() {
		return this.latency;
	}

	protected void setLatency(int i) {
		this.latency = i;
	}

	public boolean isCapeLoaded() {
		return this.getCapeLocation() != null;
	}

	public boolean isSkinLoaded() {
		return this.getSkinLocation() != null;
	}

	public String getModelName() {
		return this.skinModel == null ? DefaultPlayerSkin.getSkinModelName(this.profile.getId()) : this.skinModel;
	}

	public ResourceLocation getSkinLocation() {
		this.registerTextures();
		return MoreObjects.firstNonNull((ResourceLocation)this.textureLocations.get(Type.SKIN), DefaultPlayerSkin.getDefaultSkin(this.profile.getId()));
	}

	@Nullable
	public ResourceLocation getCapeLocation() {
		this.registerTextures();
		return (ResourceLocation)this.textureLocations.get(Type.CAPE);
	}

	@Nullable
	public ResourceLocation getElytraLocation() {
		this.registerTextures();
		return (ResourceLocation)this.textureLocations.get(Type.ELYTRA);
	}

	@Nullable
	public PlayerTeam getTeam() {
		return Minecraft.getInstance().level.getScoreboard().getPlayersTeam(this.getProfile().getName());
	}

	protected void registerTextures() {
		synchronized(this) {
			if (!this.pendingTextures) {
				this.pendingTextures = true;
				Minecraft.getInstance().getSkinManager().registerSkins(this.profile, (type, resourceLocation, minecraftProfileTexture) -> {
					this.textureLocations.put(type, resourceLocation);
					if (type == Type.SKIN) {
						this.skinModel = minecraftProfileTexture.getMetadata("model");
						if (this.skinModel == null) {
							this.skinModel = "default";
						}
					}
				}, true);
			}
		}
	}

	public void setTabListDisplayName(@Nullable Component component) {
		this.tabListDisplayName = component;
	}

	@Nullable
	public Component getTabListDisplayName() {
		return this.tabListDisplayName;
	}

	public int getLastHealth() {
		return this.lastHealth;
	}

	public void setLastHealth(int i) {
		this.lastHealth = i;
	}

	public int getDisplayHealth() {
		return this.displayHealth;
	}

	public void setDisplayHealth(int i) {
		this.displayHealth = i;
	}

	public long getLastHealthTime() {
		return this.lastHealthTime;
	}

	public void setLastHealthTime(long l) {
		this.lastHealthTime = l;
	}

	public long getHealthBlinkTime() {
		return this.healthBlinkTime;
	}

	public void setHealthBlinkTime(long l) {
		this.healthBlinkTime = l;
	}

	public long getRenderVisibilityId() {
		return this.renderVisibilityId;
	}

	public void setRenderVisibilityId(long l) {
		this.renderVisibilityId = l;
	}
}

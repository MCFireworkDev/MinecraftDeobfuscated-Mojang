package net.minecraft.client.multiplayer;

import com.google.common.base.Suppliers;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignedMessageValidator;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

@Environment(EnvType.CLIENT)
public class PlayerInfo {
	private final GameProfile profile;
	private final Supplier<PlayerSkin> skinLookup;
	private GameType gameMode = GameType.DEFAULT_MODE;
	private int latency;
	@Nullable
	private Component tabListDisplayName;
	@Nullable
	private RemoteChatSession chatSession;
	private SignedMessageValidator messageValidator;

	public PlayerInfo(GameProfile gameProfile, boolean bl) {
		this.profile = gameProfile;
		this.messageValidator = fallbackMessageValidator(bl);
		Supplier<Supplier<PlayerSkin>> supplier = Suppliers.memoize(() -> createSkinLookup(gameProfile));
		this.skinLookup = () -> (PlayerSkin)((Supplier)supplier.get()).get();
	}

	private static Supplier<PlayerSkin> createSkinLookup(GameProfile gameProfile) {
		Minecraft minecraft = Minecraft.getInstance();
		CompletableFuture<PlayerSkin> completableFuture = loadSkin(gameProfile, minecraft.getSkinManager(), minecraft.getMinecraftSessionService());
		boolean bl = !minecraft.isLocalPlayer(gameProfile.getId());
		PlayerSkin playerSkin = DefaultPlayerSkin.get(gameProfile);
		return () -> {
			PlayerSkin playerSkin2 = (PlayerSkin)completableFuture.getNow(playerSkin);
			return bl && !playerSkin2.secure() ? playerSkin : playerSkin2;
		};
	}

	private static CompletableFuture<PlayerSkin> loadSkin(GameProfile gameProfile, SkinManager skinManager, MinecraftSessionService minecraftSessionService) {
		CompletableFuture<GameProfile> completableFuture;
		if (skinManager.hasSecureTextureData(gameProfile)) {
			completableFuture = CompletableFuture.completedFuture(gameProfile);
		} else {
			completableFuture = CompletableFuture.supplyAsync(() -> fillProfileProperties(gameProfile, minecraftSessionService), Util.ioPool());
		}

		return completableFuture.thenCompose(skinManager::getOrLoad);
	}

	private static GameProfile fillProfileProperties(GameProfile gameProfile, MinecraftSessionService minecraftSessionService) {
		Minecraft minecraft = Minecraft.getInstance();
		gameProfile.getProperties().clear();
		if (minecraft.isLocalPlayer(gameProfile.getId())) {
			gameProfile.getProperties().putAll(minecraft.getProfileProperties());
		} else {
			GameProfile gameProfile2 = minecraftSessionService.fetchProfile(gameProfile.getId(), true);
			if (gameProfile2 != null) {
				gameProfile2.getProperties().putAll(gameProfile2.getProperties());
			}
		}

		return gameProfile;
	}

	public GameProfile getProfile() {
		return this.profile;
	}

	@Nullable
	public RemoteChatSession getChatSession() {
		return this.chatSession;
	}

	public SignedMessageValidator getMessageValidator() {
		return this.messageValidator;
	}

	public boolean hasVerifiableChat() {
		return this.chatSession != null;
	}

	protected void setChatSession(RemoteChatSession remoteChatSession) {
		this.chatSession = remoteChatSession;
		this.messageValidator = remoteChatSession.createMessageValidator();
	}

	protected void clearChatSession(boolean bl) {
		this.chatSession = null;
		this.messageValidator = fallbackMessageValidator(bl);
	}

	private static SignedMessageValidator fallbackMessageValidator(boolean bl) {
		return bl ? SignedMessageValidator.REJECT_ALL : SignedMessageValidator.ACCEPT_UNSIGNED;
	}

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

	public PlayerSkin getSkin() {
		return (PlayerSkin)this.skinLookup.get();
	}

	@Nullable
	public PlayerTeam getTeam() {
		return Minecraft.getInstance().level.getScoreboard().getPlayersTeam(this.getProfile().getName());
	}

	public void setTabListDisplayName(@Nullable Component component) {
		this.tabListDisplayName = component;
	}

	@Nullable
	public Component getTabListDisplayName() {
		return this.tabListDisplayName;
	}
}

package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class ClientSuggestionProvider implements SharedSuggestionProvider {
	private final ClientPacketListener connection;
	private final Minecraft minecraft;
	private int pendingSuggestionsId = -1;
	@Nullable
	private CompletableFuture<Suggestions> pendingSuggestionsFuture;
	private final Set<String> customCompletionSuggestions = new HashSet();

	public ClientSuggestionProvider(ClientPacketListener clientPacketListener, Minecraft minecraft) {
		this.connection = clientPacketListener;
		this.minecraft = minecraft;
	}

	@Override
	public Collection<String> getOnlinePlayerNames() {
		List<String> list = Lists.newArrayList();

		for(PlayerInfo playerInfo : this.connection.getOnlinePlayers()) {
			list.add(playerInfo.getProfile().getName());
		}

		return list;
	}

	@Override
	public Collection<String> getCustomTabSugggestions() {
		if (this.customCompletionSuggestions.isEmpty()) {
			return this.getOnlinePlayerNames();
		} else {
			Set<String> set = new HashSet(this.getOnlinePlayerNames());
			set.addAll(this.customCompletionSuggestions);
			return set;
		}
	}

	@Override
	public Collection<String> getSelectedEntities() {
		return (Collection<String>)(this.minecraft.hitResult != null && this.minecraft.hitResult.getType() == HitResult.Type.ENTITY
			? Collections.singleton(((EntityHitResult)this.minecraft.hitResult).getEntity().getStringUUID())
			: Collections.emptyList());
	}

	@Override
	public Collection<String> getAllTeams() {
		return this.connection.getLevel().getScoreboard().getTeamNames();
	}

	@Override
	public Collection<ResourceLocation> getAvailableSoundEvents() {
		return this.minecraft.getSoundManager().getAvailableSounds();
	}

	@Override
	public Stream<ResourceLocation> getRecipeNames() {
		return this.connection.getRecipeManager().getRecipeIds();
	}

	@Override
	public boolean hasPermission(int i) {
		LocalPlayer localPlayer = this.minecraft.player;
		return localPlayer != null ? localPlayer.hasPermissions(i) : i == 0;
	}

	@Override
	public CompletableFuture<Suggestions> suggestRegistryElements(
		ResourceKey<? extends Registry<?>> resourceKey,
		SharedSuggestionProvider.ElementSuggestionType elementSuggestionType,
		SuggestionsBuilder suggestionsBuilder,
		CommandContext<?> commandContext
	) {
		return (CompletableFuture<Suggestions>)this.registryAccess().registry(resourceKey).map(registry -> {
			this.suggestRegistryElements(registry, elementSuggestionType, suggestionsBuilder);
			return suggestionsBuilder.buildFuture();
		}).orElseGet(() -> this.customSuggestion(commandContext));
	}

	@Override
	public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> commandContext) {
		if (this.pendingSuggestionsFuture != null) {
			this.pendingSuggestionsFuture.cancel(false);
		}

		this.pendingSuggestionsFuture = new CompletableFuture();
		int i = ++this.pendingSuggestionsId;
		this.connection.send(new ServerboundCommandSuggestionPacket(i, commandContext.getInput()));
		return this.pendingSuggestionsFuture;
	}

	private static String prettyPrint(double d) {
		return String.format(Locale.ROOT, "%.2f", d);
	}

	private static String prettyPrint(int i) {
		return Integer.toString(i);
	}

	@Override
	public Collection<SharedSuggestionProvider.TextCoordinates> getRelevantCoordinates() {
		HitResult hitResult = this.minecraft.hitResult;
		if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
			BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
			return Collections.singleton(
				new SharedSuggestionProvider.TextCoordinates(prettyPrint(blockPos.getX()), prettyPrint(blockPos.getY()), prettyPrint(blockPos.getZ()))
			);
		} else {
			return SharedSuggestionProvider.super.getRelevantCoordinates();
		}
	}

	@Override
	public Collection<SharedSuggestionProvider.TextCoordinates> getAbsoluteCoordinates() {
		HitResult hitResult = this.minecraft.hitResult;
		if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
			Vec3 vec3 = hitResult.getLocation();
			return Collections.singleton(new SharedSuggestionProvider.TextCoordinates(prettyPrint(vec3.x), prettyPrint(vec3.y), prettyPrint(vec3.z)));
		} else {
			return SharedSuggestionProvider.super.getAbsoluteCoordinates();
		}
	}

	@Override
	public Set<ResourceKey<Level>> levels() {
		return this.connection.levels();
	}

	@Override
	public RegistryAccess registryAccess() {
		return this.connection.registryAccess();
	}

	@Override
	public FeatureFlagSet enabledFeatures() {
		return this.connection.enabledFeatures();
	}

	public void completeCustomSuggestions(int i, Suggestions suggestions) {
		if (i == this.pendingSuggestionsId) {
			this.pendingSuggestionsFuture.complete(suggestions);
			this.pendingSuggestionsFuture = null;
			this.pendingSuggestionsId = -1;
		}
	}

	public void modifyCustomCompletions(ClientboundCustomChatCompletionsPacket.Action action, List<String> list) {
		switch(action) {
			case ADD:
				this.customCompletionSuggestions.addAll(list);
				break;
			case REMOVE:
				list.forEach(this.customCompletionSuggestions::remove);
				break;
			case SET:
				this.customCompletionSuggestions.clear();
				this.customCompletionSuggestions.addAll(list);
		}
	}
}

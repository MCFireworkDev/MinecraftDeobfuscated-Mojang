package net.minecraft.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.TaskChainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class CommandSourceStack implements SharedSuggestionProvider {
	public static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(Component.translatable("permissions.requires.player"));
	public static final SimpleCommandExceptionType ERROR_NOT_ENTITY = new SimpleCommandExceptionType(Component.translatable("permissions.requires.entity"));
	private final CommandSource source;
	private final Vec3 worldPosition;
	private final ServerLevel level;
	private final int permissionLevel;
	private final String textName;
	private final Component displayName;
	private final MinecraftServer server;
	private final boolean silent;
	@Nullable
	private final Entity entity;
	@Nullable
	private final ResultConsumer<CommandSourceStack> consumer;
	private final EntityAnchorArgument.Anchor anchor;
	private final Vec2 rotation;
	private final CommandSigningContext signingContext;
	private final TaskChainer chatMessageChainer;
	private final IntConsumer returnValueConsumer;

	public CommandSourceStack(
		CommandSource commandSource,
		Vec3 vec3,
		Vec2 vec2,
		ServerLevel serverLevel,
		int i,
		String string,
		Component component,
		MinecraftServer minecraftServer,
		@Nullable Entity entity
	) {
		this(commandSource, vec3, vec2, serverLevel, i, string, component, minecraftServer, entity, false, (commandContext, bl, ix) -> {
		}, EntityAnchorArgument.Anchor.FEET, CommandSigningContext.ANONYMOUS, TaskChainer.immediate(minecraftServer), ix -> {
		});
	}

	protected CommandSourceStack(
		CommandSource commandSource,
		Vec3 vec3,
		Vec2 vec2,
		ServerLevel serverLevel,
		int i,
		String string,
		Component component,
		MinecraftServer minecraftServer,
		@Nullable Entity entity,
		boolean bl,
		@Nullable ResultConsumer<CommandSourceStack> resultConsumer,
		EntityAnchorArgument.Anchor anchor,
		CommandSigningContext commandSigningContext,
		TaskChainer taskChainer,
		IntConsumer intConsumer
	) {
		this.source = commandSource;
		this.worldPosition = vec3;
		this.level = serverLevel;
		this.silent = bl;
		this.entity = entity;
		this.permissionLevel = i;
		this.textName = string;
		this.displayName = component;
		this.server = minecraftServer;
		this.consumer = resultConsumer;
		this.anchor = anchor;
		this.rotation = vec2;
		this.signingContext = commandSigningContext;
		this.chatMessageChainer = taskChainer;
		this.returnValueConsumer = intConsumer;
	}

	public CommandSourceStack withSource(CommandSource commandSource) {
		return this.source == commandSource
			? this
			: new CommandSourceStack(
				commandSource,
				this.worldPosition,
				this.rotation,
				this.level,
				this.permissionLevel,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				this.consumer,
				this.anchor,
				this.signingContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			);
	}

	public CommandSourceStack withEntity(Entity entity) {
		return this.entity == entity
			? this
			: new CommandSourceStack(
				this.source,
				this.worldPosition,
				this.rotation,
				this.level,
				this.permissionLevel,
				entity.getName().getString(),
				entity.getDisplayName(),
				this.server,
				entity,
				this.silent,
				this.consumer,
				this.anchor,
				this.signingContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			);
	}

	public CommandSourceStack withPosition(Vec3 vec3) {
		return this.worldPosition.equals(vec3)
			? this
			: new CommandSourceStack(
				this.source,
				vec3,
				this.rotation,
				this.level,
				this.permissionLevel,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				this.consumer,
				this.anchor,
				this.signingContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			);
	}

	public CommandSourceStack withRotation(Vec2 vec2) {
		return this.rotation.equals(vec2)
			? this
			: new CommandSourceStack(
				this.source,
				this.worldPosition,
				vec2,
				this.level,
				this.permissionLevel,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				this.consumer,
				this.anchor,
				this.signingContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			);
	}

	public CommandSourceStack withCallback(ResultConsumer<CommandSourceStack> resultConsumer) {
		return Objects.equals(this.consumer, resultConsumer)
			? this
			: new CommandSourceStack(
				this.source,
				this.worldPosition,
				this.rotation,
				this.level,
				this.permissionLevel,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				resultConsumer,
				this.anchor,
				this.signingContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			);
	}

	public CommandSourceStack withCallback(ResultConsumer<CommandSourceStack> resultConsumer, BinaryOperator<ResultConsumer<CommandSourceStack>> binaryOperator) {
		ResultConsumer<CommandSourceStack> resultConsumer2 = (ResultConsumer)binaryOperator.apply(this.consumer, resultConsumer);
		return this.withCallback(resultConsumer2);
	}

	public CommandSourceStack withSuppressedOutput() {
		return !this.silent && !this.source.alwaysAccepts()
			? new CommandSourceStack(
				this.source,
				this.worldPosition,
				this.rotation,
				this.level,
				this.permissionLevel,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				true,
				this.consumer,
				this.anchor,
				this.signingContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			)
			: this;
	}

	public CommandSourceStack withPermission(int i) {
		return i == this.permissionLevel
			? this
			: new CommandSourceStack(
				this.source,
				this.worldPosition,
				this.rotation,
				this.level,
				i,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				this.consumer,
				this.anchor,
				this.signingContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			);
	}

	public CommandSourceStack withMaximumPermission(int i) {
		return i <= this.permissionLevel
			? this
			: new CommandSourceStack(
				this.source,
				this.worldPosition,
				this.rotation,
				this.level,
				i,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				this.consumer,
				this.anchor,
				this.signingContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			);
	}

	public CommandSourceStack withAnchor(EntityAnchorArgument.Anchor anchor) {
		return anchor == this.anchor
			? this
			: new CommandSourceStack(
				this.source,
				this.worldPosition,
				this.rotation,
				this.level,
				this.permissionLevel,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				this.consumer,
				anchor,
				this.signingContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			);
	}

	public CommandSourceStack withLevel(ServerLevel serverLevel) {
		if (serverLevel == this.level) {
			return this;
		} else {
			double d = DimensionType.getTeleportationScale(this.level.dimensionType(), serverLevel.dimensionType());
			Vec3 vec3 = new Vec3(this.worldPosition.x * d, this.worldPosition.y, this.worldPosition.z * d);
			return new CommandSourceStack(
				this.source,
				vec3,
				this.rotation,
				serverLevel,
				this.permissionLevel,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				this.consumer,
				this.anchor,
				this.signingContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			);
		}
	}

	public CommandSourceStack facing(Entity entity, EntityAnchorArgument.Anchor anchor) {
		return this.facing(anchor.apply(entity));
	}

	public CommandSourceStack facing(Vec3 vec3) {
		Vec3 vec32 = this.anchor.apply(this);
		double d = vec3.x - vec32.x;
		double e = vec3.y - vec32.y;
		double f = vec3.z - vec32.z;
		double g = Math.sqrt(d * d + f * f);
		float h = Mth.wrapDegrees((float)(-(Mth.atan2(e, g) * 180.0F / (float)Math.PI)));
		float i = Mth.wrapDegrees((float)(Mth.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F);
		return this.withRotation(new Vec2(h, i));
	}

	public CommandSourceStack withSigningContext(CommandSigningContext commandSigningContext) {
		return commandSigningContext == this.signingContext
			? this
			: new CommandSourceStack(
				this.source,
				this.worldPosition,
				this.rotation,
				this.level,
				this.permissionLevel,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				this.consumer,
				this.anchor,
				commandSigningContext,
				this.chatMessageChainer,
				this.returnValueConsumer
			);
	}

	public CommandSourceStack withChatMessageChainer(TaskChainer taskChainer) {
		return taskChainer == this.chatMessageChainer
			? this
			: new CommandSourceStack(
				this.source,
				this.worldPosition,
				this.rotation,
				this.level,
				this.permissionLevel,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				this.consumer,
				this.anchor,
				this.signingContext,
				taskChainer,
				this.returnValueConsumer
			);
	}

	public CommandSourceStack withReturnValueConsumer(IntConsumer intConsumer) {
		return intConsumer == this.returnValueConsumer
			? this
			: new CommandSourceStack(
				this.source,
				this.worldPosition,
				this.rotation,
				this.level,
				this.permissionLevel,
				this.textName,
				this.displayName,
				this.server,
				this.entity,
				this.silent,
				this.consumer,
				this.anchor,
				this.signingContext,
				this.chatMessageChainer,
				intConsumer
			);
	}

	public Component getDisplayName() {
		return this.displayName;
	}

	public String getTextName() {
		return this.textName;
	}

	@Override
	public boolean hasPermission(int i) {
		return this.permissionLevel >= i;
	}

	public Vec3 getPosition() {
		return this.worldPosition;
	}

	public ServerLevel getLevel() {
		return this.level;
	}

	@Nullable
	public Entity getEntity() {
		return this.entity;
	}

	public Entity getEntityOrException() throws CommandSyntaxException {
		if (this.entity == null) {
			throw ERROR_NOT_ENTITY.create();
		} else {
			return this.entity;
		}
	}

	public ServerPlayer getPlayerOrException() throws CommandSyntaxException {
		Entity var2 = this.entity;
		if (var2 instanceof ServerPlayer) {
			return (ServerPlayer)var2;
		} else {
			throw ERROR_NOT_PLAYER.create();
		}
	}

	@Nullable
	public ServerPlayer getPlayer() {
		Entity var2 = this.entity;
		return var2 instanceof ServerPlayer serverPlayer ? serverPlayer : null;
	}

	public boolean isPlayer() {
		return this.entity instanceof ServerPlayer;
	}

	public Vec2 getRotation() {
		return this.rotation;
	}

	public MinecraftServer getServer() {
		return this.server;
	}

	public EntityAnchorArgument.Anchor getAnchor() {
		return this.anchor;
	}

	public CommandSigningContext getSigningContext() {
		return this.signingContext;
	}

	public TaskChainer getChatMessageChainer() {
		return this.chatMessageChainer;
	}

	public IntConsumer getReturnValueConsumer() {
		return this.returnValueConsumer;
	}

	public boolean shouldFilterMessageTo(ServerPlayer serverPlayer) {
		ServerPlayer serverPlayer2 = this.getPlayer();
		if (serverPlayer == serverPlayer2) {
			return false;
		} else {
			return serverPlayer2 != null && serverPlayer2.isTextFilteringEnabled() || serverPlayer.isTextFilteringEnabled();
		}
	}

	public void sendChatMessage(OutgoingChatMessage outgoingChatMessage, boolean bl, ChatType.Bound bound) {
		if (!this.silent) {
			ServerPlayer serverPlayer = this.getPlayer();
			if (serverPlayer != null) {
				serverPlayer.sendChatMessage(outgoingChatMessage, bl, bound);
			} else {
				this.source.sendSystemMessage(bound.decorate(outgoingChatMessage.content()));
			}
		}
	}

	public void sendSystemMessage(Component component) {
		if (!this.silent) {
			ServerPlayer serverPlayer = this.getPlayer();
			if (serverPlayer != null) {
				serverPlayer.sendSystemMessage(component);
			} else {
				this.source.sendSystemMessage(component);
			}
		}
	}

	public void sendSuccess(Supplier<Component> supplier, boolean bl) {
		boolean bl2 = this.source.acceptsSuccess() && !this.silent;
		boolean bl3 = bl && this.source.shouldInformAdmins() && !this.silent;
		if (bl2 || bl3) {
			Component component = (Component)supplier.get();
			if (bl2) {
				this.source.sendSystemMessage(component);
			}

			if (bl3) {
				this.broadcastToAdmins(component);
			}
		}
	}

	private void broadcastToAdmins(Component component) {
		Component component2 = Component.translatable("chat.type.admin", this.getDisplayName(), component).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
		if (this.server.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
			for(ServerPlayer serverPlayer : this.server.getPlayerList().getPlayers()) {
				if (serverPlayer != this.source && this.server.getPlayerList().isOp(serverPlayer.getGameProfile())) {
					serverPlayer.sendSystemMessage(component2);
				}
			}
		}

		if (this.source != this.server && this.server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS)) {
			this.server.sendSystemMessage(component2);
		}
	}

	public void sendFailure(Component component) {
		if (this.source.acceptsFailure() && !this.silent) {
			this.source.sendSystemMessage(Component.empty().append(component).withStyle(ChatFormatting.RED));
		}
	}

	public void onCommandComplete(CommandContext<CommandSourceStack> commandContext, boolean bl, int i) {
		if (this.consumer != null) {
			this.consumer.onCommandComplete(commandContext, bl, i);
		}
	}

	@Override
	public Collection<String> getOnlinePlayerNames() {
		return Lists.newArrayList(this.server.getPlayerNames());
	}

	@Override
	public Collection<String> getAllTeams() {
		return this.server.getScoreboard().getTeamNames();
	}

	@Override
	public Stream<ResourceLocation> getAvailableSounds() {
		return BuiltInRegistries.SOUND_EVENT.stream().map(SoundEvent::getLocation);
	}

	@Override
	public Stream<ResourceLocation> getRecipeNames() {
		return this.server.getRecipeManager().getRecipeIds();
	}

	@Override
	public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> commandContext) {
		return Suggestions.empty();
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
		}).orElseGet(Suggestions::empty);
	}

	@Override
	public Set<ResourceKey<Level>> levels() {
		return this.server.levelKeys();
	}

	@Override
	public RegistryAccess registryAccess() {
		return this.server.registryAccess();
	}

	@Override
	public FeatureFlagSet enabledFeatures() {
		return this.level.enabledFeatures();
	}
}

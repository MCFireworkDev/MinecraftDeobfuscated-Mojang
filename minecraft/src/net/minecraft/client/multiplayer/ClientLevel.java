package net.minecraft.client.multiplayer;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagManager;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.global.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.EmptyTickList;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.Scoreboard;

@Environment(EnvType.CLIENT)
public class ClientLevel extends Level {
	private final List<Entity> globalEntities = Lists.<Entity>newArrayList();
	private final Int2ObjectMap<Entity> entitiesById = new Int2ObjectOpenHashMap<>();
	private final ClientPacketListener connection;
	private final LevelRenderer levelRenderer;
	private final ClientLevel.ClientLevelData clientLevelData;
	private final DimensionSpecialEffects effects;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final List<AbstractClientPlayer> players = Lists.<AbstractClientPlayer>newArrayList();
	private Scoreboard scoreboard = new Scoreboard();
	private final Map<String, MapItemSavedData> mapData = Maps.newHashMap();
	private int skyFlashTime;
	private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = Util.make(new Object2ObjectArrayMap<>(3), object2ObjectArrayMap -> {
		object2ObjectArrayMap.put(BiomeColors.GRASS_COLOR_RESOLVER, new BlockTintCache());
		object2ObjectArrayMap.put(BiomeColors.FOLIAGE_COLOR_RESOLVER, new BlockTintCache());
		object2ObjectArrayMap.put(BiomeColors.WATER_COLOR_RESOLVER, new BlockTintCache());
	});
	private final ClientChunkCache chunkSource;

	public ClientLevel(
		ClientPacketListener clientPacketListener,
		ClientLevel.ClientLevelData clientLevelData,
		DimensionType dimensionType,
		int i,
		Supplier<ProfilerFiller> supplier,
		LevelRenderer levelRenderer,
		boolean bl,
		long l
	) {
		super(clientLevelData, dimensionType, supplier, true, bl, l);
		this.chunkSource = new ClientChunkCache(this, i);
		this.clientLevelData = clientLevelData;
		this.connection = clientPacketListener;
		this.levelRenderer = levelRenderer;
		this.effects = DimensionSpecialEffects.forType(clientPacketListener.registryAccess().dimensionTypes().getResourceKey(dimensionType));
		this.setDefaultSpawnPos(new BlockPos(8, 64, 8));
		this.updateSkyBrightness();
		this.prepareWeather();
	}

	public DimensionSpecialEffects effects() {
		return this.effects;
	}

	public void tick(BooleanSupplier booleanSupplier) {
		this.getWorldBorder().tick();
		this.tickTime();
		this.getProfiler().push("blocks");
		this.chunkSource.tick(booleanSupplier);
		this.getProfiler().pop();
	}

	private void tickTime() {
		this.setGameTime(this.levelData.getGameTime() + 1L);
		if (this.levelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
			this.setDayTime(this.levelData.getDayTime() + 1L);
		}
	}

	public void setGameTime(long l) {
		this.clientLevelData.setGameTime(l);
	}

	public void setDayTime(long l) {
		if (l < 0L) {
			l = -l;
			this.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, null);
		} else {
			this.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, null);
		}

		this.clientLevelData.setDayTime(l);
	}

	public Iterable<Entity> entitiesForRendering() {
		return Iterables.concat(this.entitiesById.values(), this.globalEntities);
	}

	public void tickEntities() {
		ProfilerFiller profilerFiller = this.getProfiler();
		profilerFiller.push("entities");
		profilerFiller.push("global");

		for(int i = 0; i < this.globalEntities.size(); ++i) {
			Entity entity = (Entity)this.globalEntities.get(i);
			this.guardEntityTick(entity -> {
				++entity.tickCount;
				entity.tick();
			}, entity);
			if (entity.removed) {
				this.globalEntities.remove(i--);
			}
		}

		profilerFiller.popPush("regular");
		ObjectIterator<Entry<Entity>> objectIterator = this.entitiesById.int2ObjectEntrySet().iterator();

		while(objectIterator.hasNext()) {
			Entry<Entity> entry = (Entry)objectIterator.next();
			Entity entity2 = (Entity)entry.getValue();
			if (!entity2.isPassenger()) {
				profilerFiller.push("tick");
				if (!entity2.removed) {
					this.guardEntityTick(this::tickNonPassenger, entity2);
				}

				profilerFiller.pop();
				profilerFiller.push("remove");
				if (entity2.removed) {
					objectIterator.remove();
					this.onEntityRemoved(entity2);
				}

				profilerFiller.pop();
			}
		}

		profilerFiller.pop();
		this.tickBlockEntities();
		profilerFiller.pop();
	}

	public void tickNonPassenger(Entity entity) {
		if (entity instanceof Player || this.getChunkSource().isEntityTickingChunk(entity)) {
			entity.setPosAndOldPos(entity.getX(), entity.getY(), entity.getZ());
			entity.yRotO = entity.yRot;
			entity.xRotO = entity.xRot;
			if (entity.inChunk || entity.isSpectator()) {
				++entity.tickCount;
				this.getProfiler().push((Supplier<String>)(() -> Registry.ENTITY_TYPE.getKey(entity.getType()).toString()));
				entity.tick();
				this.getProfiler().pop();
			}

			this.updateChunkPos(entity);
			if (entity.inChunk) {
				for(Entity entity2 : entity.getPassengers()) {
					this.tickPassenger(entity, entity2);
				}
			}
		}
	}

	public void tickPassenger(Entity entity, Entity entity2) {
		if (entity2.removed || entity2.getVehicle() != entity) {
			entity2.stopRiding();
		} else if (entity2 instanceof Player || this.getChunkSource().isEntityTickingChunk(entity2)) {
			entity2.setPosAndOldPos(entity2.getX(), entity2.getY(), entity2.getZ());
			entity2.yRotO = entity2.yRot;
			entity2.xRotO = entity2.xRot;
			if (entity2.inChunk) {
				++entity2.tickCount;
				entity2.rideTick();
			}

			this.updateChunkPos(entity2);
			if (entity2.inChunk) {
				for(Entity entity3 : entity2.getPassengers()) {
					this.tickPassenger(entity2, entity3);
				}
			}
		}
	}

	public void updateChunkPos(Entity entity) {
		this.getProfiler().push("chunkCheck");
		int i = Mth.floor(entity.getX() / 16.0);
		int j = Mth.floor(entity.getY() / 16.0);
		int k = Mth.floor(entity.getZ() / 16.0);
		if (!entity.inChunk || entity.xChunk != i || entity.yChunk != j || entity.zChunk != k) {
			if (entity.inChunk && this.hasChunk(entity.xChunk, entity.zChunk)) {
				this.getChunk(entity.xChunk, entity.zChunk).removeEntity(entity, entity.yChunk);
			}

			if (!entity.checkAndResetTeleportedFlag() && !this.hasChunk(i, k)) {
				entity.inChunk = false;
			} else {
				this.getChunk(i, k).addEntity(entity);
			}
		}

		this.getProfiler().pop();
	}

	public void unload(LevelChunk levelChunk) {
		this.blockEntitiesToUnload.addAll(levelChunk.getBlockEntities().values());
		this.chunkSource.getLightEngine().enableLightSources(levelChunk.getPos(), false);
	}

	public void onChunkLoaded(int i, int j) {
		this.tintCaches.forEach((colorResolver, blockTintCache) -> blockTintCache.invalidateForChunk(i, j));
	}

	public void clearTintCaches() {
		this.tintCaches.forEach((colorResolver, blockTintCache) -> blockTintCache.invalidateAll());
	}

	@Override
	public boolean hasChunk(int i, int j) {
		return true;
	}

	public int getEntityCount() {
		return this.entitiesById.size();
	}

	public void addLightning(LightningBolt lightningBolt) {
		this.globalEntities.add(lightningBolt);
	}

	public void addPlayer(int i, AbstractClientPlayer abstractClientPlayer) {
		this.addEntity(i, abstractClientPlayer);
		this.players.add(abstractClientPlayer);
	}

	public void putNonPlayerEntity(int i, Entity entity) {
		this.addEntity(i, entity);
	}

	private void addEntity(int i, Entity entity) {
		this.removeEntity(i);
		this.entitiesById.put(i, entity);
		this.getChunkSource().getChunk(Mth.floor(entity.getX() / 16.0), Mth.floor(entity.getZ() / 16.0), ChunkStatus.FULL, true).addEntity(entity);
	}

	public void removeEntity(int i) {
		Entity entity = this.entitiesById.remove(i);
		if (entity != null) {
			entity.remove();
			this.onEntityRemoved(entity);
		}
	}

	private void onEntityRemoved(Entity entity) {
		entity.unRide();
		if (entity.inChunk) {
			this.getChunk(entity.xChunk, entity.zChunk).removeEntity(entity);
		}

		this.players.remove(entity);
	}

	public void reAddEntitiesToChunk(LevelChunk levelChunk) {
		for(Entry<Entity> entry : this.entitiesById.int2ObjectEntrySet()) {
			Entity entity = (Entity)entry.getValue();
			int i = Mth.floor(entity.getX() / 16.0);
			int j = Mth.floor(entity.getZ() / 16.0);
			if (i == levelChunk.getPos().x && j == levelChunk.getPos().z) {
				levelChunk.addEntity(entity);
			}
		}
	}

	@Nullable
	@Override
	public Entity getEntity(int i) {
		return this.entitiesById.get(i);
	}

	public void setKnownState(BlockPos blockPos, BlockState blockState) {
		this.setBlock(blockPos, blockState, 19);
	}

	@Override
	public void disconnect() {
		this.connection.getConnection().disconnect(new TranslatableComponent("multiplayer.status.quitting"));
	}

	public void animateTick(int i, int j, int k) {
		int l = 32;
		Random random = new Random();
		boolean bl = false;
		if (this.minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
			for(ItemStack itemStack : this.minecraft.player.getHandSlots()) {
				if (itemStack.getItem() == Blocks.BARRIER.asItem()) {
					bl = true;
					break;
				}
			}
		}

		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

		for(int m = 0; m < 667; ++m) {
			this.doAnimateTick(i, j, k, 16, random, bl, mutableBlockPos);
			this.doAnimateTick(i, j, k, 32, random, bl, mutableBlockPos);
		}
	}

	public void doAnimateTick(int i, int j, int k, int l, Random random, boolean bl, BlockPos.MutableBlockPos mutableBlockPos) {
		int m = i + this.random.nextInt(l) - this.random.nextInt(l);
		int n = j + this.random.nextInt(l) - this.random.nextInt(l);
		int o = k + this.random.nextInt(l) - this.random.nextInt(l);
		mutableBlockPos.set(m, n, o);
		BlockState blockState = this.getBlockState(mutableBlockPos);
		blockState.getBlock().animateTick(blockState, this, mutableBlockPos, random);
		FluidState fluidState = this.getFluidState(mutableBlockPos);
		if (!fluidState.isEmpty()) {
			fluidState.animateTick(this, mutableBlockPos, random);
			ParticleOptions particleOptions = fluidState.getDripParticle();
			if (particleOptions != null && this.random.nextInt(10) == 0) {
				boolean bl2 = blockState.isFaceSturdy(this, mutableBlockPos, Direction.DOWN);
				BlockPos blockPos = mutableBlockPos.below();
				this.trySpawnDripParticles(blockPos, this.getBlockState(blockPos), particleOptions, bl2);
			}
		}

		if (bl && blockState.is(Blocks.BARRIER)) {
			this.addParticle(ParticleTypes.BARRIER, (double)m + 0.5, (double)n + 0.5, (double)o + 0.5, 0.0, 0.0, 0.0);
		}

		if (!blockState.isCollisionShapeFullBlock(this, mutableBlockPos)) {
			this.getBiome(mutableBlockPos)
				.getAmbientParticle()
				.ifPresent(
					ambientParticleSettings -> {
						if (ambientParticleSettings.canSpawn(this.random)) {
							this.addParticle(
								ambientParticleSettings.getOptions(),
								(double)((float)mutableBlockPos.getX() + this.random.nextFloat()),
								(double)((float)mutableBlockPos.getY() + this.random.nextFloat()),
								(double)((float)mutableBlockPos.getZ() + this.random.nextFloat()),
								0.0,
								0.0,
								0.0
							);
						}
					}
				);
		}
	}

	private void trySpawnDripParticles(BlockPos blockPos, BlockState blockState, ParticleOptions particleOptions, boolean bl) {
		if (blockState.getFluidState().isEmpty()) {
			VoxelShape voxelShape = blockState.getCollisionShape(this, blockPos);
			double d = voxelShape.max(Direction.Axis.Y);
			if (d < 1.0) {
				if (bl) {
					this.spawnFluidParticle(
						(double)blockPos.getX(),
						(double)(blockPos.getX() + 1),
						(double)blockPos.getZ(),
						(double)(blockPos.getZ() + 1),
						(double)(blockPos.getY() + 1) - 0.05,
						particleOptions
					);
				}
			} else if (!blockState.is(BlockTags.IMPERMEABLE)) {
				double e = voxelShape.min(Direction.Axis.Y);
				if (e > 0.0) {
					this.spawnParticle(blockPos, particleOptions, voxelShape, (double)blockPos.getY() + e - 0.05);
				} else {
					BlockPos blockPos2 = blockPos.below();
					BlockState blockState2 = this.getBlockState(blockPos2);
					VoxelShape voxelShape2 = blockState2.getCollisionShape(this, blockPos2);
					double f = voxelShape2.max(Direction.Axis.Y);
					if (f < 1.0 && blockState2.getFluidState().isEmpty()) {
						this.spawnParticle(blockPos, particleOptions, voxelShape, (double)blockPos.getY() - 0.05);
					}
				}
			}
		}
	}

	private void spawnParticle(BlockPos blockPos, ParticleOptions particleOptions, VoxelShape voxelShape, double d) {
		this.spawnFluidParticle(
			(double)blockPos.getX() + voxelShape.min(Direction.Axis.X),
			(double)blockPos.getX() + voxelShape.max(Direction.Axis.X),
			(double)blockPos.getZ() + voxelShape.min(Direction.Axis.Z),
			(double)blockPos.getZ() + voxelShape.max(Direction.Axis.Z),
			d,
			particleOptions
		);
	}

	private void spawnFluidParticle(double d, double e, double f, double g, double h, ParticleOptions particleOptions) {
		this.addParticle(particleOptions, Mth.lerp(this.random.nextDouble(), d, e), h, Mth.lerp(this.random.nextDouble(), f, g), 0.0, 0.0, 0.0);
	}

	public void removeAllPendingEntityRemovals() {
		ObjectIterator<Entry<Entity>> objectIterator = this.entitiesById.int2ObjectEntrySet().iterator();

		while(objectIterator.hasNext()) {
			Entry<Entity> entry = (Entry)objectIterator.next();
			Entity entity = (Entity)entry.getValue();
			if (entity.removed) {
				objectIterator.remove();
				this.onEntityRemoved(entity);
			}
		}
	}

	@Override
	public CrashReportCategory fillReportDetails(CrashReport crashReport) {
		CrashReportCategory crashReportCategory = super.fillReportDetails(crashReport);
		crashReportCategory.setDetail("Server brand", (CrashReportDetail<String>)(() -> this.minecraft.player.getServerBrand()));
		crashReportCategory.setDetail(
			"Server type",
			(CrashReportDetail<String>)(() -> this.minecraft.getSingleplayerServer() == null ? "Non-integrated multiplayer server" : "Integrated singleplayer server")
		);
		return crashReportCategory;
	}

	@Override
	public void playSound(@Nullable Player player, double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h) {
		if (player == this.minecraft.player) {
			this.playLocalSound(d, e, f, soundEvent, soundSource, g, h, false);
		}
	}

	@Override
	public void playSound(@Nullable Player player, Entity entity, SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
		if (player == this.minecraft.player) {
			this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(soundEvent, soundSource, entity));
		}
	}

	public void playLocalSound(BlockPos blockPos, SoundEvent soundEvent, SoundSource soundSource, float f, float g, boolean bl) {
		this.playLocalSound((double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5, soundEvent, soundSource, f, g, bl);
	}

	@Override
	public void playLocalSound(double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h, boolean bl) {
		double i = this.minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(d, e, f);
		SimpleSoundInstance simpleSoundInstance = new SimpleSoundInstance(soundEvent, soundSource, g, h, (float)d, (float)e, (float)f);
		if (bl && i > 100.0) {
			double j = Math.sqrt(i) / 40.0;
			this.minecraft.getSoundManager().playDelayed(simpleSoundInstance, (int)(j * 20.0));
		} else {
			this.minecraft.getSoundManager().play(simpleSoundInstance);
		}
	}

	@Override
	public void createFireworks(double d, double e, double f, double g, double h, double i, @Nullable CompoundTag compoundTag) {
		this.minecraft.particleEngine.add(new FireworkParticles.Starter(this, d, e, f, g, h, i, this.minecraft.particleEngine, compoundTag));
	}

	@Override
	public void sendPacketToServer(Packet<?> packet) {
		this.connection.send(packet);
	}

	@Override
	public RecipeManager getRecipeManager() {
		return this.connection.getRecipeManager();
	}

	public void setScoreboard(Scoreboard scoreboard) {
		this.scoreboard = scoreboard;
	}

	@Override
	public TickList<Block> getBlockTicks() {
		return EmptyTickList.empty();
	}

	@Override
	public TickList<Fluid> getLiquidTicks() {
		return EmptyTickList.empty();
	}

	public ClientChunkCache getChunkSource() {
		return this.chunkSource;
	}

	@Nullable
	@Override
	public MapItemSavedData getMapData(String string) {
		return (MapItemSavedData)this.mapData.get(string);
	}

	@Override
	public void setMapData(MapItemSavedData mapItemSavedData) {
		this.mapData.put(mapItemSavedData.getId(), mapItemSavedData);
	}

	@Override
	public int getFreeMapId() {
		return 0;
	}

	@Override
	public Scoreboard getScoreboard() {
		return this.scoreboard;
	}

	@Override
	public TagManager getTagManager() {
		return this.connection.getTags();
	}

	@Override
	public RegistryAccess registryAccess() {
		return this.connection.registryAccess();
	}

	@Override
	public void sendBlockUpdated(BlockPos blockPos, BlockState blockState, BlockState blockState2, int i) {
		this.levelRenderer.blockChanged(this, blockPos, blockState, blockState2, i);
	}

	@Override
	public void setBlocksDirty(BlockPos blockPos, BlockState blockState, BlockState blockState2) {
		this.levelRenderer.setBlockDirty(blockPos, blockState, blockState2);
	}

	public void setSectionDirtyWithNeighbors(int i, int j, int k) {
		this.levelRenderer.setSectionDirtyWithNeighbors(i, j, k);
	}

	@Override
	public void destroyBlockProgress(int i, BlockPos blockPos, int j) {
		this.levelRenderer.destroyBlockProgress(i, blockPos, j);
	}

	@Override
	public void globalLevelEvent(int i, BlockPos blockPos, int j) {
		this.levelRenderer.globalLevelEvent(i, blockPos, j);
	}

	@Override
	public void levelEvent(@Nullable Player player, int i, BlockPos blockPos, int j) {
		try {
			this.levelRenderer.levelEvent(player, i, blockPos, j);
		} catch (Throwable var8) {
			CrashReport crashReport = CrashReport.forThrowable(var8, "Playing level event");
			CrashReportCategory crashReportCategory = crashReport.addCategory("Level event being played");
			crashReportCategory.setDetail("Block coordinates", CrashReportCategory.formatLocation(blockPos));
			crashReportCategory.setDetail("Event source", player);
			crashReportCategory.setDetail("Event type", i);
			crashReportCategory.setDetail("Event data", j);
			throw new ReportedException(crashReport);
		}
	}

	@Override
	public void addParticle(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i) {
		this.levelRenderer.addParticle(particleOptions, particleOptions.getType().getOverrideLimiter(), d, e, f, g, h, i);
	}

	@Override
	public void addParticle(ParticleOptions particleOptions, boolean bl, double d, double e, double f, double g, double h, double i) {
		this.levelRenderer.addParticle(particleOptions, particleOptions.getType().getOverrideLimiter() || bl, d, e, f, g, h, i);
	}

	@Override
	public void addAlwaysVisibleParticle(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i) {
		this.levelRenderer.addParticle(particleOptions, false, true, d, e, f, g, h, i);
	}

	@Override
	public void addAlwaysVisibleParticle(ParticleOptions particleOptions, boolean bl, double d, double e, double f, double g, double h, double i) {
		this.levelRenderer.addParticle(particleOptions, particleOptions.getType().getOverrideLimiter() || bl, true, d, e, f, g, h, i);
	}

	@Override
	public List<AbstractClientPlayer> players() {
		return this.players;
	}

	@Override
	public Biome getUncachedNoiseBiome(int i, int j, int k) {
		return Biomes.PLAINS;
	}

	public float getSkyDarken(float f) {
		float g = this.getTimeOfDay(f);
		float h = 1.0F - (Mth.cos(g * (float) (Math.PI * 2)) * 2.0F + 0.2F);
		h = Mth.clamp(h, 0.0F, 1.0F);
		h = 1.0F - h;
		h = (float)((double)h * (1.0 - (double)(this.getRainLevel(f) * 5.0F) / 16.0));
		h = (float)((double)h * (1.0 - (double)(this.getThunderLevel(f) * 5.0F) / 16.0));
		return h * 0.8F + 0.2F;
	}

	public Vec3 getSkyColor(BlockPos blockPos, float f) {
		float g = this.getTimeOfDay(f);
		float h = Mth.cos(g * (float) (Math.PI * 2)) * 2.0F + 0.5F;
		h = Mth.clamp(h, 0.0F, 1.0F);
		Biome biome = this.getBiome(blockPos);
		int i = biome.getSkyColor();
		float j = (float)(i >> 16 & 0xFF) / 255.0F;
		float k = (float)(i >> 8 & 0xFF) / 255.0F;
		float l = (float)(i & 0xFF) / 255.0F;
		j *= h;
		k *= h;
		l *= h;
		float m = this.getRainLevel(f);
		if (m > 0.0F) {
			float n = (j * 0.3F + k * 0.59F + l * 0.11F) * 0.6F;
			float o = 1.0F - m * 0.75F;
			j = j * o + n * (1.0F - o);
			k = k * o + n * (1.0F - o);
			l = l * o + n * (1.0F - o);
		}

		float n = this.getThunderLevel(f);
		if (n > 0.0F) {
			float o = (j * 0.3F + k * 0.59F + l * 0.11F) * 0.2F;
			float p = 1.0F - n * 0.75F;
			j = j * p + o * (1.0F - p);
			k = k * p + o * (1.0F - p);
			l = l * p + o * (1.0F - p);
		}

		if (this.skyFlashTime > 0) {
			float o = (float)this.skyFlashTime - f;
			if (o > 1.0F) {
				o = 1.0F;
			}

			o *= 0.45F;
			j = j * (1.0F - o) + 0.8F * o;
			k = k * (1.0F - o) + 0.8F * o;
			l = l * (1.0F - o) + 1.0F * o;
		}

		return new Vec3((double)j, (double)k, (double)l);
	}

	public Vec3 getCloudColor(float f) {
		float g = this.getTimeOfDay(f);
		float h = Mth.cos(g * (float) (Math.PI * 2)) * 2.0F + 0.5F;
		h = Mth.clamp(h, 0.0F, 1.0F);
		float i = 1.0F;
		float j = 1.0F;
		float k = 1.0F;
		float l = this.getRainLevel(f);
		if (l > 0.0F) {
			float m = (i * 0.3F + j * 0.59F + k * 0.11F) * 0.6F;
			float n = 1.0F - l * 0.95F;
			i = i * n + m * (1.0F - n);
			j = j * n + m * (1.0F - n);
			k = k * n + m * (1.0F - n);
		}

		i *= h * 0.9F + 0.1F;
		j *= h * 0.9F + 0.1F;
		k *= h * 0.85F + 0.15F;
		float m = this.getThunderLevel(f);
		if (m > 0.0F) {
			float n = (i * 0.3F + j * 0.59F + k * 0.11F) * 0.2F;
			float o = 1.0F - m * 0.95F;
			i = i * o + n * (1.0F - o);
			j = j * o + n * (1.0F - o);
			k = k * o + n * (1.0F - o);
		}

		return new Vec3((double)i, (double)j, (double)k);
	}

	public float getStarBrightness(float f) {
		float g = this.getTimeOfDay(f);
		float h = 1.0F - (Mth.cos(g * (float) (Math.PI * 2)) * 2.0F + 0.25F);
		h = Mth.clamp(h, 0.0F, 1.0F);
		return h * h * 0.5F;
	}

	public int getSkyFlashTime() {
		return this.skyFlashTime;
	}

	@Override
	public void setSkyFlashTime(int i) {
		this.skyFlashTime = i;
	}

	@Override
	public float getShade(Direction direction, boolean bl) {
		boolean bl2 = this.dimensionType().isNether();
		if (!bl) {
			return bl2 ? 0.9F : 1.0F;
		} else {
			switch(direction) {
				case DOWN:
					return bl2 ? 0.9F : 0.5F;
				case UP:
					return bl2 ? 0.9F : 1.0F;
				case NORTH:
				case SOUTH:
					return 0.8F;
				case WEST:
				case EAST:
					return 0.6F;
				default:
					return 1.0F;
			}
		}
	}

	@Override
	public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
		BlockTintCache blockTintCache = this.tintCaches.get(colorResolver);
		return blockTintCache.getColor(blockPos, () -> this.calculateBlockTint(blockPos, colorResolver));
	}

	public int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
		int i = Minecraft.getInstance().options.biomeBlendRadius;
		if (i == 0) {
			return colorResolver.getColor(this.getBiome(blockPos), (double)blockPos.getX(), (double)blockPos.getZ());
		} else {
			int j = (i * 2 + 1) * (i * 2 + 1);
			int k = 0;
			int l = 0;
			int m = 0;
			Cursor3D cursor3D = new Cursor3D(blockPos.getX() - i, blockPos.getY(), blockPos.getZ() - i, blockPos.getX() + i, blockPos.getY(), blockPos.getZ() + i);

			int n;
			for(BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(); cursor3D.advance(); m += n & 0xFF) {
				mutableBlockPos.set(cursor3D.nextX(), cursor3D.nextY(), cursor3D.nextZ());
				n = colorResolver.getColor(this.getBiome(mutableBlockPos), (double)mutableBlockPos.getX(), (double)mutableBlockPos.getZ());
				k += (n & 0xFF0000) >> 16;
				l += (n & 0xFF00) >> 8;
			}

			return (k / j & 0xFF) << 16 | (l / j & 0xFF) << 8 | m / j & 0xFF;
		}
	}

	public BlockPos getSharedSpawnPos() {
		BlockPos blockPos = new BlockPos(this.levelData.getXSpawn(), this.levelData.getYSpawn(), this.levelData.getZSpawn());
		if (!this.getWorldBorder().isWithinBounds(blockPos)) {
			blockPos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(this.getWorldBorder().getCenterX(), 0.0, this.getWorldBorder().getCenterZ()));
		}

		return blockPos;
	}

	public void setDefaultSpawnPos(BlockPos blockPos) {
		this.levelData.setSpawn(blockPos);
	}

	public String toString() {
		return "ClientLevel";
	}

	public ClientLevel.ClientLevelData getLevelData() {
		return this.clientLevelData;
	}

	@Environment(EnvType.CLIENT)
	public static class ClientLevelData implements WritableLevelData {
		private final boolean hardcore;
		private final GameRules gameRules;
		private final boolean isFlat;
		private int xSpawn;
		private int ySpawn;
		private int zSpawn;
		private long gameTime;
		private long dayTime;
		private boolean raining;
		private Difficulty difficulty;
		private boolean difficultyLocked;

		public ClientLevelData(Difficulty difficulty, boolean bl, boolean bl2) {
			this.difficulty = difficulty;
			this.hardcore = bl;
			this.isFlat = bl2;
			this.gameRules = new GameRules();
		}

		@Override
		public int getXSpawn() {
			return this.xSpawn;
		}

		@Override
		public int getYSpawn() {
			return this.ySpawn;
		}

		@Override
		public int getZSpawn() {
			return this.zSpawn;
		}

		@Override
		public long getGameTime() {
			return this.gameTime;
		}

		@Override
		public long getDayTime() {
			return this.dayTime;
		}

		@Override
		public void setXSpawn(int i) {
			this.xSpawn = i;
		}

		@Override
		public void setYSpawn(int i) {
			this.ySpawn = i;
		}

		@Override
		public void setZSpawn(int i) {
			this.zSpawn = i;
		}

		public void setGameTime(long l) {
			this.gameTime = l;
		}

		public void setDayTime(long l) {
			this.dayTime = l;
		}

		@Override
		public void setSpawn(BlockPos blockPos) {
			this.xSpawn = blockPos.getX();
			this.ySpawn = blockPos.getY();
			this.zSpawn = blockPos.getZ();
		}

		@Override
		public boolean isThundering() {
			return false;
		}

		@Override
		public boolean isRaining() {
			return this.raining;
		}

		@Override
		public void setRaining(boolean bl) {
			this.raining = bl;
		}

		@Override
		public boolean isHardcore() {
			return this.hardcore;
		}

		@Override
		public GameRules getGameRules() {
			return this.gameRules;
		}

		@Override
		public Difficulty getDifficulty() {
			return this.difficulty;
		}

		@Override
		public boolean isDifficultyLocked() {
			return this.difficultyLocked;
		}

		@Override
		public void fillCrashReportCategory(CrashReportCategory crashReportCategory) {
			WritableLevelData.super.fillCrashReportCategory(crashReportCategory);
		}

		public void setDifficulty(Difficulty difficulty) {
			this.difficulty = difficulty;
		}

		public void setDifficultyLocked(boolean bl) {
			this.difficultyLocked = bl;
		}

		public double getHorizonHeight() {
			return this.isFlat ? 0.0 : 63.0;
		}

		public double getClearColorScale() {
			return this.isFlat ? 1.0 : 0.03125;
		}
	}
}

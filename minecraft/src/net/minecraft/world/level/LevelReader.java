package net.minecraft.world.level;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public interface LevelReader extends BlockAndTintGetter, CollisionGetter, BiomeManager.NoiseBiomeSource {
	@Nullable
	ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl);

	@Deprecated
	boolean hasChunk(int i, int j);

	int getHeight(Heightmap.Types types, int i, int j);

	int getSkyDarken();

	BiomeManager getBiomeManager();

	default Biome getBiome(BlockPos blockPos) {
		return this.getBiomeManager().getBiome(blockPos);
	}

	default Stream<BlockState> getBlockStatesIfLoaded(AABB aABB) {
		int i = Mth.floor(aABB.minX);
		int j = Mth.floor(aABB.maxX);
		int k = Mth.floor(aABB.minY);
		int l = Mth.floor(aABB.maxY);
		int m = Mth.floor(aABB.minZ);
		int n = Mth.floor(aABB.maxZ);
		return this.hasChunksAt(i, k, m, j, l, n) ? this.getBlockStates(aABB) : Stream.empty();
	}

	@Environment(EnvType.CLIENT)
	@Override
	default int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
		return colorResolver.getColor(this.getBiome(blockPos), (double)blockPos.getX(), (double)blockPos.getZ());
	}

	@Override
	default Biome getNoiseBiome(int i, int j, int k) {
		ChunkAccess chunkAccess = this.getChunk(QuartPos.toSection(i), QuartPos.toSection(k), ChunkStatus.BIOMES, false);
		return chunkAccess != null && chunkAccess.getBiomes() != null ? chunkAccess.getBiomes().getNoiseBiome(i, j, k) : this.getUncachedNoiseBiome(i, j, k);
	}

	Biome getUncachedNoiseBiome(int i, int j, int k);

	boolean isClientSide();

	@Deprecated
	int getSeaLevel();

	DimensionType dimensionType();

	@Override
	default int getMinBuildHeight() {
		return this.dimensionType().minY();
	}

	@Override
	default int getHeight() {
		return this.dimensionType().height();
	}

	default BlockPos getHeightmapPos(Heightmap.Types types, BlockPos blockPos) {
		return new BlockPos(blockPos.getX(), this.getHeight(types, blockPos.getX(), blockPos.getZ()), blockPos.getZ());
	}

	default boolean isEmptyBlock(BlockPos blockPos) {
		return this.getBlockState(blockPos).isAir();
	}

	default boolean canSeeSkyFromBelowWater(BlockPos blockPos) {
		if (blockPos.getY() >= this.getSeaLevel()) {
			return this.canSeeSky(blockPos);
		} else {
			BlockPos blockPos2 = new BlockPos(blockPos.getX(), this.getSeaLevel(), blockPos.getZ());
			if (!this.canSeeSky(blockPos2)) {
				return false;
			} else {
				for(BlockPos var4 = blockPos2.below(); var4.getY() > blockPos.getY(); var4 = var4.below()) {
					BlockState blockState = this.getBlockState(var4);
					if (blockState.getLightBlock(this, var4) > 0 && !blockState.getMaterial().isLiquid()) {
						return false;
					}
				}

				return true;
			}
		}
	}

	@Deprecated
	default float getBrightness(BlockPos blockPos) {
		return this.dimensionType().brightness(this.getMaxLocalRawBrightness(blockPos));
	}

	default int getDirectSignal(BlockPos blockPos, Direction direction) {
		return this.getBlockState(blockPos).getDirectSignal(this, blockPos, direction);
	}

	default ChunkAccess getChunk(BlockPos blockPos) {
		return this.getChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()));
	}

	default ChunkAccess getChunk(int i, int j) {
		return this.getChunk(i, j, ChunkStatus.FULL, true);
	}

	default ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus) {
		return this.getChunk(i, j, chunkStatus, true);
	}

	@Nullable
	@Override
	default BlockGetter getChunkForCollisions(int i, int j) {
		return this.getChunk(i, j, ChunkStatus.EMPTY, false);
	}

	default boolean isWaterAt(BlockPos blockPos) {
		return this.getFluidState(blockPos).is(FluidTags.WATER);
	}

	default boolean containsAnyLiquid(AABB aABB) {
		int i = Mth.floor(aABB.minX);
		int j = Mth.ceil(aABB.maxX);
		int k = Mth.floor(aABB.minY);
		int l = Mth.ceil(aABB.maxY);
		int m = Mth.floor(aABB.minZ);
		int n = Mth.ceil(aABB.maxZ);
		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

		for(int o = i; o < j; ++o) {
			for(int p = k; p < l; ++p) {
				for(int q = m; q < n; ++q) {
					BlockState blockState = this.getBlockState(mutableBlockPos.set(o, p, q));
					if (!blockState.getFluidState().isEmpty()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	default int getMaxLocalRawBrightness(BlockPos blockPos) {
		return this.getMaxLocalRawBrightness(blockPos, this.getSkyDarken());
	}

	default int getMaxLocalRawBrightness(BlockPos blockPos, int i) {
		return blockPos.getX() >= -30000000 && blockPos.getZ() >= -30000000 && blockPos.getX() < 30000000 && blockPos.getZ() < 30000000
			? this.getRawBrightness(blockPos, i)
			: 15;
	}

	@Deprecated
	default boolean hasChunkAt(int i, int j) {
		return this.hasChunk(SectionPos.blockToSectionCoord(i), SectionPos.blockToSectionCoord(j));
	}

	@Deprecated
	default boolean hasChunkAt(BlockPos blockPos) {
		return this.hasChunkAt(blockPos.getX(), blockPos.getZ());
	}

	@Deprecated
	default boolean hasChunksAt(BlockPos blockPos, BlockPos blockPos2) {
		return this.hasChunksAt(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos2.getX(), blockPos2.getY(), blockPos2.getZ());
	}

	@Deprecated
	default boolean hasChunksAt(int i, int j, int k, int l, int m, int n) {
		return m >= this.getMinBuildHeight() && j < this.getMaxBuildHeight() ? this.hasChunksAt(i, k, l, n) : false;
	}

	@Deprecated
	default boolean hasChunksAt(int i, int j, int k, int l) {
		int m = SectionPos.blockToSectionCoord(i);
		int n = SectionPos.blockToSectionCoord(k);
		int o = SectionPos.blockToSectionCoord(j);
		int p = SectionPos.blockToSectionCoord(l);

		for(int q = m; q <= n; ++q) {
			for(int r = o; r <= p; ++r) {
				if (!this.hasChunk(q, r)) {
					return false;
				}
			}
		}

		return true;
	}
}

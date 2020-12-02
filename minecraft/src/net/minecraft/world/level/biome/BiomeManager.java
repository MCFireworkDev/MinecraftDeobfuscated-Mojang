package net.minecraft.world.level.biome;

import com.google.common.hash.Hashing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;

public class BiomeManager {
	private static final int CHUNK_CENTER_QUART = QuartPos.fromBlock(8);
	private final BiomeManager.NoiseBiomeSource noiseBiomeSource;
	private final long biomeZoomSeed;
	private final BiomeZoomer zoomer;

	public BiomeManager(BiomeManager.NoiseBiomeSource noiseBiomeSource, long l, BiomeZoomer biomeZoomer) {
		this.noiseBiomeSource = noiseBiomeSource;
		this.biomeZoomSeed = l;
		this.zoomer = biomeZoomer;
	}

	public static long obfuscateSeed(long l) {
		return Hashing.sha256().hashLong(l).asLong();
	}

	public BiomeManager withDifferentSource(BiomeSource biomeSource) {
		return new BiomeManager(biomeSource, this.biomeZoomSeed, this.zoomer);
	}

	public Biome getBiome(BlockPos blockPos) {
		return this.zoomer.getBiome(this.biomeZoomSeed, blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.noiseBiomeSource);
	}

	@Environment(EnvType.CLIENT)
	public Biome getNoiseBiomeAtPosition(double d, double e, double f) {
		int i = QuartPos.fromBlock(Mth.floor(d));
		int j = QuartPos.fromBlock(Mth.floor(e));
		int k = QuartPos.fromBlock(Mth.floor(f));
		return this.getNoiseBiomeAtQuart(i, j, k);
	}

	@Environment(EnvType.CLIENT)
	public Biome getNoiseBiomeAtPosition(BlockPos blockPos) {
		int i = QuartPos.fromBlock(blockPos.getX());
		int j = QuartPos.fromBlock(blockPos.getY());
		int k = QuartPos.fromBlock(blockPos.getZ());
		return this.getNoiseBiomeAtQuart(i, j, k);
	}

	@Environment(EnvType.CLIENT)
	public Biome getNoiseBiomeAtQuart(int i, int j, int k) {
		return this.noiseBiomeSource.getNoiseBiome(i, j, k);
	}

	public Biome getPrimaryBiomeAtChunk(int i, int j) {
		return this.noiseBiomeSource.getPrimaryBiome(i, j);
	}

	public interface NoiseBiomeSource {
		Biome getNoiseBiome(int i, int j, int k);

		default Biome getPrimaryBiome(int i, int j) {
			return this.getNoiseBiome(QuartPos.fromSection(i) + BiomeManager.CHUNK_CENTER_QUART, 0, QuartPos.fromSection(j) + BiomeManager.CHUNK_CENTER_QUART);
		}
	}
}

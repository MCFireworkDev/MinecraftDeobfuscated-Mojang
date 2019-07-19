package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ChunkTickList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ProtoTickList;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureFeatureIO;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkSerializer {
	private static final Logger LOGGER = LogManager.getLogger();

	public static ProtoChunk read(ServerLevel serverLevel, StructureManager structureManager, PoiManager poiManager, ChunkPos chunkPos, CompoundTag compoundTag) {
		ChunkGenerator<?> chunkGenerator = serverLevel.getChunkSource().getGenerator();
		BiomeSource biomeSource = chunkGenerator.getBiomeSource();
		CompoundTag compoundTag2 = compoundTag.getCompound("Level");
		ChunkPos chunkPos2 = new ChunkPos(compoundTag2.getInt("xPos"), compoundTag2.getInt("zPos"));
		if (!Objects.equals(chunkPos, chunkPos2)) {
			LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", chunkPos, chunkPos, chunkPos2);
		}

		Biome[] biomes = new Biome[256];
		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
		if (compoundTag2.contains("Biomes", 11)) {
			int[] is = compoundTag2.getIntArray("Biomes");

			for(int i = 0; i < is.length; ++i) {
				biomes[i] = Registry.BIOME.byId(is[i]);
				if (biomes[i] == null) {
					biomes[i] = biomeSource.getBiome(mutableBlockPos.set((i & 15) + chunkPos.getMinBlockX(), 0, (i >> 4 & 15) + chunkPos.getMinBlockZ()));
				}
			}
		} else {
			for(int j = 0; j < biomes.length; ++j) {
				biomes[j] = biomeSource.getBiome(mutableBlockPos.set((j & 15) + chunkPos.getMinBlockX(), 0, (j >> 4 & 15) + chunkPos.getMinBlockZ()));
			}
		}

		UpgradeData upgradeData = compoundTag2.contains("UpgradeData", 10) ? new UpgradeData(compoundTag2.getCompound("UpgradeData")) : UpgradeData.EMPTY;
		ProtoTickList<Block> protoTickList = new ProtoTickList<>(
			block -> block == null || block.defaultBlockState().isAir(), chunkPos, compoundTag2.getList("ToBeTicked", 9)
		);
		ProtoTickList<Fluid> protoTickList2 = new ProtoTickList<>(
			fluid -> fluid == null || fluid == Fluids.EMPTY, chunkPos, compoundTag2.getList("LiquidsToBeTicked", 9)
		);
		boolean bl = compoundTag2.getBoolean("isLightOn");
		ListTag listTag = compoundTag2.getList("Sections", 10);
		int k = 16;
		LevelChunkSection[] levelChunkSections = new LevelChunkSection[16];
		boolean bl2 = serverLevel.getDimension().isHasSkyLight();
		ChunkSource chunkSource = serverLevel.getChunkSource();
		LevelLightEngine levelLightEngine = chunkSource.getLightEngine();
		if (bl) {
			levelLightEngine.retainData(chunkPos, true);
		}

		for(int l = 0; l < listTag.size(); ++l) {
			CompoundTag compoundTag3 = listTag.getCompound(l);
			int m = compoundTag3.getByte("Y");
			if (compoundTag3.contains("Palette", 9) && compoundTag3.contains("BlockStates", 12)) {
				LevelChunkSection levelChunkSection = new LevelChunkSection(m << 4);
				levelChunkSection.getStates().read(compoundTag3.getList("Palette", 10), compoundTag3.getLongArray("BlockStates"));
				levelChunkSection.recalcBlockCounts();
				if (!levelChunkSection.isEmpty()) {
					levelChunkSections[m] = levelChunkSection;
				}

				poiManager.checkConsistencyWithBlocks(chunkPos, levelChunkSection);
			}

			if (bl) {
				if (compoundTag3.contains("BlockLight", 7)) {
					levelLightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, m), new DataLayer(compoundTag3.getByteArray("BlockLight")));
				}

				if (bl2 && compoundTag3.contains("SkyLight", 7)) {
					levelLightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, m), new DataLayer(compoundTag3.getByteArray("SkyLight")));
				}
			}
		}

		long n = compoundTag2.getLong("InhabitedTime");
		ChunkStatus.ChunkType chunkType = getChunkTypeFromTag(compoundTag);
		ChunkAccess chunkAccess;
		if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
			TickList<Block> tickList;
			if (compoundTag2.contains("TileTicks", 9)) {
				tickList = ChunkTickList.create(compoundTag2.getList("TileTicks", 10), Registry.BLOCK::getKey, Registry.BLOCK::get);
			} else {
				tickList = protoTickList;
			}

			TickList<Fluid> tickList2;
			if (compoundTag2.contains("LiquidTicks", 9)) {
				tickList2 = ChunkTickList.create(compoundTag2.getList("LiquidTicks", 10), Registry.FLUID::getKey, Registry.FLUID::get);
			} else {
				tickList2 = protoTickList2;
			}

			chunkAccess = new LevelChunk(
				serverLevel.getLevel(), chunkPos, biomes, upgradeData, tickList, tickList2, n, levelChunkSections, levelChunk -> postLoadChunk(compoundTag2, levelChunk)
			);
		} else {
			ProtoChunk protoChunk = new ProtoChunk(chunkPos, upgradeData, levelChunkSections, protoTickList, protoTickList2);
			chunkAccess = protoChunk;
			protoChunk.setBiomes(biomes);
			protoChunk.setInhabitedTime(n);
			protoChunk.setStatus(ChunkStatus.byName(compoundTag2.getString("Status")));
			if (protoChunk.getStatus().isOrAfter(ChunkStatus.FEATURES)) {
				protoChunk.setLightEngine(levelLightEngine);
			}

			if (!bl && protoChunk.getStatus().isOrAfter(ChunkStatus.LIGHT)) {
				for(BlockPos blockPos : BlockPos.betweenClosed(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(), 255, chunkPos.getMaxBlockZ())) {
					if (chunkAccess.getBlockState(blockPos).getLightEmission() != 0) {
						protoChunk.addLight(blockPos);
					}
				}
			}
		}

		chunkAccess.setLightCorrect(bl);
		CompoundTag compoundTag4 = compoundTag2.getCompound("Heightmaps");
		EnumSet<Heightmap.Types> enumSet = EnumSet.noneOf(Heightmap.Types.class);

		for(Heightmap.Types types : chunkAccess.getStatus().heightmapsAfter()) {
			String string = types.getSerializationKey();
			if (compoundTag4.contains(string, 12)) {
				chunkAccess.setHeightmap(types, compoundTag4.getLongArray(string));
			} else {
				enumSet.add(types);
			}
		}

		Heightmap.primeHeightmaps(chunkAccess, enumSet);
		CompoundTag compoundTag5 = compoundTag2.getCompound("Structures");
		chunkAccess.setAllStarts(unpackStructureStart(chunkGenerator, structureManager, biomeSource, compoundTag5));
		chunkAccess.setAllReferences(unpackStructureReferences(compoundTag5));
		if (compoundTag2.getBoolean("shouldSave")) {
			chunkAccess.setUnsaved(true);
		}

		ListTag listTag2 = compoundTag2.getList("PostProcessing", 9);

		for(int o = 0; o < listTag2.size(); ++o) {
			ListTag listTag3 = listTag2.getList(o);

			for(int p = 0; p < listTag3.size(); ++p) {
				chunkAccess.addPackedPostProcess(listTag3.getShort(p), o);
			}
		}

		if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
			return new ImposterProtoChunk((LevelChunk)chunkAccess);
		} else {
			ProtoChunk protoChunk2 = (ProtoChunk)chunkAccess;
			ListTag listTag3 = compoundTag2.getList("Entities", 10);

			for(int p = 0; p < listTag3.size(); ++p) {
				protoChunk2.addEntity(listTag3.getCompound(p));
			}

			ListTag listTag4 = compoundTag2.getList("TileEntities", 10);

			for(int q = 0; q < listTag4.size(); ++q) {
				CompoundTag compoundTag6 = listTag4.getCompound(q);
				chunkAccess.setBlockEntityNbt(compoundTag6);
			}

			ListTag listTag5 = compoundTag2.getList("Lights", 9);

			for(int r = 0; r < listTag5.size(); ++r) {
				ListTag listTag6 = listTag5.getList(r);

				for(int s = 0; s < listTag6.size(); ++s) {
					protoChunk2.addLight(listTag6.getShort(s), r);
				}
			}

			CompoundTag compoundTag6 = compoundTag2.getCompound("CarvingMasks");

			for(String string2 : compoundTag6.getAllKeys()) {
				GenerationStep.Carving carving = GenerationStep.Carving.valueOf(string2);
				protoChunk2.setCarvingMask(carving, BitSet.valueOf(compoundTag6.getByteArray(string2)));
			}

			return protoChunk2;
		}
	}

	public static CompoundTag write(ServerLevel serverLevel, ChunkAccess chunkAccess) {
		ChunkPos chunkPos = chunkAccess.getPos();
		CompoundTag compoundTag = new CompoundTag();
		CompoundTag compoundTag2 = new CompoundTag();
		compoundTag.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
		compoundTag.put("Level", compoundTag2);
		compoundTag2.putInt("xPos", chunkPos.x);
		compoundTag2.putInt("zPos", chunkPos.z);
		compoundTag2.putLong("LastUpdate", serverLevel.getGameTime());
		compoundTag2.putLong("InhabitedTime", chunkAccess.getInhabitedTime());
		compoundTag2.putString("Status", chunkAccess.getStatus().getName());
		UpgradeData upgradeData = chunkAccess.getUpgradeData();
		if (!upgradeData.isEmpty()) {
			compoundTag2.put("UpgradeData", upgradeData.write());
		}

		LevelChunkSection[] levelChunkSections = chunkAccess.getSections();
		ListTag listTag = new ListTag();
		LevelLightEngine levelLightEngine = serverLevel.getChunkSource().getLightEngine();
		boolean bl = chunkAccess.isLightCorrect();

		for(int i = -1; i < 17; ++i) {
			int j = i;
			LevelChunkSection levelChunkSection = (LevelChunkSection)Arrays.stream(levelChunkSections)
				.filter(levelChunkSection -> levelChunkSection != null && levelChunkSection.bottomBlockY() >> 4 == j)
				.findFirst()
				.orElse(LevelChunk.EMPTY_SECTION);
			DataLayer dataLayer = levelLightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkPos, j));
			DataLayer dataLayer2 = levelLightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkPos, j));
			if (levelChunkSection != LevelChunk.EMPTY_SECTION || dataLayer != null || dataLayer2 != null) {
				CompoundTag compoundTag3 = new CompoundTag();
				compoundTag3.putByte("Y", (byte)(j & 0xFF));
				if (levelChunkSection != LevelChunk.EMPTY_SECTION) {
					levelChunkSection.getStates().write(compoundTag3, "Palette", "BlockStates");
				}

				if (dataLayer != null && !dataLayer.isEmpty()) {
					compoundTag3.putByteArray("BlockLight", dataLayer.getData());
				}

				if (dataLayer2 != null && !dataLayer2.isEmpty()) {
					compoundTag3.putByteArray("SkyLight", dataLayer2.getData());
				}

				listTag.add(compoundTag3);
			}
		}

		compoundTag2.put("Sections", listTag);
		if (bl) {
			compoundTag2.putBoolean("isLightOn", true);
		}

		Biome[] biomes = chunkAccess.getBiomes();
		int[] is = biomes != null ? new int[biomes.length] : new int[0];
		if (biomes != null) {
			for(int k = 0; k < biomes.length; ++k) {
				is[k] = Registry.BIOME.getId(biomes[k]);
			}
		}

		compoundTag2.putIntArray("Biomes", is);
		ListTag listTag2 = new ListTag();

		for(BlockPos blockPos : chunkAccess.getBlockEntitiesPos()) {
			CompoundTag compoundTag3 = chunkAccess.getBlockEntityNbtForSaving(blockPos);
			if (compoundTag3 != null) {
				listTag2.add(compoundTag3);
			}
		}

		compoundTag2.put("TileEntities", listTag2);
		ListTag listTag3 = new ListTag();
		if (chunkAccess.getStatus().getChunkType() == ChunkStatus.ChunkType.LEVELCHUNK) {
			LevelChunk levelChunk = (LevelChunk)chunkAccess;
			levelChunk.setLastSaveHadEntities(false);

			for(int l = 0; l < levelChunk.getEntitySections().length; ++l) {
				for(Entity entity : levelChunk.getEntitySections()[l]) {
					CompoundTag compoundTag4 = new CompoundTag();
					if (entity.save(compoundTag4)) {
						levelChunk.setLastSaveHadEntities(true);
						listTag3.add(compoundTag4);
					}
				}
			}
		} else {
			ProtoChunk protoChunk = (ProtoChunk)chunkAccess;
			listTag3.addAll(protoChunk.getEntities());
			compoundTag2.put("Lights", packOffsets(protoChunk.getPackedLights()));
			CompoundTag compoundTag3 = new CompoundTag();

			for(GenerationStep.Carving carving : GenerationStep.Carving.values()) {
				compoundTag3.putByteArray(carving.toString(), chunkAccess.getCarvingMask(carving).toByteArray());
			}

			compoundTag2.put("CarvingMasks", compoundTag3);
		}

		compoundTag2.put("Entities", listTag3);
		TickList<Block> tickList = chunkAccess.getBlockTicks();
		if (tickList instanceof ProtoTickList) {
			compoundTag2.put("ToBeTicked", ((ProtoTickList)tickList).save());
		} else if (tickList instanceof ChunkTickList) {
			compoundTag2.put("TileTicks", ((ChunkTickList)tickList).save(serverLevel.getGameTime()));
		} else {
			compoundTag2.put("TileTicks", serverLevel.getBlockTicks().save(chunkPos));
		}

		TickList<Fluid> tickList2 = chunkAccess.getLiquidTicks();
		if (tickList2 instanceof ProtoTickList) {
			compoundTag2.put("LiquidsToBeTicked", ((ProtoTickList)tickList2).save());
		} else if (tickList2 instanceof ChunkTickList) {
			compoundTag2.put("LiquidTicks", ((ChunkTickList)tickList2).save(serverLevel.getGameTime()));
		} else {
			compoundTag2.put("LiquidTicks", serverLevel.getLiquidTicks().save(chunkPos));
		}

		compoundTag2.put("PostProcessing", packOffsets(chunkAccess.getPostProcessing()));
		CompoundTag compoundTag5 = new CompoundTag();

		for(Entry<Heightmap.Types, Heightmap> entry : chunkAccess.getHeightmaps()) {
			if (chunkAccess.getStatus().heightmapsAfter().contains(entry.getKey())) {
				compoundTag5.put(((Heightmap.Types)entry.getKey()).getSerializationKey(), new LongArrayTag(((Heightmap)entry.getValue()).getRawData()));
			}
		}

		compoundTag2.put("Heightmaps", compoundTag5);
		compoundTag2.put("Structures", packStructureData(chunkPos, chunkAccess.getAllStarts(), chunkAccess.getAllReferences()));
		return compoundTag;
	}

	public static ChunkStatus.ChunkType getChunkTypeFromTag(@Nullable CompoundTag compoundTag) {
		if (compoundTag != null) {
			ChunkStatus chunkStatus = ChunkStatus.byName(compoundTag.getCompound("Level").getString("Status"));
			if (chunkStatus != null) {
				return chunkStatus.getChunkType();
			}
		}

		return ChunkStatus.ChunkType.PROTOCHUNK;
	}

	private static void postLoadChunk(CompoundTag compoundTag, LevelChunk levelChunk) {
		ListTag listTag = compoundTag.getList("Entities", 10);
		Level level = levelChunk.getLevel();

		for(int i = 0; i < listTag.size(); ++i) {
			CompoundTag compoundTag2 = listTag.getCompound(i);
			EntityType.loadEntityRecursive(compoundTag2, level, entity -> {
				levelChunk.addEntity(entity);
				return entity;
			});
			levelChunk.setLastSaveHadEntities(true);
		}

		ListTag listTag2 = compoundTag.getList("TileEntities", 10);

		for(int j = 0; j < listTag2.size(); ++j) {
			CompoundTag compoundTag3 = listTag2.getCompound(j);
			boolean bl = compoundTag3.getBoolean("keepPacked");
			if (bl) {
				levelChunk.setBlockEntityNbt(compoundTag3);
			} else {
				BlockEntity blockEntity = BlockEntity.loadStatic(compoundTag3);
				if (blockEntity != null) {
					levelChunk.addBlockEntity(blockEntity);
				}
			}
		}
	}

	private static CompoundTag packStructureData(ChunkPos chunkPos, Map<String, StructureStart> map, Map<String, LongSet> map2) {
		CompoundTag compoundTag = new CompoundTag();
		CompoundTag compoundTag2 = new CompoundTag();

		for(Entry<String, StructureStart> entry : map.entrySet()) {
			compoundTag2.put((String)entry.getKey(), ((StructureStart)entry.getValue()).createTag(chunkPos.x, chunkPos.z));
		}

		compoundTag.put("Starts", compoundTag2);
		CompoundTag compoundTag3 = new CompoundTag();

		for(Entry<String, LongSet> entry2 : map2.entrySet()) {
			compoundTag3.put((String)entry2.getKey(), new LongArrayTag((LongSet)entry2.getValue()));
		}

		compoundTag.put("References", compoundTag3);
		return compoundTag;
	}

	private static Map<String, StructureStart> unpackStructureStart(
		ChunkGenerator<?> chunkGenerator, StructureManager structureManager, BiomeSource biomeSource, CompoundTag compoundTag
	) {
		Map<String, StructureStart> map = Maps.newHashMap();
		CompoundTag compoundTag2 = compoundTag.getCompound("Starts");

		for(String string : compoundTag2.getAllKeys()) {
			map.put(string, StructureFeatureIO.loadStaticStart(chunkGenerator, structureManager, biomeSource, compoundTag2.getCompound(string)));
		}

		return map;
	}

	private static Map<String, LongSet> unpackStructureReferences(CompoundTag compoundTag) {
		Map<String, LongSet> map = Maps.newHashMap();
		CompoundTag compoundTag2 = compoundTag.getCompound("References");

		for(String string : compoundTag2.getAllKeys()) {
			map.put(string, new LongOpenHashSet(compoundTag2.getLongArray(string)));
		}

		return map;
	}

	public static ListTag packOffsets(ShortList[] shortLists) {
		ListTag listTag = new ListTag();

		for(ShortList shortList : shortLists) {
			ListTag listTag2 = new ListTag();
			if (shortList != null) {
				for(Short short_ : shortList) {
					listTag2.add(new ShortTag(short_));
				}
			}

			listTag.add(listTag2);
		}

		return listTag;
	}
}

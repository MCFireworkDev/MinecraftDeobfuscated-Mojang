package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;

public class GravityProcessor extends StructureProcessor {
	private final Heightmap.Types heightmap;
	private final int offset;

	public GravityProcessor(Heightmap.Types types, int i) {
		this.heightmap = types;
		this.offset = i;
	}

	public GravityProcessor(Dynamic<?> dynamic) {
		this(Heightmap.Types.getFromKey(dynamic.get("heightmap").asString(Heightmap.Types.WORLD_SURFACE_WG.getSerializationKey())), dynamic.get("offset").asInt(0));
	}

	@Nullable
	@Override
	public StructureTemplate.StructureBlockInfo processBlock(
		LevelReader levelReader,
		BlockPos blockPos,
		BlockPos blockPos2,
		StructureTemplate.StructureBlockInfo structureBlockInfo,
		StructureTemplate.StructureBlockInfo structureBlockInfo2,
		StructurePlaceSettings structurePlaceSettings
	) {
		Heightmap.Types types;
		if (levelReader instanceof ServerLevel) {
			if (this.heightmap == Heightmap.Types.WORLD_SURFACE_WG) {
				types = Heightmap.Types.WORLD_SURFACE;
			} else if (this.heightmap == Heightmap.Types.OCEAN_FLOOR_WG) {
				types = Heightmap.Types.OCEAN_FLOOR;
			} else {
				types = this.heightmap;
			}
		} else {
			types = this.heightmap;
		}

		int i = levelReader.getHeight(types, structureBlockInfo2.pos.getX(), structureBlockInfo2.pos.getZ()) + this.offset;
		int j = structureBlockInfo.pos.getY();
		return new StructureTemplate.StructureBlockInfo(
			new BlockPos(structureBlockInfo2.pos.getX(), i + j, structureBlockInfo2.pos.getZ()), structureBlockInfo2.state, structureBlockInfo2.nbt
		);
	}

	@Override
	protected StructureProcessorType getType() {
		return StructureProcessorType.GRAVITY;
	}

	@Override
	protected <T> Dynamic<T> getDynamic(DynamicOps<T> dynamicOps) {
		return new Dynamic<>(
			dynamicOps,
			dynamicOps.createMap(
				ImmutableMap.of(
					dynamicOps.createString("heightmap"),
					dynamicOps.createString(this.heightmap.getSerializationKey()),
					dynamicOps.createString("offset"),
					dynamicOps.createInt(this.offset)
				)
			)
		);
	}
}

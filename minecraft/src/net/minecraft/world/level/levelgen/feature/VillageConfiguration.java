package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import net.minecraft.resources.ResourceLocation;

public class VillageConfiguration implements FeatureConfiguration {
	public final ResourceLocation startPool;
	public final int size;

	public VillageConfiguration(String string, int i) {
		this.startPool = new ResourceLocation(string);
		this.size = i;
	}

	@Override
	public <T> Dynamic<T> serialize(DynamicOps<T> dynamicOps) {
		return new Dynamic<>(
			dynamicOps,
			dynamicOps.createMap(
				ImmutableMap.of(
					dynamicOps.createString("start_pool"),
					dynamicOps.createString(this.startPool.toString()),
					dynamicOps.createString("size"),
					dynamicOps.createInt(this.size)
				)
			)
		);
	}

	public static <T> VillageConfiguration deserialize(Dynamic<T> dynamic) {
		String string = dynamic.get("start_pool").asString("");
		int i = dynamic.get("size").asInt(6);
		return new VillageConfiguration(string, i);
	}
}

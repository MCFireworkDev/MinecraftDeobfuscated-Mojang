package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.util.StringRepresentable;

public enum MobCategory implements StringRepresentable {
	MONSTER("monster", 70, false, 128),
	CREATURE("creature", 10, true),
	AMBIENT("ambient", 15, true, 128),
	WATER_CREATURE("water_creature", 5, true, 128),
	WATER_AMBIENT("water_ambient", 20, true, 64),
	MISC("misc", -1, true);

	public static final Codec<MobCategory> CODEC = StringRepresentable.fromEnum(MobCategory::values, MobCategory::byName);
	private static final Map<String, MobCategory> BY_NAME = (Map<String, MobCategory>)Arrays.stream(values())
		.collect(Collectors.toMap(MobCategory::getName, mobCategory -> mobCategory));
	private final int max;
	private final boolean isFriendly;
	private final boolean isPersistent;
	private final String name;
	private final int noDespawnDistance = 32;
	private final int despawnDistance;

	private MobCategory(String string2, int j, boolean bl) {
		this.name = string2;
		this.max = j;
		this.isFriendly = bl;
		this.isPersistent = true;
		this.despawnDistance = Integer.MAX_VALUE;
	}

	private MobCategory(String string2, int j, boolean bl, int k) {
		this.name = string2;
		this.max = j;
		this.isFriendly = bl;
		this.isPersistent = false;
		this.despawnDistance = k;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public static MobCategory byName(String string) {
		return (MobCategory)BY_NAME.get(string);
	}

	public int getMaxInstancesPerChunk() {
		return this.max;
	}

	public boolean isFriendly() {
		return this.isFriendly;
	}

	public boolean isPersistent() {
		return this.isPersistent;
	}

	public int getDespawnDistance() {
		return this.despawnDistance;
	}

	public int getNoDespawnDistance() {
		return 32;
	}
}

package net.minecraft.world.entity.ai.village.poi;

import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.BlockState;

public record PoiType(Set<BlockState> matchingStates, int maxTickets, int validRange) {
	public static final Predicate<Holder<PoiType>> NONE = holder -> false;

	public PoiType(Set<BlockState> set, int i, int j) {
		set = Set.copyOf(set);
		this.matchingStates = set;
		this.maxTickets = i;
		this.validRange = j;
	}

	public boolean is(BlockState blockState) {
		return this.matchingStates.contains(blockState);
	}
}

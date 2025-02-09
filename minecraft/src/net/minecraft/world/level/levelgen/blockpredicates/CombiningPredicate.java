package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Function;

abstract class CombiningPredicate implements BlockPredicate {
	protected final List<BlockPredicate> predicates;

	protected CombiningPredicate(List<BlockPredicate> list) {
		this.predicates = list;
	}

	public static <T extends CombiningPredicate> Codec<T> codec(Function<List<BlockPredicate>, T> function) {
		return RecordCodecBuilder.create(
			instance -> instance.group(BlockPredicate.CODEC.listOf().fieldOf("predicates").forGetter(combiningPredicate -> combiningPredicate.predicates))
					.apply(instance, function)
		);
	}
}

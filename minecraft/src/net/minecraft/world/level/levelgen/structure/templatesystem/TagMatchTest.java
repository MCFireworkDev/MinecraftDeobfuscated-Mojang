package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TagMatchTest extends RuleTest {
	public static final Codec<TagMatchTest> CODEC = TagKey.codec(Registries.BLOCK)
		.fieldOf("tag")
		.<TagMatchTest>xmap(TagMatchTest::new, tagMatchTest -> tagMatchTest.tag)
		.codec();
	private final TagKey<Block> tag;

	public TagMatchTest(TagKey<Block> tagKey) {
		this.tag = tagKey;
	}

	@Override
	public boolean test(BlockState blockState, RandomSource randomSource) {
		return blockState.is(this.tag);
	}

	@Override
	protected RuleTestType<?> getType() {
		return RuleTestType.TAG_TEST;
	}
}

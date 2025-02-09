package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public class AppendStatic implements RuleBlockEntityModifier {
	public static final Codec<AppendStatic> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(CompoundTag.CODEC.fieldOf("data").forGetter(appendStatic -> appendStatic.tag)).apply(instance, AppendStatic::new)
	);
	private final CompoundTag tag;

	public AppendStatic(CompoundTag compoundTag) {
		this.tag = compoundTag;
	}

	@Override
	public CompoundTag apply(RandomSource randomSource, @Nullable CompoundTag compoundTag) {
		return compoundTag == null ? this.tag.copy() : compoundTag.merge(this.tag);
	}

	@Override
	public RuleBlockEntityModifierType<?> getType() {
		return RuleBlockEntityModifierType.APPEND_STATIC;
	}
}

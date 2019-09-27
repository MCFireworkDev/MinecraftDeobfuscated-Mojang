package net.minecraft.world.level.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

public abstract class AbstractSkullBlock extends BaseEntityBlock {
	private final SkullBlock.Type type;

	public AbstractSkullBlock(SkullBlock.Type type, Block.Properties properties) {
		super(properties);
		this.type = type;
	}

	@Override
	public BlockEntity newBlockEntity(BlockGetter blockGetter) {
		return new SkullBlockEntity();
	}

	@Environment(EnvType.CLIENT)
	public SkullBlock.Type getType() {
		return this.type;
	}
}

package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public abstract class AbstractProjectileDispenseBehavior extends DefaultDispenseItemBehavior {
	@Override
	public ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
		Level level = blockSource.level();
		Position position = DispenserBlock.getDispensePosition(blockSource);
		Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
		Projectile projectile = this.getProjectile(level, position, itemStack);
		projectile.shoot(
			(double)direction.getStepX(), (double)((float)direction.getStepY() + 0.1F), (double)direction.getStepZ(), this.getPower(), this.getUncertainty()
		);
		level.addFreshEntity(projectile);
		itemStack.shrink(1);
		return itemStack;
	}

	@Override
	protected void playSound(BlockSource blockSource) {
		blockSource.level().levelEvent(1002, blockSource.pos(), 0);
	}

	protected abstract Projectile getProjectile(Level level, Position position, ItemStack itemStack);

	protected float getUncertainty() {
		return 6.0F;
	}

	protected float getPower() {
		return 1.1F;
	}
}

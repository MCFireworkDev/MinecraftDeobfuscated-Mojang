package net.minecraft.world.item;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArmorStandItem extends Item {
	public ArmorStandItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext useOnContext) {
		Direction direction = useOnContext.getClickedFace();
		if (direction == Direction.DOWN) {
			return InteractionResult.FAIL;
		} else {
			Level level = useOnContext.getLevel();
			BlockPlaceContext blockPlaceContext = new BlockPlaceContext(useOnContext);
			BlockPos blockPos = blockPlaceContext.getClickedPos();
			ItemStack itemStack = useOnContext.getItemInHand();
			Vec3 vec3 = Vec3.atBottomCenterOf(blockPos);
			AABB aABB = EntityType.ARMOR_STAND.getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());
			if (level.noCollision(null, aABB) && level.getEntities(null, aABB).isEmpty()) {
				if (level instanceof ServerLevel serverLevel) {
					Consumer<ArmorStand> consumer = EntityType.createDefaultStackConfig(serverLevel, itemStack, useOnContext.getPlayer());
					ArmorStand armorStand = EntityType.ARMOR_STAND.create(serverLevel, itemStack.getTag(), consumer, blockPos, MobSpawnType.SPAWN_EGG, true, true);
					if (armorStand == null) {
						return InteractionResult.FAIL;
					}

					float f = (float)Mth.floor((Mth.wrapDegrees(useOnContext.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
					armorStand.moveTo(armorStand.getX(), armorStand.getY(), armorStand.getZ(), f, 0.0F);
					serverLevel.addFreshEntityWithPassengers(armorStand);
					level.playSound(null, armorStand.getX(), armorStand.getY(), armorStand.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
					armorStand.gameEvent(GameEvent.ENTITY_PLACE, useOnContext.getPlayer());
				}

				itemStack.shrink(1);
				return InteractionResult.sidedSuccess(level.isClientSide);
			} else {
				return InteractionResult.FAIL;
			}
		}
	}
}

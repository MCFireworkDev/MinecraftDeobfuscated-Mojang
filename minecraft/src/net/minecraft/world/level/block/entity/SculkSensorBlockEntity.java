package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationListener;

public class SculkSensorBlockEntity extends BlockEntity implements VibrationListener.VibrationListenerConfig {
	private final VibrationListener listener;
	private int lastVibrationFrequency;

	public SculkSensorBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BlockEntityType.SCULK_SENSOR, blockPos, blockState);
		this.listener = new VibrationListener(new BlockPositionSource(this.worldPosition), ((SculkSensorBlock)blockState.getBlock()).getListenerRange(), this);
	}

	@Override
	public void load(CompoundTag compoundTag) {
		super.load(compoundTag);
		this.lastVibrationFrequency = compoundTag.getInt("last_vibration_frequency");
	}

	@Override
	public CompoundTag save(CompoundTag compoundTag) {
		super.save(compoundTag);
		compoundTag.putInt("last_vibration_frequency", this.lastVibrationFrequency);
		return compoundTag;
	}

	public VibrationListener getListener() {
		return this.listener;
	}

	public int getLastVibrationFrequency() {
		return this.lastVibrationFrequency;
	}

	@Override
	public boolean shouldListen(Level level, GameEventListener gameEventListener, BlockPos blockPos, GameEvent gameEvent, @Nullable Entity entity) {
		boolean bl = gameEvent == GameEvent.BLOCK_DESTROY && blockPos.equals(this.getBlockPos());
		boolean bl2 = gameEvent == GameEvent.BLOCK_PLACE && blockPos.equals(this.getBlockPos());
		return !bl && !bl2 && SculkSensorBlock.canActivate(this.getBlockState());
	}

	@Override
	public void onSignalReceive(Level level, GameEventListener gameEventListener, GameEvent gameEvent, int i) {
		BlockState blockState = this.getBlockState();
		if (!level.isClientSide() && SculkSensorBlock.canActivate(blockState)) {
			this.lastVibrationFrequency = SculkSensorBlock.VIBRATION_STRENGTH_FOR_EVENT.getInt(gameEvent);
			SculkSensorBlock.activate(level, this.worldPosition, blockState, getRedstoneStrengthForDistance(i, gameEventListener.getListenerRadius()));
		}
	}

	public static int getRedstoneStrengthForDistance(int i, int j) {
		double d = (double)i / (double)j;
		return Math.max(1, 15 - Mth.floor(d * 15.0));
	}
}

package net.minecraft.client.player;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerLevel;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.resources.sounds.BubbleColumnAmbientSoundHandler;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundHandler;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundInstances;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(EnvType.CLIENT)
public class LocalPlayer extends AbstractClientPlayer {
	public final ClientPacketListener connection;
	private final StatsCounter stats;
	private final ClientRecipeBook recipeBook;
	private final List<AmbientSoundHandler> ambientSoundHandlers = Lists.<AmbientSoundHandler>newArrayList();
	private int permissionLevel = 0;
	private double xLast;
	private double yLast1;
	private double zLast;
	private float yRotLast;
	private float xRotLast;
	private boolean lastOnGround;
	private boolean wasTryingToSneak;
	private boolean wasSprinting;
	private int positionReminder;
	private boolean flashOnSetHealth;
	private String serverBrand;
	public Input input;
	protected final Minecraft minecraft;
	protected int sprintTriggerTime;
	public int sprintTime;
	public float yBob;
	public float xBob;
	public float yBobO;
	public float xBobO;
	private int jumpRidingTicks;
	private float jumpRidingScale;
	public float portalTime;
	public float oPortalTime;
	private boolean startedUsingItem;
	private InteractionHand usingItemHand;
	private boolean handsBusy;
	private boolean autoJumpEnabled = true;
	private int autoJumpTime;
	private boolean wasFallFlying;
	private int waterVisionTime;

	public LocalPlayer(
		Minecraft minecraft,
		MultiPlayerLevel multiPlayerLevel,
		ClientPacketListener clientPacketListener,
		StatsCounter statsCounter,
		ClientRecipeBook clientRecipeBook
	) {
		super(multiPlayerLevel, clientPacketListener.getLocalGameProfile());
		this.connection = clientPacketListener;
		this.stats = statsCounter;
		this.recipeBook = clientRecipeBook;
		this.minecraft = minecraft;
		this.dimension = DimensionType.OVERWORLD;
		this.ambientSoundHandlers.add(new UnderwaterAmbientSoundHandler(this, minecraft.getSoundManager()));
		this.ambientSoundHandlers.add(new BubbleColumnAmbientSoundHandler(this));
	}

	@Override
	public boolean hurt(DamageSource damageSource, float f) {
		return false;
	}

	@Override
	public void heal(float f) {
	}

	@Override
	public boolean startRiding(Entity entity, boolean bl) {
		if (!super.startRiding(entity, bl)) {
			return false;
		} else {
			if (entity instanceof AbstractMinecart) {
				this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)entity));
			}

			if (entity instanceof Boat) {
				this.yRotO = entity.yRot;
				this.yRot = entity.yRot;
				this.setYHeadRot(entity.yRot);
			}

			return true;
		}
	}

	@Override
	public void stopRiding() {
		super.stopRiding();
		this.handsBusy = false;
	}

	@Override
	public float getViewXRot(float f) {
		return this.xRot;
	}

	@Override
	public float getViewYRot(float f) {
		return this.isPassenger() ? super.getViewYRot(f) : this.yRot;
	}

	@Override
	public void tick() {
		if (this.level.hasChunkAt(new BlockPos(this.x, 0.0, this.z))) {
			super.tick();
			if (this.isPassenger()) {
				this.connection.send(new ServerboundMovePlayerPacket.Rot(this.yRot, this.xRot, this.onGround));
				this.connection.send(new ServerboundPlayerInputPacket(this.xxa, this.zza, this.input.jumping, this.input.sneakKeyDown));
				Entity entity = this.getRootVehicle();
				if (entity != this && entity.isControlledByLocalInstance()) {
					this.connection.send(new ServerboundMoveVehiclePacket(entity));
				}
			} else {
				this.sendPosition();
			}

			for(AmbientSoundHandler ambientSoundHandler : this.ambientSoundHandlers) {
				ambientSoundHandler.tick();
			}
		}
	}

	private void sendPosition() {
		boolean bl = this.isSprinting();
		if (bl != this.wasSprinting) {
			ServerboundPlayerCommandPacket.Action action = bl
				? ServerboundPlayerCommandPacket.Action.START_SPRINTING
				: ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
			this.connection.send(new ServerboundPlayerCommandPacket(this, action));
			this.wasSprinting = bl;
		}

		boolean bl2 = this.isTryingToSneak();
		if (bl2 != this.wasTryingToSneak) {
			ServerboundPlayerCommandPacket.Action action2 = bl2
				? ServerboundPlayerCommandPacket.Action.START_SNEAKING
				: ServerboundPlayerCommandPacket.Action.STOP_SNEAKING;
			this.connection.send(new ServerboundPlayerCommandPacket(this, action2));
			this.wasTryingToSneak = bl2;
		}

		if (this.isControlledCamera()) {
			AABB aABB = this.getBoundingBox();
			double d = this.x - this.xLast;
			double e = aABB.minY - this.yLast1;
			double f = this.z - this.zLast;
			double g = (double)(this.yRot - this.yRotLast);
			double h = (double)(this.xRot - this.xRotLast);
			++this.positionReminder;
			boolean bl3 = d * d + e * e + f * f > 9.0E-4 || this.positionReminder >= 20;
			boolean bl4 = g != 0.0 || h != 0.0;
			if (this.isPassenger()) {
				Vec3 vec3 = this.getDeltaMovement();
				this.connection.send(new ServerboundMovePlayerPacket.PosRot(vec3.x, -999.0, vec3.z, this.yRot, this.xRot, this.onGround));
				bl3 = false;
			} else if (bl3 && bl4) {
				this.connection.send(new ServerboundMovePlayerPacket.PosRot(this.x, aABB.minY, this.z, this.yRot, this.xRot, this.onGround));
			} else if (bl3) {
				this.connection.send(new ServerboundMovePlayerPacket.Pos(this.x, aABB.minY, this.z, this.onGround));
			} else if (bl4) {
				this.connection.send(new ServerboundMovePlayerPacket.Rot(this.yRot, this.xRot, this.onGround));
			} else if (this.lastOnGround != this.onGround) {
				this.connection.send(new ServerboundMovePlayerPacket(this.onGround));
			}

			if (bl3) {
				this.xLast = this.x;
				this.yLast1 = aABB.minY;
				this.zLast = this.z;
				this.positionReminder = 0;
			}

			if (bl4) {
				this.yRotLast = this.yRot;
				this.xRotLast = this.xRot;
			}

			this.lastOnGround = this.onGround;
			this.autoJumpEnabled = this.minecraft.options.autoJump;
		}
	}

	@Nullable
	@Override
	public ItemEntity drop(boolean bl) {
		ServerboundPlayerActionPacket.Action action = bl ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS : ServerboundPlayerActionPacket.Action.DROP_ITEM;
		this.connection.send(new ServerboundPlayerActionPacket(action, BlockPos.ZERO, Direction.DOWN));
		this.inventory.removeItem(this.inventory.selected, bl && !this.inventory.getSelected().isEmpty() ? this.inventory.getSelected().getCount() : 1);
		return null;
	}

	public void chat(String string) {
		this.connection.send(new ServerboundChatPacket(string));
	}

	@Override
	public void swing(InteractionHand interactionHand) {
		super.swing(interactionHand);
		this.connection.send(new ServerboundSwingPacket(interactionHand));
	}

	@Override
	public void respawn() {
		this.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
	}

	@Override
	protected void actuallyHurt(DamageSource damageSource, float f) {
		if (!this.isInvulnerableTo(damageSource)) {
			this.setHealth(this.getHealth() - f);
		}
	}

	@Override
	public void closeContainer() {
		this.connection.send(new ServerboundContainerClosePacket(this.containerMenu.containerId));
		this.clientSideCloseContainer();
	}

	public void clientSideCloseContainer() {
		this.inventory.setCarried(ItemStack.EMPTY);
		super.closeContainer();
		this.minecraft.setScreen(null);
	}

	public void hurtTo(float f) {
		if (this.flashOnSetHealth) {
			float g = this.getHealth() - f;
			if (g <= 0.0F) {
				this.setHealth(f);
				if (g < 0.0F) {
					this.invulnerableTime = 10;
				}
			} else {
				this.lastHurt = g;
				this.setHealth(this.getHealth());
				this.invulnerableTime = 20;
				this.actuallyHurt(DamageSource.GENERIC, g);
				this.hurtDuration = 10;
				this.hurtTime = this.hurtDuration;
			}
		} else {
			this.setHealth(f);
			this.flashOnSetHealth = true;
		}
	}

	@Override
	public void onUpdateAbilities() {
		this.connection.send(new ServerboundPlayerAbilitiesPacket(this.abilities));
	}

	@Override
	public boolean isLocalPlayer() {
		return true;
	}

	protected void sendRidingJump() {
		this.connection
			.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP, Mth.floor(this.getJumpRidingScale() * 100.0F)));
	}

	public void sendOpenInventory() {
		this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
	}

	public void setServerBrand(String string) {
		this.serverBrand = string;
	}

	public String getServerBrand() {
		return this.serverBrand;
	}

	public StatsCounter getStats() {
		return this.stats;
	}

	public ClientRecipeBook getRecipeBook() {
		return this.recipeBook;
	}

	public void removeRecipeHighlight(Recipe<?> recipe) {
		if (this.recipeBook.willHighlight(recipe)) {
			this.recipeBook.removeHighlight(recipe);
			this.connection.send(new ServerboundRecipeBookUpdatePacket(recipe));
		}
	}

	@Override
	protected int getPermissionLevel() {
		return this.permissionLevel;
	}

	public void setPermissionLevel(int i) {
		this.permissionLevel = i;
	}

	@Override
	public void displayClientMessage(Component component, boolean bl) {
		if (bl) {
			this.minecraft.gui.setOverlayMessage(component, false);
		} else {
			this.minecraft.gui.getChat().addMessage(component);
		}
	}

	@Override
	protected void checkInBlock(double d, double e, double f) {
		BlockPos blockPos = new BlockPos(d, e, f);
		if (this.blocked(blockPos)) {
			double g = d - (double)blockPos.getX();
			double h = f - (double)blockPos.getZ();
			Direction direction = null;
			double i = 9999.0;
			if (!this.blocked(blockPos.west()) && g < i) {
				i = g;
				direction = Direction.WEST;
			}

			if (!this.blocked(blockPos.east()) && 1.0 - g < i) {
				i = 1.0 - g;
				direction = Direction.EAST;
			}

			if (!this.blocked(blockPos.north()) && h < i) {
				i = h;
				direction = Direction.NORTH;
			}

			if (!this.blocked(blockPos.south()) && 1.0 - h < i) {
				i = 1.0 - h;
				direction = Direction.SOUTH;
			}

			if (direction != null) {
				Vec3 vec3 = this.getDeltaMovement();
				switch(direction) {
					case WEST:
						this.setDeltaMovement(-0.1, vec3.y, vec3.z);
						break;
					case EAST:
						this.setDeltaMovement(0.1, vec3.y, vec3.z);
						break;
					case NORTH:
						this.setDeltaMovement(vec3.x, vec3.y, -0.1);
						break;
					case SOUTH:
						this.setDeltaMovement(vec3.x, vec3.y, 0.1);
				}
			}
		}
	}

	private boolean blocked(BlockPos blockPos) {
		AABB aABB = this.getBoundingBox();
		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(blockPos);

		for(int i = Mth.floor(aABB.minY); i < Mth.ceil(aABB.maxY); ++i) {
			mutableBlockPos.setY(i);
			if (!this.freeAt(mutableBlockPos)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void setSprinting(boolean bl) {
		super.setSprinting(bl);
		this.sprintTime = 0;
	}

	public void setExperienceValues(float f, int i, int j) {
		this.experienceProgress = f;
		this.totalExperience = i;
		this.experienceLevel = j;
	}

	@Override
	public void sendMessage(Component component) {
		this.minecraft.gui.getChat().addMessage(component);
	}

	@Override
	public void handleEntityEvent(byte b) {
		if (b >= 24 && b <= 28) {
			this.setPermissionLevel(b - 24);
		} else {
			super.handleEntityEvent(b);
		}
	}

	@Override
	public void playSound(SoundEvent soundEvent, float f, float g) {
		this.level.playLocalSound(this.x, this.y, this.z, soundEvent, this.getSoundSource(), f, g, false);
	}

	@Override
	public void playNotifySound(SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
		this.level.playLocalSound(this.x, this.y, this.z, soundEvent, soundSource, f, g, false);
	}

	@Override
	public boolean isEffectiveAi() {
		return true;
	}

	@Override
	public void startUsingItem(InteractionHand interactionHand) {
		ItemStack itemStack = this.getItemInHand(interactionHand);
		if (!itemStack.isEmpty() && !this.isUsingItem()) {
			super.startUsingItem(interactionHand);
			this.startedUsingItem = true;
			this.usingItemHand = interactionHand;
		}
	}

	@Override
	public boolean isUsingItem() {
		return this.startedUsingItem;
	}

	@Override
	public void stopUsingItem() {
		super.stopUsingItem();
		this.startedUsingItem = false;
	}

	@Override
	public InteractionHand getUsedItemHand() {
		return this.usingItemHand;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> entityDataAccessor) {
		super.onSyncedDataUpdated(entityDataAccessor);
		if (DATA_LIVING_ENTITY_FLAGS.equals(entityDataAccessor)) {
			boolean bl = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
			InteractionHand interactionHand = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
			if (bl && !this.startedUsingItem) {
				this.startUsingItem(interactionHand);
			} else if (!bl && this.startedUsingItem) {
				this.stopUsingItem();
			}
		}

		if (DATA_SHARED_FLAGS_ID.equals(entityDataAccessor) && this.isFallFlying() && !this.wasFallFlying) {
			this.minecraft.getSoundManager().play(new ElytraOnPlayerSoundInstance(this));
		}
	}

	public boolean isRidingJumpable() {
		Entity entity = this.getVehicle();
		return this.isPassenger() && entity instanceof PlayerRideableJumping && ((PlayerRideableJumping)entity).canJump();
	}

	public float getJumpRidingScale() {
		return this.jumpRidingScale;
	}

	@Override
	public void openTextEdit(SignBlockEntity signBlockEntity) {
		this.minecraft.setScreen(new SignEditScreen(signBlockEntity));
	}

	@Override
	public void openMinecartCommandBlock(BaseCommandBlock baseCommandBlock) {
		this.minecraft.setScreen(new MinecartCommandBlockEditScreen(baseCommandBlock));
	}

	@Override
	public void openCommandBlock(CommandBlockEntity commandBlockEntity) {
		this.minecraft.setScreen(new CommandBlockEditScreen(commandBlockEntity));
	}

	@Override
	public void openStructureBlock(StructureBlockEntity structureBlockEntity) {
		this.minecraft.setScreen(new StructureBlockEditScreen(structureBlockEntity));
	}

	@Override
	public void openJigsawBlock(JigsawBlockEntity jigsawBlockEntity) {
		this.minecraft.setScreen(new JigsawBlockEditScreen(jigsawBlockEntity));
	}

	@Override
	public void openItemGui(ItemStack itemStack, InteractionHand interactionHand) {
		Item item = itemStack.getItem();
		if (item == Items.WRITABLE_BOOK) {
			this.minecraft.setScreen(new BookEditScreen(this, itemStack, interactionHand));
		}
	}

	@Override
	public void crit(Entity entity) {
		this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.CRIT);
	}

	@Override
	public void magicCrit(Entity entity) {
		this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.ENCHANTED_HIT);
	}

	@Override
	public boolean isSneaking() {
		return this.isTryingToSneak();
	}

	public boolean isTryingToSneak() {
		return this.input != null && this.input.sneakKeyDown;
	}

	@Override
	public boolean isVisuallySneaking() {
		if (!this.abilities.flying && !this.isSwimming() && this.canEnterPose(Pose.SNEAKING)) {
			return this.isTryingToSneak() || !this.canEnterPose(Pose.STANDING);
		} else {
			return false;
		}
	}

	@Override
	public void serverAiStep() {
		super.serverAiStep();
		if (this.isControlledCamera()) {
			this.xxa = this.input.leftImpulse;
			this.zza = this.input.forwardImpulse;
			this.jumping = this.input.jumping;
			this.yBobO = this.yBob;
			this.xBobO = this.xBob;
			this.xBob = (float)((double)this.xBob + (double)(this.xRot - this.xBob) * 0.5);
			this.yBob = (float)((double)this.yBob + (double)(this.yRot - this.yBob) * 0.5);
		}
	}

	protected boolean isControlledCamera() {
		return this.minecraft.getCameraEntity() == this;
	}

	@Override
	public void aiStep() {
		++this.sprintTime;
		if (this.sprintTriggerTime > 0) {
			--this.sprintTriggerTime;
		}

		this.handleNetherPortalClient();
		boolean bl = this.input.jumping;
		boolean bl2 = this.input.sneakKeyDown;
		boolean bl3 = this.hasEnoughImpulseToStartSprinting();
		boolean bl4 = this.isVisuallySneaking() || this.isVisuallyCrawling();
		this.input.tick(bl4, this.isSpectator());
		this.minecraft.getTutorial().onInput(this.input);
		if (this.isUsingItem() && !this.isPassenger()) {
			this.input.leftImpulse *= 0.2F;
			this.input.forwardImpulse *= 0.2F;
			this.sprintTriggerTime = 0;
		}

		boolean bl5 = false;
		if (this.autoJumpTime > 0) {
			--this.autoJumpTime;
			bl5 = true;
			this.input.jumping = true;
		}

		if (!this.noPhysics) {
			AABB aABB = this.getBoundingBox();
			this.checkInBlock(this.x - (double)this.getBbWidth() * 0.35, aABB.minY + 0.5, this.z + (double)this.getBbWidth() * 0.35);
			this.checkInBlock(this.x - (double)this.getBbWidth() * 0.35, aABB.minY + 0.5, this.z - (double)this.getBbWidth() * 0.35);
			this.checkInBlock(this.x + (double)this.getBbWidth() * 0.35, aABB.minY + 0.5, this.z - (double)this.getBbWidth() * 0.35);
			this.checkInBlock(this.x + (double)this.getBbWidth() * 0.35, aABB.minY + 0.5, this.z + (double)this.getBbWidth() * 0.35);
		}

		boolean bl6 = (float)this.getFoodData().getFoodLevel() > 6.0F || this.abilities.mayfly;
		if ((this.onGround || this.isUnderWater())
			&& !bl2
			&& !bl3
			&& this.hasEnoughImpulseToStartSprinting()
			&& !this.isSprinting()
			&& bl6
			&& !this.isUsingItem()
			&& !this.hasEffect(MobEffects.BLINDNESS)) {
			if (this.sprintTriggerTime <= 0 && !this.minecraft.options.keySprint.isDown()) {
				this.sprintTriggerTime = 7;
			} else {
				this.setSprinting(true);
			}
		}

		if (!this.isSprinting()
			&& (!this.isInWater() || this.isUnderWater())
			&& this.hasEnoughImpulseToStartSprinting()
			&& bl6
			&& !this.isUsingItem()
			&& !this.hasEffect(MobEffects.BLINDNESS)
			&& this.minecraft.options.keySprint.isDown()) {
			this.setSprinting(true);
		}

		if (this.isSprinting()) {
			boolean bl7 = !this.input.hasForwardImpulse() || !bl6;
			boolean bl8 = bl7 || this.horizontalCollision || this.isInWater() && !this.isUnderWater();
			if (this.isSwimming()) {
				if (!this.onGround && !this.input.sneakKeyDown && bl7 || !this.isInWater()) {
					this.setSprinting(false);
				}
			} else if (bl8) {
				this.setSprinting(false);
			}
		}

		if (this.abilities.mayfly) {
			if (this.minecraft.gameMode.isAlwaysFlying()) {
				if (!this.abilities.flying) {
					this.abilities.flying = true;
					this.onUpdateAbilities();
				}
			} else if (!bl && this.input.jumping && !bl5) {
				if (this.jumpTriggerTime == 0) {
					this.jumpTriggerTime = 7;
				} else if (!this.isSwimming()) {
					this.abilities.flying = !this.abilities.flying;
					this.onUpdateAbilities();
					this.jumpTriggerTime = 0;
				}
			}
		}

		if (this.input.jumping && !bl && !this.onGround && this.getDeltaMovement().y < 0.0 && !this.isFallFlying() && !this.abilities.flying) {
			ItemStack itemStack = this.getItemBySlot(EquipmentSlot.CHEST);
			if (itemStack.getItem() == Items.ELYTRA && ElytraItem.isFlyEnabled(itemStack)) {
				this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
			}
		}

		this.wasFallFlying = this.isFallFlying();
		if (this.isInWater() && this.input.sneakKeyDown) {
			this.goDownInWater();
		}

		if (this.isUnderLiquid(FluidTags.WATER)) {
			int i = this.isSpectator() ? 10 : 1;
			this.waterVisionTime = Mth.clamp(this.waterVisionTime + i, 0, 600);
		} else if (this.waterVisionTime > 0) {
			this.isUnderLiquid(FluidTags.WATER);
			this.waterVisionTime = Mth.clamp(this.waterVisionTime - 10, 0, 600);
		}

		if (this.abilities.flying && this.isControlledCamera()) {
			int i = 0;
			if (this.input.sneakKeyDown) {
				this.input.leftImpulse = (float)((double)this.input.leftImpulse / 0.3);
				this.input.forwardImpulse = (float)((double)this.input.forwardImpulse / 0.3);
				--i;
			}

			if (this.input.jumping) {
				++i;
			}

			if (i != 0) {
				this.setDeltaMovement(this.getDeltaMovement().add(0.0, (double)((float)i * this.abilities.getFlyingSpeed() * 3.0F), 0.0));
			}
		}

		if (this.isRidingJumpable()) {
			PlayerRideableJumping playerRideableJumping = (PlayerRideableJumping)this.getVehicle();
			if (this.jumpRidingTicks < 0) {
				++this.jumpRidingTicks;
				if (this.jumpRidingTicks == 0) {
					this.jumpRidingScale = 0.0F;
				}
			}

			if (bl && !this.input.jumping) {
				this.jumpRidingTicks = -10;
				playerRideableJumping.onPlayerJump(Mth.floor(this.getJumpRidingScale() * 100.0F));
				this.sendRidingJump();
			} else if (!bl && this.input.jumping) {
				this.jumpRidingTicks = 0;
				this.jumpRidingScale = 0.0F;
			} else if (bl) {
				++this.jumpRidingTicks;
				if (this.jumpRidingTicks < 10) {
					this.jumpRidingScale = (float)this.jumpRidingTicks * 0.1F;
				} else {
					this.jumpRidingScale = 0.8F + 2.0F / (float)(this.jumpRidingTicks - 9) * 0.1F;
				}
			}
		} else {
			this.jumpRidingScale = 0.0F;
		}

		super.aiStep();
		if (this.onGround && this.abilities.flying && !this.minecraft.gameMode.isAlwaysFlying()) {
			this.abilities.flying = false;
			this.onUpdateAbilities();
		}
	}

	private void handleNetherPortalClient() {
		this.oPortalTime = this.portalTime;
		if (this.isInsidePortal) {
			if (this.minecraft.screen != null && !this.minecraft.screen.isPauseScreen()) {
				if (this.minecraft.screen instanceof AbstractContainerScreen) {
					this.closeContainer();
				}

				this.minecraft.setScreen(null);
			}

			if (this.portalTime == 0.0F) {
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PORTAL_TRIGGER, this.random.nextFloat() * 0.4F + 0.8F));
			}

			this.portalTime += 0.0125F;
			if (this.portalTime >= 1.0F) {
				this.portalTime = 1.0F;
			}

			this.isInsidePortal = false;
		} else if (this.hasEffect(MobEffects.CONFUSION) && this.getEffect(MobEffects.CONFUSION).getDuration() > 60) {
			this.portalTime += 0.006666667F;
			if (this.portalTime > 1.0F) {
				this.portalTime = 1.0F;
			}
		} else {
			if (this.portalTime > 0.0F) {
				this.portalTime -= 0.05F;
			}

			if (this.portalTime < 0.0F) {
				this.portalTime = 0.0F;
			}
		}

		this.processDimensionDelay();
	}

	@Override
	public void rideTick() {
		super.rideTick();
		this.handsBusy = false;
		if (this.getVehicle() instanceof Boat) {
			Boat boat = (Boat)this.getVehicle();
			boat.setInput(this.input.left, this.input.right, this.input.up, this.input.down);
			this.handsBusy |= this.input.left || this.input.right || this.input.up || this.input.down;
		}
	}

	public boolean isHandsBusy() {
		return this.handsBusy;
	}

	@Nullable
	@Override
	public MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect mobEffect) {
		if (mobEffect == MobEffects.CONFUSION) {
			this.oPortalTime = 0.0F;
			this.portalTime = 0.0F;
		}

		return super.removeEffectNoUpdate(mobEffect);
	}

	@Override
	public void move(MoverType moverType, Vec3 vec3) {
		double d = this.x;
		double e = this.z;
		super.move(moverType, vec3);
		this.updateAutoJump((float)(this.x - d), (float)(this.z - e));
	}

	public boolean isAutoJumpEnabled() {
		return this.autoJumpEnabled;
	}

	protected void updateAutoJump(float f, float g) {
		if (this.isAutoJumpEnabled()) {
			if (this.autoJumpTime <= 0 && this.onGround && !this.isSneaking() && !this.isPassenger()) {
				Vec2 vec2 = this.input.getMoveVector();
				if (vec2.x != 0.0F || vec2.y != 0.0F) {
					Vec3 vec3 = new Vec3(this.x, this.getBoundingBox().minY, this.z);
					Vec3 vec32 = new Vec3(this.x + (double)f, this.getBoundingBox().minY, this.z + (double)g);
					Vec3 vec33 = new Vec3((double)f, 0.0, (double)g);
					float h = this.getSpeed();
					float i = (float)vec33.lengthSqr();
					if (i <= 0.001F) {
						float j = h * vec2.x;
						float k = h * vec2.y;
						float l = Mth.sin(this.yRot * (float) (Math.PI / 180.0));
						float m = Mth.cos(this.yRot * (float) (Math.PI / 180.0));
						vec33 = new Vec3((double)(j * m - k * l), vec33.y, (double)(k * m + j * l));
						i = (float)vec33.lengthSqr();
						if (i <= 0.001F) {
							return;
						}
					}

					float j = (float)Mth.fastInvSqrt((double)i);
					Vec3 vec34 = vec33.scale((double)j);
					Vec3 vec35 = this.getForward();
					float m = (float)(vec35.x * vec34.x + vec35.z * vec34.z);
					if (!(m < -0.15F)) {
						CollisionContext collisionContext = CollisionContext.of(this);
						BlockPos blockPos = new BlockPos(this.x, this.getBoundingBox().maxY, this.z);
						BlockState blockState = this.level.getBlockState(blockPos);
						if (blockState.getCollisionShape(this.level, blockPos, collisionContext).isEmpty()) {
							blockPos = blockPos.above();
							BlockState blockState2 = this.level.getBlockState(blockPos);
							if (blockState2.getCollisionShape(this.level, blockPos, collisionContext).isEmpty()) {
								float n = 7.0F;
								float o = 1.2F;
								if (this.hasEffect(MobEffects.JUMP)) {
									o += (float)(this.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.75F;
								}

								float p = Math.max(h * 7.0F, 1.0F / j);
								Vec3 vec37 = vec32.add(vec34.scale((double)p));
								float q = this.getBbWidth();
								float r = this.getBbHeight();
								AABB aABB = new AABB(vec3, vec37.add(0.0, (double)r, 0.0)).inflate((double)q, 0.0, (double)q);
								Vec3 vec36 = vec3.add(0.0, 0.51F, 0.0);
								vec37 = vec37.add(0.0, 0.51F, 0.0);
								Vec3 vec38 = vec34.cross(new Vec3(0.0, 1.0, 0.0));
								Vec3 vec39 = vec38.scale((double)(q * 0.5F));
								Vec3 vec310 = vec36.subtract(vec39);
								Vec3 vec311 = vec37.subtract(vec39);
								Vec3 vec312 = vec36.add(vec39);
								Vec3 vec313 = vec37.add(vec39);
								Iterator<AABB> iterator = this.level
									.getCollisions(this, aABB, Collections.emptySet())
									.flatMap(voxelShapex -> voxelShapex.toAabbs().stream())
									.iterator();
								float s = Float.MIN_VALUE;

								while(iterator.hasNext()) {
									AABB aABB2 = (AABB)iterator.next();
									if (aABB2.intersects(vec310, vec311) || aABB2.intersects(vec312, vec313)) {
										s = (float)aABB2.maxY;
										Vec3 vec314 = aABB2.getCenter();
										BlockPos blockPos2 = new BlockPos(vec314);

										for(int t = 1; (float)t < o; ++t) {
											BlockPos blockPos3 = blockPos2.above(t);
											BlockState blockState3 = this.level.getBlockState(blockPos3);
											VoxelShape voxelShape;
											if (!(voxelShape = blockState3.getCollisionShape(this.level, blockPos3, collisionContext)).isEmpty()) {
												s = (float)voxelShape.max(Direction.Axis.Y) + (float)blockPos3.getY();
												if ((double)s - this.getBoundingBox().minY > (double)o) {
													return;
												}
											}

											if (t > 1) {
												blockPos = blockPos.above();
												BlockState blockState4 = this.level.getBlockState(blockPos);
												if (!blockState4.getCollisionShape(this.level, blockPos, collisionContext).isEmpty()) {
													return;
												}
											}
										}
										break;
									}
								}

								if (s != Float.MIN_VALUE) {
									float u = (float)((double)s - this.getBoundingBox().minY);
									if (!(u <= 0.5F) && !(u > o)) {
										this.autoJumpTime = 1;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean hasEnoughImpulseToStartSprinting() {
		double d = 0.8;
		return this.isUnderWater() ? this.input.hasForwardImpulse() : (double)this.input.forwardImpulse >= 0.8;
	}

	public float getWaterVision() {
		if (!this.isUnderLiquid(FluidTags.WATER)) {
			return 0.0F;
		} else {
			float f = 600.0F;
			float g = 100.0F;
			if ((float)this.waterVisionTime >= 600.0F) {
				return 1.0F;
			} else {
				float h = Mth.clamp((float)this.waterVisionTime / 100.0F, 0.0F, 1.0F);
				float i = (float)this.waterVisionTime < 100.0F ? 0.0F : Mth.clamp(((float)this.waterVisionTime - 100.0F) / 500.0F, 0.0F, 1.0F);
				return h * 0.6F + i * 0.39999998F;
			}
		}
	}

	@Override
	public boolean isUnderWater() {
		return this.wasUnderwater;
	}

	@Override
	protected boolean updateIsUnderwater() {
		boolean bl = this.wasUnderwater;
		boolean bl2 = super.updateIsUnderwater();
		if (this.isSpectator()) {
			return this.wasUnderwater;
		} else {
			if (!bl && bl2) {
				this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.AMBIENT, 1.0F, 1.0F, false);
				this.minecraft.getSoundManager().play(new UnderwaterAmbientSoundInstances.UnderwaterAmbientSoundInstance(this));
			}

			if (bl && !bl2) {
				this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.AMBIENT, 1.0F, 1.0F, false);
			}

			return this.wasUnderwater;
		}
	}
}

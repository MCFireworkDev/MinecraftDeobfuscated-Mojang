package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidHeadModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.dragon.DragonHeadModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public class SkullBlockRenderer extends BlockEntityRenderer<SkullBlockEntity> {
	public static SkullBlockRenderer instance;
	private static final Map<SkullBlock.Type, SkullModel> MODEL_BY_TYPE = Util.make(Maps.<SkullBlock.Type, SkullModel>newHashMap(), hashMap -> {
		SkullModel skullModel = new SkullModel(0, 0, 64, 32);
		SkullModel skullModel2 = new HumanoidHeadModel();
		DragonHeadModel dragonHeadModel = new DragonHeadModel(0.0F);
		hashMap.put(SkullBlock.Types.SKELETON, skullModel);
		hashMap.put(SkullBlock.Types.WITHER_SKELETON, skullModel);
		hashMap.put(SkullBlock.Types.PLAYER, skullModel2);
		hashMap.put(SkullBlock.Types.ZOMBIE, skullModel2);
		hashMap.put(SkullBlock.Types.CREEPER, skullModel);
		hashMap.put(SkullBlock.Types.DRAGON, dragonHeadModel);
	});
	private static final Map<SkullBlock.Type, ResourceLocation> SKIN_BY_TYPE = Util.make(Maps.<SkullBlock.Type, ResourceLocation>newHashMap(), hashMap -> {
		hashMap.put(SkullBlock.Types.SKELETON, new ResourceLocation("textures/entity/skeleton/skeleton.png"));
		hashMap.put(SkullBlock.Types.WITHER_SKELETON, new ResourceLocation("textures/entity/skeleton/wither_skeleton.png"));
		hashMap.put(SkullBlock.Types.ZOMBIE, new ResourceLocation("textures/entity/zombie/zombie.png"));
		hashMap.put(SkullBlock.Types.CREEPER, new ResourceLocation("textures/entity/creeper/creeper.png"));
		hashMap.put(SkullBlock.Types.DRAGON, new ResourceLocation("textures/entity/enderdragon/dragon.png"));
		hashMap.put(SkullBlock.Types.PLAYER, DefaultPlayerSkin.getDefaultSkin());
	});

	public void render(SkullBlockEntity skullBlockEntity, double d, double e, double f, float g, int i, RenderType renderType) {
		float h = skullBlockEntity.getMouthAnimation(g);
		BlockState blockState = skullBlockEntity.getBlockState();
		boolean bl = blockState.getBlock() instanceof WallSkullBlock;
		Direction direction = bl ? blockState.getValue(WallSkullBlock.FACING) : null;
		float j = 22.5F * (float)(bl ? (2 + direction.get2DDataValue()) * 4 : blockState.getValue(SkullBlock.ROTATION));
		this.renderSkull((float)d, (float)e, (float)f, direction, j, ((AbstractSkullBlock)blockState.getBlock()).getType(), skullBlockEntity.getOwnerProfile(), i, h);
	}

	@Override
	public void init(BlockEntityRenderDispatcher blockEntityRenderDispatcher) {
		super.init(blockEntityRenderDispatcher);
		instance = this;
	}

	public void renderSkull(
		float f, float g, float h, @Nullable Direction direction, float i, SkullBlock.Type type, @Nullable GameProfile gameProfile, int j, float k
	) {
		SkullModel skullModel = (SkullModel)MODEL_BY_TYPE.get(type);
		if (j >= 0) {
			this.bindTexture((ResourceLocation)BREAKING_LOCATIONS.get(j));
			RenderSystem.matrixMode(5890);
			RenderSystem.pushMatrix();
			RenderSystem.scalef(4.0F, 2.0F, 1.0F);
			RenderSystem.translatef(0.0625F, 0.0625F, 0.0625F);
			RenderSystem.matrixMode(5888);
		} else {
			this.bindTexture(this.getLocation(type, gameProfile));
		}

		RenderSystem.pushMatrix();
		if (direction == null) {
			RenderSystem.translatef(f + 0.5F, g, h + 0.5F);
		} else {
			switch(direction) {
				case NORTH:
					RenderSystem.translatef(f + 0.5F, g + 0.25F, h + 0.74F);
					break;
				case SOUTH:
					RenderSystem.translatef(f + 0.5F, g + 0.25F, h + 0.26F);
					break;
				case WEST:
					RenderSystem.translatef(f + 0.74F, g + 0.25F, h + 0.5F);
					break;
				case EAST:
				default:
					RenderSystem.translatef(f + 0.26F, g + 0.25F, h + 0.5F);
			}
		}

		RenderSystem.enableRescaleNormal();
		RenderSystem.scalef(-1.0F, -1.0F, 1.0F);
		RenderSystem.enableAlphaTest();
		if (type == SkullBlock.Types.PLAYER) {
			RenderSystem.setProfile(RenderSystem.Profile.PLAYER_SKIN);
		}

		skullModel.render(k, 0.0F, 0.0F, i, 0.0F, 0.0625F);
		RenderSystem.popMatrix();
		if (j >= 0) {
			RenderSystem.matrixMode(5890);
			RenderSystem.popMatrix();
			RenderSystem.matrixMode(5888);
		}
	}

	private ResourceLocation getLocation(SkullBlock.Type type, @Nullable GameProfile gameProfile) {
		ResourceLocation resourceLocation = (ResourceLocation)SKIN_BY_TYPE.get(type);
		if (type == SkullBlock.Types.PLAYER && gameProfile != null) {
			Minecraft minecraft = Minecraft.getInstance();
			Map<Type, MinecraftProfileTexture> map = minecraft.getSkinManager().getInsecureSkinInformation(gameProfile);
			if (map.containsKey(Type.SKIN)) {
				resourceLocation = minecraft.getSkinManager().registerTexture((MinecraftProfileTexture)map.get(Type.SKIN), Type.SKIN);
			} else {
				resourceLocation = DefaultPlayerSkin.getDefaultSkin(Player.createPlayerUUID(gameProfile));
			}
		}

		return resourceLocation;
	}
}

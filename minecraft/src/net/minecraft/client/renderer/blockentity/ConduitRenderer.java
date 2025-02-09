package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class ConduitRenderer implements BlockEntityRenderer<ConduitBlockEntity> {
	public static final Material SHELL_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/base"));
	public static final Material ACTIVE_SHELL_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/cage"));
	public static final Material WIND_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/wind"));
	public static final Material VERTICAL_WIND_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/wind_vertical"));
	public static final Material OPEN_EYE_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/open_eye"));
	public static final Material CLOSED_EYE_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/closed_eye"));
	private final ModelPart eye;
	private final ModelPart wind;
	private final ModelPart shell;
	private final ModelPart cage;
	private final BlockEntityRenderDispatcher renderer;

	public ConduitRenderer(BlockEntityRendererProvider.Context context) {
		this.renderer = context.getBlockEntityRenderDispatcher();
		this.eye = context.bakeLayer(ModelLayers.CONDUIT_EYE);
		this.wind = context.bakeLayer(ModelLayers.CONDUIT_WIND);
		this.shell = context.bakeLayer(ModelLayers.CONDUIT_SHELL);
		this.cage = context.bakeLayer(ModelLayers.CONDUIT_CAGE);
	}

	public static LayerDefinition createEyeLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild(
			"eye", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.01F)), PartPose.ZERO
		);
		return LayerDefinition.create(meshDefinition, 16, 16);
	}

	public static LayerDefinition createWindLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild("wind", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.ZERO);
		return LayerDefinition.create(meshDefinition, 64, 32);
	}

	public static LayerDefinition createShellLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
		return LayerDefinition.create(meshDefinition, 32, 16);
	}

	public static LayerDefinition createCageLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();
		partDefinition.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
		return LayerDefinition.create(meshDefinition, 32, 16);
	}

	public void render(ConduitBlockEntity conduitBlockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int j) {
		float g = (float)conduitBlockEntity.tickCount + f;
		if (!conduitBlockEntity.isActive()) {
			float h = conduitBlockEntity.getActiveRotation(0.0F);
			VertexConsumer vertexConsumer = SHELL_TEXTURE.buffer(multiBufferSource, RenderType::entitySolid);
			poseStack.pushPose();
			poseStack.translate(0.5F, 0.5F, 0.5F);
			poseStack.mulPose(new Quaternionf().rotationY(h * (float) (Math.PI / 180.0)));
			this.shell.render(poseStack, vertexConsumer, i, j);
			poseStack.popPose();
		} else {
			float h = conduitBlockEntity.getActiveRotation(f) * (180.0F / (float)Math.PI);
			float k = Mth.sin(g * 0.1F) / 2.0F + 0.5F;
			k = k * k + k;
			poseStack.pushPose();
			poseStack.translate(0.5F, 0.3F + k * 0.2F, 0.5F);
			Vector3f vector3f = new Vector3f(0.5F, 1.0F, 0.5F).normalize();
			poseStack.mulPose(new Quaternionf().rotationAxis(h * (float) (Math.PI / 180.0), vector3f));
			this.cage.render(poseStack, ACTIVE_SHELL_TEXTURE.buffer(multiBufferSource, RenderType::entityCutoutNoCull), i, j);
			poseStack.popPose();
			int l = conduitBlockEntity.tickCount / 66 % 3;
			poseStack.pushPose();
			poseStack.translate(0.5F, 0.5F, 0.5F);
			if (l == 1) {
				poseStack.mulPose(new Quaternionf().rotationX((float) (Math.PI / 2)));
			} else if (l == 2) {
				poseStack.mulPose(new Quaternionf().rotationZ((float) (Math.PI / 2)));
			}

			VertexConsumer vertexConsumer2 = (l == 1 ? VERTICAL_WIND_TEXTURE : WIND_TEXTURE).buffer(multiBufferSource, RenderType::entityCutoutNoCull);
			this.wind.render(poseStack, vertexConsumer2, i, j);
			poseStack.popPose();
			poseStack.pushPose();
			poseStack.translate(0.5F, 0.5F, 0.5F);
			poseStack.scale(0.875F, 0.875F, 0.875F);
			poseStack.mulPose(new Quaternionf().rotationXYZ((float) Math.PI, 0.0F, (float) Math.PI));
			this.wind.render(poseStack, vertexConsumer2, i, j);
			poseStack.popPose();
			Camera camera = this.renderer.camera;
			poseStack.pushPose();
			poseStack.translate(0.5F, 0.3F + k * 0.2F, 0.5F);
			poseStack.scale(0.5F, 0.5F, 0.5F);
			float m = -camera.getYRot();
			poseStack.mulPose(new Quaternionf().rotationYXZ(m * (float) (Math.PI / 180.0), camera.getXRot() * (float) (Math.PI / 180.0), (float) Math.PI));
			float n = 1.3333334F;
			poseStack.scale(1.3333334F, 1.3333334F, 1.3333334F);
			this.eye
				.render(
					poseStack, (conduitBlockEntity.isHunting() ? OPEN_EYE_TEXTURE : CLOSED_EYE_TEXTURE).buffer(multiBufferSource, RenderType::entityCutoutNoCull), i, j
				);
			poseStack.popPose();
		}
	}
}

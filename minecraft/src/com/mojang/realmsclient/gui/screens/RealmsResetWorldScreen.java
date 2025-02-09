package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.WorldGenerationInfo;
import com.mojang.realmsclient.util.task.LongRunningTask;
import com.mojang.realmsclient.util.task.ResettingGeneratedWorldTask;
import com.mojang.realmsclient.util.task.ResettingTemplateWorldTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import com.mojang.realmsclient.util.task.WorldCreationTask;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsResetWorldScreen extends RealmsScreen {
	static final Logger LOGGER = LogUtils.getLogger();
	static final ResourceLocation SLOT_FRAME_SPRITE = new ResourceLocation("widget/slot_frame");
	private static final Component CREATE_REALM_TITLE = Component.translatable("mco.selectServer.create");
	private static final Component CREATE_REALM_SUBTITLE = Component.translatable("mco.selectServer.create.subtitle");
	private static final Component CREATE_WORLD_TITLE = Component.translatable("mco.configure.world.switch.slot");
	private static final Component CREATE_WORLD_SUBTITLE = Component.translatable("mco.configure.world.switch.slot.subtitle");
	private static final Component RESET_WORLD_TITLE = Component.translatable("mco.reset.world.title");
	private static final Component RESET_WORLD_SUBTITLE = Component.translatable("mco.reset.world.warning");
	public static final Component CREATE_WORLD_RESET_TASK_TITLE = Component.translatable("mco.create.world.reset.title");
	private static final Component RESET_WORLD_RESET_TASK_TITLE = Component.translatable("mco.reset.world.resetting.screen.title");
	private static final Component WORLD_TEMPLATES_TITLE = Component.translatable("mco.reset.world.template");
	private static final Component ADVENTURES_TITLE = Component.translatable("mco.reset.world.adventure");
	private static final Component EXPERIENCES_TITLE = Component.translatable("mco.reset.world.experience");
	private static final Component INSPIRATION_TITLE = Component.translatable("mco.reset.world.inspiration");
	private final Screen lastScreen;
	private final RealmsServer serverData;
	private final Component subtitle;
	private final int subtitleColor;
	private final Component resetTaskTitle;
	private static final ResourceLocation UPLOAD_LOCATION = new ResourceLocation("textures/gui/realms/upload.png");
	private static final ResourceLocation ADVENTURE_MAP_LOCATION = new ResourceLocation("textures/gui/realms/adventure.png");
	private static final ResourceLocation SURVIVAL_SPAWN_LOCATION = new ResourceLocation("textures/gui/realms/survival_spawn.png");
	private static final ResourceLocation NEW_WORLD_LOCATION = new ResourceLocation("textures/gui/realms/new_world.png");
	private static final ResourceLocation EXPERIENCE_LOCATION = new ResourceLocation("textures/gui/realms/experience.png");
	private static final ResourceLocation INSPIRATION_LOCATION = new ResourceLocation("textures/gui/realms/inspiration.png");
	WorldTemplatePaginatedList templates;
	WorldTemplatePaginatedList adventuremaps;
	WorldTemplatePaginatedList experiences;
	WorldTemplatePaginatedList inspirations;
	public final int slot;
	@Nullable
	private final WorldCreationTask worldCreationTask;
	private final Runnable resetWorldRunnable;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

	private RealmsResetWorldScreen(
		Screen screen, RealmsServer realmsServer, int i, Component component, Component component2, int j, Component component3, Runnable runnable
	) {
		this(screen, realmsServer, i, component, component2, j, component3, null, runnable);
	}

	public RealmsResetWorldScreen(
		Screen screen,
		RealmsServer realmsServer,
		int i,
		Component component,
		Component component2,
		int j,
		Component component3,
		@Nullable WorldCreationTask worldCreationTask,
		Runnable runnable
	) {
		super(component);
		this.lastScreen = screen;
		this.serverData = realmsServer;
		this.slot = i;
		this.subtitle = component2;
		this.subtitleColor = j;
		this.resetTaskTitle = component3;
		this.worldCreationTask = worldCreationTask;
		this.resetWorldRunnable = runnable;
	}

	public static RealmsResetWorldScreen forNewRealm(Screen screen, RealmsServer realmsServer, WorldCreationTask worldCreationTask, Runnable runnable) {
		return new RealmsResetWorldScreen(
			screen,
			realmsServer,
			realmsServer.activeSlot,
			CREATE_REALM_TITLE,
			CREATE_REALM_SUBTITLE,
			-6250336,
			CREATE_WORLD_RESET_TASK_TITLE,
			worldCreationTask,
			runnable
		);
	}

	public static RealmsResetWorldScreen forEmptySlot(Screen screen, int i, RealmsServer realmsServer, Runnable runnable) {
		return new RealmsResetWorldScreen(screen, realmsServer, i, CREATE_WORLD_TITLE, CREATE_WORLD_SUBTITLE, -6250336, CREATE_WORLD_RESET_TASK_TITLE, runnable);
	}

	public static RealmsResetWorldScreen forResetSlot(Screen screen, RealmsServer realmsServer, Runnable runnable) {
		return new RealmsResetWorldScreen(
			screen, realmsServer, realmsServer.activeSlot, RESET_WORLD_TITLE, RESET_WORLD_SUBTITLE, -65536, RESET_WORLD_RESET_TASK_TITLE, runnable
		);
	}

	@Override
	public void init() {
		LinearLayout linearLayout = LinearLayout.vertical();
		linearLayout.addChild(new StringWidget(this.title, this.font), LayoutSettings::alignHorizontallyCenter);
		linearLayout.addChild(SpacerElement.height(3));
		linearLayout.addChild(new StringWidget(this.subtitle, this.font).setColor(this.subtitleColor), LayoutSettings::alignHorizontallyCenter);
		this.layout.addToHeader(linearLayout);
		(new Thread("Realms-reset-world-fetcher") {
			public void run() {
				RealmsClient realmsClient = RealmsClient.create();

				try {
					WorldTemplatePaginatedList worldTemplatePaginatedList = realmsClient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.NORMAL);
					WorldTemplatePaginatedList worldTemplatePaginatedList2 = realmsClient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.ADVENTUREMAP);
					WorldTemplatePaginatedList worldTemplatePaginatedList3 = realmsClient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.EXPERIENCE);
					WorldTemplatePaginatedList worldTemplatePaginatedList4 = realmsClient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.INSPIRATION);
					RealmsResetWorldScreen.this.minecraft.execute(() -> {
						RealmsResetWorldScreen.this.templates = worldTemplatePaginatedList;
						RealmsResetWorldScreen.this.adventuremaps = worldTemplatePaginatedList2;
						RealmsResetWorldScreen.this.experiences = worldTemplatePaginatedList3;
						RealmsResetWorldScreen.this.inspirations = worldTemplatePaginatedList4;
					});
				} catch (RealmsServiceException var6) {
					RealmsResetWorldScreen.LOGGER.error("Couldn't fetch templates in reset world", var6);
				}
			}
		}).start();
		this.addRenderableWidget(
			new RealmsResetWorldScreen.FrameButton(
				this.frame(1),
				row(0) + 10,
				RealmsResetNormalWorldScreen.TITLE,
				NEW_WORLD_LOCATION,
				button -> this.minecraft.setScreen(new RealmsResetNormalWorldScreen(this::generationSelectionCallback, this.title))
			)
		);
		this.addRenderableWidget(
			new RealmsResetWorldScreen.FrameButton(
				this.frame(2),
				row(0) + 10,
				RealmsSelectFileToUploadScreen.TITLE,
				UPLOAD_LOCATION,
				button -> this.minecraft.setScreen(new RealmsSelectFileToUploadScreen(this.serverData.id, this.slot, this))
			)
		);
		this.addRenderableWidget(
			new RealmsResetWorldScreen.FrameButton(
				this.frame(3),
				row(0) + 10,
				WORLD_TEMPLATES_TITLE,
				SURVIVAL_SPAWN_LOCATION,
				button -> this.minecraft
						.setScreen(new RealmsSelectWorldTemplateScreen(WORLD_TEMPLATES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.NORMAL, this.templates))
			)
		);
		this.addRenderableWidget(
			new RealmsResetWorldScreen.FrameButton(
				this.frame(1),
				row(6) + 20,
				ADVENTURES_TITLE,
				ADVENTURE_MAP_LOCATION,
				button -> this.minecraft
						.setScreen(
							new RealmsSelectWorldTemplateScreen(ADVENTURES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.ADVENTUREMAP, this.adventuremaps)
						)
			)
		);
		this.addRenderableWidget(
			new RealmsResetWorldScreen.FrameButton(
				this.frame(2),
				row(6) + 20,
				EXPERIENCES_TITLE,
				EXPERIENCE_LOCATION,
				button -> this.minecraft
						.setScreen(new RealmsSelectWorldTemplateScreen(EXPERIENCES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.EXPERIENCE, this.experiences))
			)
		);
		this.addRenderableWidget(
			new RealmsResetWorldScreen.FrameButton(
				this.frame(3),
				row(6) + 20,
				INSPIRATION_TITLE,
				INSPIRATION_LOCATION,
				button -> this.minecraft
						.setScreen(
							new RealmsSelectWorldTemplateScreen(INSPIRATION_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.INSPIRATION, this.inspirations)
						)
			)
		);
		this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
		this.layout.visitWidgets(guiEventListener -> {
		});
		this.layout.arrangeElements();
	}

	@Override
	public Component getNarrationMessage() {
		return CommonComponents.joinForNarration(this.getTitle(), this.subtitle);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.lastScreen);
	}

	private int frame(int i) {
		return this.width / 2 - 130 + (i - 1) * 100;
	}

	private void templateSelectionCallback(@Nullable WorldTemplate worldTemplate) {
		this.minecraft.setScreen(this);
		if (worldTemplate != null) {
			this.runResetTasks(new ResettingTemplateWorldTask(worldTemplate, this.serverData.id, this.resetTaskTitle, this.resetWorldRunnable));
		}
	}

	private void generationSelectionCallback(@Nullable WorldGenerationInfo worldGenerationInfo) {
		this.minecraft.setScreen(this);
		if (worldGenerationInfo != null) {
			this.runResetTasks(new ResettingGeneratedWorldTask(worldGenerationInfo, this.serverData.id, this.resetTaskTitle, this.resetWorldRunnable));
		}
	}

	private void runResetTasks(LongRunningTask longRunningTask) {
		List<LongRunningTask> list = new ArrayList();
		if (this.worldCreationTask != null) {
			list.add(this.worldCreationTask);
		}

		if (this.slot != this.serverData.activeSlot) {
			list.add(new SwitchSlotTask(this.serverData.id, this.slot, () -> {
			}));
		}

		list.add(longRunningTask);
		this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, (LongRunningTask[])list.toArray(new LongRunningTask[0])));
	}

	public void switchSlot(Runnable runnable) {
		this.minecraft
			.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(this.serverData.id, this.slot, () -> this.minecraft.execute(runnable))));
	}

	@Environment(EnvType.CLIENT)
	class FrameButton extends Button {
		private static final int WIDTH = 60;
		private static final int HEIGHT = 72;
		private static final int IMAGE_SIZE = 56;
		private final ResourceLocation image;

		FrameButton(int i, int j, Component component, ResourceLocation resourceLocation, Button.OnPress onPress) {
			super(i, j, 60, 72, component, onPress, DEFAULT_NARRATION);
			this.image = resourceLocation;
		}

		@Override
		public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
			boolean bl = this.isHoveredOrFocused();
			if (bl) {
				guiGraphics.setColor(0.56F, 0.56F, 0.56F, 1.0F);
			}

			int k = this.getX();
			int l = this.getY();
			guiGraphics.blit(this.image, k + 2, l + 14, 0.0F, 0.0F, 56, 56, 56, 56);
			guiGraphics.blitSprite(RealmsResetWorldScreen.SLOT_FRAME_SPRITE, k, l + 12, 60, 60);
			guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			int m = bl ? -6250336 : -1;
			guiGraphics.drawCenteredString(RealmsResetWorldScreen.this.font, this.getMessage(), k + 30, l, m);
		}
	}
}

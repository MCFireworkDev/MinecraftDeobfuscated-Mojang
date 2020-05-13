package net.minecraft.client.gui.screens;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldGenSettings;

@Environment(EnvType.CLIENT)
public class CreateBuffetWorldScreen extends Screen {
	private static final WorldGenSettings.BuffetGeneratorType[] TYPES = WorldGenSettings.BuffetGeneratorType.values();
	private final Screen parent;
	private final Consumer<Pair<WorldGenSettings.BuffetGeneratorType, Set<Biome>>> applySettings;
	private CreateBuffetWorldScreen.BiomeList list;
	private int generatorIndex;
	private Button doneButton;

	public CreateBuffetWorldScreen(
		Screen screen, Consumer<Pair<WorldGenSettings.BuffetGeneratorType, Set<Biome>>> consumer, Pair<WorldGenSettings.BuffetGeneratorType, Set<Biome>> pair
	) {
		super(new TranslatableComponent("createWorld.customize.buffet.title"));
		this.parent = screen;
		this.applySettings = consumer;

		for(int i = 0; i < TYPES.length; ++i) {
			if (TYPES[i].equals(pair.getFirst())) {
				this.generatorIndex = i;
				break;
			}
		}

		for(Biome biome : (Set)pair.getSecond()) {
			this.list
				.setSelected(
					(CreateBuffetWorldScreen.BiomeList.Entry)this.list.children().stream().filter(entry -> Objects.equals(entry.biome, biome)).findFirst().orElse(null)
				);
		}
	}

	@Override
	protected void init() {
		this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
		this.addButton(new Button((this.width - 200) / 2, 40, 200, 20, TYPES[this.generatorIndex].createGeneratorString(), button -> {
			++this.generatorIndex;
			if (this.generatorIndex >= TYPES.length) {
				this.generatorIndex = 0;
			}

			button.setMessage(TYPES[this.generatorIndex].createGeneratorString());
		}));
		this.list = new CreateBuffetWorldScreen.BiomeList();
		this.children.add(this.list);
		this.doneButton = this.addButton(new Button(this.width / 2 - 155, this.height - 28, 150, 20, CommonComponents.GUI_DONE, button -> {
			this.applySettings.accept(Pair.of(TYPES[this.generatorIndex], ImmutableSet.<Biome>of(this.list.getSelected().biome)));
			this.minecraft.setScreen(this.parent);
		}));
		this.addButton(new Button(this.width / 2 + 5, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen(this.parent)));
		this.updateButtonValidity();
	}

	public void updateButtonValidity() {
		this.doneButton.active = this.list.getSelected() != null;
	}

	@Override
	public void render(PoseStack poseStack, int i, int j, float f) {
		this.renderDirtBackground(0);
		this.list.render(poseStack, i, j, f);
		this.drawCenteredString(poseStack, this.font, this.title, this.width / 2, 8, 16777215);
		this.drawCenteredString(poseStack, this.font, I18n.get("createWorld.customize.buffet.generator"), this.width / 2, 30, 10526880);
		this.drawCenteredString(poseStack, this.font, I18n.get("createWorld.customize.buffet.biome"), this.width / 2, 68, 10526880);
		super.render(poseStack, i, j, f);
	}

	@Environment(EnvType.CLIENT)
	class BiomeList extends ObjectSelectionList<CreateBuffetWorldScreen.BiomeList.Entry> {
		private BiomeList() {
			super(
				CreateBuffetWorldScreen.this.minecraft,
				CreateBuffetWorldScreen.this.width,
				CreateBuffetWorldScreen.this.height,
				80,
				CreateBuffetWorldScreen.this.height - 37,
				16
			);
			Registry.BIOME
				.stream()
				.sorted(Comparator.comparing(biome -> biome.getName().getString()))
				.forEach(biome -> this.addEntry(new CreateBuffetWorldScreen.BiomeList.Entry(biome)));
		}

		@Override
		protected boolean isFocused() {
			return CreateBuffetWorldScreen.this.getFocused() == this;
		}

		public void setSelected(@Nullable CreateBuffetWorldScreen.BiomeList.Entry entry) {
			super.setSelected(entry);
			if (entry != null) {
				NarratorChatListener.INSTANCE.sayNow(new TranslatableComponent("narrator.select", entry.biome.getName().getString()).getString());
			}
		}

		@Override
		protected void moveSelection(int i) {
			super.moveSelection(i);
			CreateBuffetWorldScreen.this.updateButtonValidity();
		}

		@Environment(EnvType.CLIENT)
		class Entry extends ObjectSelectionList.Entry<CreateBuffetWorldScreen.BiomeList.Entry> {
			private final Biome biome;

			public Entry(Biome biome) {
				this.biome = biome;
			}

			@Override
			public void render(PoseStack poseStack, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
				BiomeList.this.drawString(poseStack, CreateBuffetWorldScreen.this.font, this.biome.getName().getString(), k + 5, j + 2, 16777215);
			}

			@Override
			public boolean mouseClicked(double d, double e, int i) {
				if (i == 0) {
					BiomeList.this.setSelected(this);
					CreateBuffetWorldScreen.this.updateButtonValidity();
					return true;
				} else {
					return false;
				}
			}
		}
	}
}

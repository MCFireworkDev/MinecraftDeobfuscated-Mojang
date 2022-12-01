package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameRules;

@Environment(EnvType.CLIENT)
public class EditGameRulesScreen extends Screen {
	private final Consumer<Optional<GameRules>> exitCallback;
	private EditGameRulesScreen.RuleList rules;
	private final Set<EditGameRulesScreen.RuleEntry> invalidEntries = Sets.<EditGameRulesScreen.RuleEntry>newHashSet();
	private Button doneButton;
	@Nullable
	private List<FormattedCharSequence> tooltip;
	private final GameRules gameRules;

	public EditGameRulesScreen(GameRules gameRules, Consumer<Optional<GameRules>> consumer) {
		super(Component.translatable("editGamerule.title"));
		this.gameRules = gameRules;
		this.exitCallback = consumer;
	}

	@Override
	protected void init() {
		this.rules = new EditGameRulesScreen.RuleList(this.gameRules);
		this.addWidget(this.rules);
		this.doneButton = this.addRenderableWidget(
			Button.builder(CommonComponents.GUI_DONE, button -> this.exitCallback.accept(Optional.of(this.gameRules)))
				.bounds(this.width / 2 - 155, this.height - 29, 150, 20)
				.build()
		);
		this.addRenderableWidget(
			Button.builder(CommonComponents.GUI_CANCEL, button -> this.exitCallback.accept(Optional.empty()))
				.bounds(this.width / 2 - 155 + 160, this.height - 29, 150, 20)
				.build()
		);
	}

	@Override
	public void onClose() {
		this.exitCallback.accept(Optional.empty());
	}

	@Override
	public void render(PoseStack poseStack, int i, int j, float f) {
		this.tooltip = null;
		this.rules.render(poseStack, i, j, f);
		drawCenteredString(poseStack, this.font, this.title, this.width / 2, 20, 16777215);
		super.render(poseStack, i, j, f);
	}

	private void updateDoneButton() {
		this.doneButton.active = this.invalidEntries.isEmpty();
	}

	void markInvalid(EditGameRulesScreen.RuleEntry ruleEntry) {
		this.invalidEntries.add(ruleEntry);
		this.updateDoneButton();
	}

	void clearInvalid(EditGameRulesScreen.RuleEntry ruleEntry) {
		this.invalidEntries.remove(ruleEntry);
		this.updateDoneButton();
	}

	@Environment(EnvType.CLIENT)
	public class BooleanRuleEntry extends EditGameRulesScreen.GameRuleEntry {
		private final CycleButton<Boolean> checkbox;

		public BooleanRuleEntry(Component component, List<FormattedCharSequence> list, String string, GameRules.BooleanValue booleanValue) {
			super(list, component);
			this.checkbox = CycleButton.onOffBuilder(booleanValue.get())
				.displayOnlyValue()
				.withCustomNarration(cycleButton -> cycleButton.createDefaultNarrationMessage().append("\n").append(string))
				.create(10, 5, 44, 20, component, (cycleButton, boolean_) -> booleanValue.set(boolean_, null));
			this.children.add(this.checkbox);
		}

		@Override
		public void render(PoseStack poseStack, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
			this.renderLabel(poseStack, j, k);
			this.checkbox.setX(k + l - 45);
			this.checkbox.setY(j);
			this.checkbox.render(poseStack, n, o, f);
		}
	}

	@Environment(EnvType.CLIENT)
	public class CategoryRuleEntry extends EditGameRulesScreen.RuleEntry {
		final Component label;

		public CategoryRuleEntry(Component component) {
			super(null);
			this.label = component;
		}

		@Override
		public void render(PoseStack poseStack, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
			GuiComponent.drawCenteredString(poseStack, EditGameRulesScreen.this.minecraft.font, this.label, k + l / 2, j + 5, 16777215);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return ImmutableList.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return ImmutableList.of(new NarratableEntry() {
				@Override
				public NarratableEntry.NarrationPriority narrationPriority() {
					return NarratableEntry.NarrationPriority.HOVERED;
				}

				@Override
				public void updateNarration(NarrationElementOutput narrationElementOutput) {
					narrationElementOutput.add(NarratedElementType.TITLE, CategoryRuleEntry.this.label);
				}
			});
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	interface EntryFactory<T extends GameRules.Value<T>> {
		EditGameRulesScreen.RuleEntry create(Component component, List<FormattedCharSequence> list, String string, T value);
	}

	@Environment(EnvType.CLIENT)
	public abstract class GameRuleEntry extends EditGameRulesScreen.RuleEntry {
		private final List<FormattedCharSequence> label;
		protected final List<AbstractWidget> children = Lists.<AbstractWidget>newArrayList();

		public GameRuleEntry(@Nullable List<FormattedCharSequence> list, Component component) {
			super(list);
			this.label = EditGameRulesScreen.this.minecraft.font.split(component, 175);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return this.children;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return this.children;
		}

		protected void renderLabel(PoseStack poseStack, int i, int j) {
			if (this.label.size() == 1) {
				EditGameRulesScreen.this.minecraft.font.draw(poseStack, (FormattedCharSequence)this.label.get(0), (float)j, (float)(i + 5), 16777215);
			} else if (this.label.size() >= 2) {
				EditGameRulesScreen.this.minecraft.font.draw(poseStack, (FormattedCharSequence)this.label.get(0), (float)j, (float)i, 16777215);
				EditGameRulesScreen.this.minecraft.font.draw(poseStack, (FormattedCharSequence)this.label.get(1), (float)j, (float)(i + 10), 16777215);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public class IntegerRuleEntry extends EditGameRulesScreen.GameRuleEntry {
		private final EditBox input;

		public IntegerRuleEntry(Component component, List<FormattedCharSequence> list, String string, GameRules.IntegerValue integerValue) {
			super(list, component);
			this.input = new EditBox(EditGameRulesScreen.this.minecraft.font, 10, 5, 42, 20, component.copy().append("\n").append(string).append("\n"));
			this.input.setValue(Integer.toString(integerValue.get()));
			this.input.setResponder(stringx -> {
				if (integerValue.tryDeserialize(stringx)) {
					this.input.setTextColor(14737632);
					EditGameRulesScreen.this.clearInvalid(this);
				} else {
					this.input.setTextColor(16711680);
					EditGameRulesScreen.this.markInvalid(this);
				}
			});
			this.children.add(this.input);
		}

		@Override
		public void render(PoseStack poseStack, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
			this.renderLabel(poseStack, j, k);
			this.input.setX(k + l - 44);
			this.input.setY(j);
			this.input.render(poseStack, n, o, f);
		}
	}

	@Environment(EnvType.CLIENT)
	public abstract static class RuleEntry extends ContainerObjectSelectionList.Entry<EditGameRulesScreen.RuleEntry> {
		@Nullable
		final List<FormattedCharSequence> tooltip;

		public RuleEntry(@Nullable List<FormattedCharSequence> list) {
			this.tooltip = list;
		}
	}

	@Environment(EnvType.CLIENT)
	public class RuleList extends ContainerObjectSelectionList<EditGameRulesScreen.RuleEntry> {
		public RuleList(GameRules gameRules) {
			super(EditGameRulesScreen.this.minecraft, EditGameRulesScreen.this.width, EditGameRulesScreen.this.height, 43, EditGameRulesScreen.this.height - 32, 24);
			final Map<GameRules.Category, Map<GameRules.Key<?>, EditGameRulesScreen.RuleEntry>> map = Maps.newHashMap();
			GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
				@Override
				public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
					this.addEntry(key, (component, list, string, booleanValue) -> EditGameRulesScreen.this.new BooleanRuleEntry(component, list, string, booleanValue));
				}

				@Override
				public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
					this.addEntry(key, (component, list, string, integerValue) -> EditGameRulesScreen.this.new IntegerRuleEntry(component, list, string, integerValue));
				}

				private <T extends GameRules.Value<T>> void addEntry(GameRules.Key<T> key, EditGameRulesScreen.EntryFactory<T> entryFactory) {
					Component component = Component.translatable(key.getDescriptionId());
					Component component2 = Component.literal(key.getId()).withStyle(ChatFormatting.YELLOW);
					T value = gameRules.getRule(key);
					String string = value.serialize();
					Component component3 = Component.translatable("editGamerule.default", Component.literal(string)).withStyle(ChatFormatting.GRAY);
					String string2 = key.getDescriptionId() + ".description";
					List<FormattedCharSequence> list;
					String string3;
					if (I18n.exists(string2)) {
						Builder<FormattedCharSequence> builder = ImmutableList.<FormattedCharSequence>builder().add(component2.getVisualOrderText());
						Component component4 = Component.translatable(string2);
						EditGameRulesScreen.this.font.split(component4, 150).forEach(builder::add);
						list = builder.add(component3.getVisualOrderText()).build();
						string3 = component4.getString() + "\n" + component3.getString();
					} else {
						list = ImmutableList.of(component2.getVisualOrderText(), component3.getVisualOrderText());
						string3 = component3.getString();
					}

					((Map)map.computeIfAbsent(key.getCategory(), category -> Maps.newHashMap())).put(key, entryFactory.create(component, list, string3, value));
				}
			});
			map.entrySet()
				.stream()
				.sorted(java.util.Map.Entry.comparingByKey())
				.forEach(
					entry -> {
						this.addEntry(
							EditGameRulesScreen.this.new CategoryRuleEntry(
								Component.translatable(((GameRules.Category)entry.getKey()).getDescriptionId()).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)
							)
						);
						((Map)entry.getValue())
							.entrySet()
							.stream()
							.sorted(java.util.Map.Entry.comparingByKey(Comparator.comparing(GameRules.Key::getId)))
							.forEach(entryx -> this.addEntry((EditGameRulesScreen.RuleEntry)entryx.getValue()));
					}
				);
		}

		@Override
		public void render(PoseStack poseStack, int i, int j, float f) {
			super.render(poseStack, i, j, f);
			EditGameRulesScreen.RuleEntry ruleEntry = this.getHovered();
			if (ruleEntry != null && ruleEntry.tooltip != null) {
				EditGameRulesScreen.this.setTooltipForNextRenderPass(ruleEntry.tooltip);
			}
		}
	}
}

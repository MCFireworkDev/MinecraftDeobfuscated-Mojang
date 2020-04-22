package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
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
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.GameRules;

@Environment(EnvType.CLIENT)
public class EditGameRulesScreen extends Screen {
	private final Consumer<Optional<GameRules>> exitCallback;
	private EditGameRulesScreen.RuleList rules;
	private final Set<EditGameRulesScreen.RuleEntry> invalidEntries = Sets.<EditGameRulesScreen.RuleEntry>newHashSet();
	private Button doneButton;
	@Nullable
	private List<Component> tooltip;
	private final GameRules gameRules;

	public EditGameRulesScreen(GameRules gameRules, Consumer<Optional<GameRules>> consumer) {
		super(new TranslatableComponent("editGamerule.title"));
		this.gameRules = gameRules;
		this.exitCallback = consumer;
	}

	@Override
	protected void init() {
		this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
		super.init();
		this.rules = new EditGameRulesScreen.RuleList(this.gameRules);
		this.children.add(this.rules);
		this.addButton(
			new Button(this.width / 2 - 155 + 160, this.height - 29, 150, 20, CommonComponents.GUI_CANCEL, button -> this.exitCallback.accept(Optional.empty()))
		);
		this.doneButton = this.addButton(
			new Button(this.width / 2 - 155, this.height - 29, 150, 20, CommonComponents.GUI_DONE, button -> this.exitCallback.accept(Optional.of(this.gameRules)))
		);
	}

	@Override
	public void removed() {
		this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
	}

	@Override
	public void onClose() {
		this.exitCallback.accept(Optional.empty());
	}

	@Override
	public void render(PoseStack poseStack, int i, int j, float f) {
		this.tooltip = null;
		this.rules.render(poseStack, i, j, f);
		super.render(poseStack, i, j, f);
		if (this.tooltip != null) {
			this.renderTooltip(poseStack, this.tooltip, i, j);
		}
	}

	private void setTooltip(@Nullable List<Component> list) {
		this.tooltip = list;
	}

	private void updateDoneButton() {
		this.doneButton.active = this.invalidEntries.isEmpty();
	}

	private void markInvalid(EditGameRulesScreen.RuleEntry ruleEntry) {
		this.invalidEntries.add(ruleEntry);
		this.updateDoneButton();
	}

	private void clearInvalid(EditGameRulesScreen.RuleEntry ruleEntry) {
		this.invalidEntries.remove(ruleEntry);
		this.updateDoneButton();
	}

	@Environment(EnvType.CLIENT)
	public class BooleanRuleEntry extends EditGameRulesScreen.RuleEntry {
		private final Button checkbox;
		private final List<? extends GuiEventListener> children;

		public BooleanRuleEntry(Component component, List<Component> list, String string, GameRules.BooleanValue booleanValue) {
			super(list);
			this.checkbox = new Button(10, 5, 220, 20, this.getMessage(component, booleanValue.get()), button -> {
				boolean bl = !booleanValue.get();
				booleanValue.set(bl, null);
				button.setMessage(this.getMessage(component, bl));
			}) {
				@Override
				protected MutableComponent createNarrationMessage() {
					return this.getMessage().mutableCopy().append("\n").append(string);
				}
			};
			this.children = ImmutableList.of(this.checkbox);
		}

		private Component getMessage(Component component, boolean bl) {
			return component.mutableCopy().append(": ").append(CommonComponents.optionStatus(bl));
		}

		@Override
		public void render(PoseStack poseStack, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
			this.checkbox.x = k;
			this.checkbox.y = j;
			this.checkbox.render(poseStack, n, o, f);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return this.children;
		}
	}

	@Environment(EnvType.CLIENT)
	public class CategoryRuleEntry extends EditGameRulesScreen.RuleEntry {
		private final Component label;

		public CategoryRuleEntry(Component component) {
			super(null);
			this.label = component;
		}

		@Override
		public void render(PoseStack poseStack, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
			EditGameRulesScreen.this.drawCenteredString(poseStack, EditGameRulesScreen.this.minecraft.font, this.label, k + l / 2, j + 5, 16777215);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return ImmutableList.of();
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	interface EntryFactory<T extends GameRules.Value<T>> {
		EditGameRulesScreen.RuleEntry create(Component component, List<Component> list, String string, T value);
	}

	@Environment(EnvType.CLIENT)
	public class IntegerRuleEntry extends EditGameRulesScreen.RuleEntry {
		private final Component label;
		private final EditBox input;
		private final List<? extends GuiEventListener> children;

		public IntegerRuleEntry(Component component, List<Component> list, String string, GameRules.IntegerValue integerValue) {
			super(list);
			this.label = component;
			this.input = new EditBox(EditGameRulesScreen.this.minecraft.font, 10, 5, 42, 20, component.mutableCopy().append("\n").append(string).append("\n"));
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
			this.children = ImmutableList.of(this.input);
		}

		@Override
		public void render(PoseStack poseStack, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
			EditGameRulesScreen.this.minecraft.font.draw(poseStack, this.label, (float)k, (float)(j + 5), 16777215);
			this.input.x = k + l - 44;
			this.input.y = j;
			this.input.render(poseStack, n, o, f);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return this.children;
		}
	}

	@Environment(EnvType.CLIENT)
	public abstract class RuleEntry extends ContainerObjectSelectionList.Entry<EditGameRulesScreen.RuleEntry> {
		@Nullable
		private final List<Component> tooltip;

		public RuleEntry(@Nullable List<Component> list) {
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
					Component component = new TranslatableComponent(key.getDescriptionId());
					Component component2 = new TextComponent(key.getId()).withStyle(ChatFormatting.YELLOW);
					T value = gameRules.getRule(key);
					String string = value.serialize();
					Component component3 = new TranslatableComponent("editGamerule.default", new TextComponent(string)).withStyle(ChatFormatting.GRAY);
					String string2 = key.getDescriptionId() + ".description";
					List<Component> list;
					String string3;
					if (I18n.exists(string2)) {
						Builder<Component> builder = ImmutableList.<Component>builder().add(component2);
						Component component4 = new TranslatableComponent(string2);
						EditGameRulesScreen.this.font.split(component4, 150).forEach(builder::add);
						list = builder.add(component3).build();
						string3 = component4.getString() + "\n" + component3.getString();
					} else {
						list = ImmutableList.of(component2, component3);
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
								new TranslatableComponent(((GameRules.Category)entry.getKey()).getDescriptionId())
									.withStyle(new ChatFormatting[]{ChatFormatting.BOLD, ChatFormatting.YELLOW})
							)
						);
						((Map)entry.getValue())
							.entrySet()
							.stream()
							.sorted(java.util.Map.Entry.comparingByKey(Comparator.comparing(GameRules.Key::getId)))
							.forEach(entryx -> this.addEntry((AbstractSelectionList.Entry)entryx.getValue()));
					}
				);
		}

		@Override
		public void render(PoseStack poseStack, int i, int j, float f) {
			super.render(poseStack, i, j, f);
			if (this.isMouseOver((double)i, (double)j)) {
				EditGameRulesScreen.RuleEntry ruleEntry = this.getEntryAtPosition((double)i, (double)j);
				if (ruleEntry != null) {
					EditGameRulesScreen.this.setTooltip(ruleEntry.tooltip);
				}
			}
		}
	}
}

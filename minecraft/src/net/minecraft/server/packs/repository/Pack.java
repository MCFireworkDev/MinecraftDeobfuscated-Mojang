package net.minecraft.server.packs.repository;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import org.slf4j.Logger;

public class Pack {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final String id;
	private final Supplier<PackResources> supplier;
	private final Component title;
	private final Component description;
	private final PackCompatibility compatibility;
	private final Pack.Position defaultPosition;
	private final boolean required;
	private final boolean fixedPosition;
	private final PackSource packSource;

	@Nullable
	public static Pack create(
		String string, boolean bl, Supplier<PackResources> supplier, Pack.PackConstructor packConstructor, Pack.Position position, PackSource packSource
	) {
		try {
			Pack var8;
			try (PackResources packResources = (PackResources)supplier.get()) {
				PackMetadataSection packMetadataSection = packResources.getMetadataSection(PackMetadataSection.SERIALIZER);
				if (packMetadataSection == null) {
					LOGGER.warn("Couldn't find pack meta for pack {}", string);
					return null;
				}

				var8 = packConstructor.create(string, new TextComponent(packResources.getName()), bl, supplier, packMetadataSection, position, packSource);
			}

			return var8;
		} catch (IOException var11) {
			LOGGER.warn("Couldn't get pack info for: {}", var11.toString());
			return null;
		}
	}

	public Pack(
		String string,
		boolean bl,
		Supplier<PackResources> supplier,
		Component component,
		Component component2,
		PackCompatibility packCompatibility,
		Pack.Position position,
		boolean bl2,
		PackSource packSource
	) {
		this.id = string;
		this.supplier = supplier;
		this.title = component;
		this.description = component2;
		this.compatibility = packCompatibility;
		this.required = bl;
		this.defaultPosition = position;
		this.fixedPosition = bl2;
		this.packSource = packSource;
	}

	public Pack(
		String string,
		Component component,
		boolean bl,
		Supplier<PackResources> supplier,
		PackMetadataSection packMetadataSection,
		PackType packType,
		Pack.Position position,
		PackSource packSource
	) {
		this(
			string,
			bl,
			supplier,
			component,
			packMetadataSection.getDescription(),
			PackCompatibility.forMetadata(packMetadataSection, packType),
			position,
			false,
			packSource
		);
	}

	public Component getTitle() {
		return this.title;
	}

	public Component getDescription() {
		return this.description;
	}

	public Component getChatLink(boolean bl) {
		return ComponentUtils.wrapInSquareBrackets(this.packSource.decorate(new TextComponent(this.id)))
			.withStyle(
				style -> style.withColor(bl ? ChatFormatting.GREEN : ChatFormatting.RED)
						.withInsertion(StringArgumentType.escapeIfRequired(this.id))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("").append(this.title).append("\n").append(this.description)))
			);
	}

	public PackCompatibility getCompatibility() {
		return this.compatibility;
	}

	public PackResources open() {
		return (PackResources)this.supplier.get();
	}

	public String getId() {
		return this.id;
	}

	public boolean isRequired() {
		return this.required;
	}

	public boolean isFixedPosition() {
		return this.fixedPosition;
	}

	public Pack.Position getDefaultPosition() {
		return this.defaultPosition;
	}

	public PackSource getPackSource() {
		return this.packSource;
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else if (!(object instanceof Pack)) {
			return false;
		} else {
			Pack pack = (Pack)object;
			return this.id.equals(pack.id);
		}
	}

	public int hashCode() {
		return this.id.hashCode();
	}

	@FunctionalInterface
	public interface PackConstructor {
		@Nullable
		Pack create(
			String string,
			Component component,
			boolean bl,
			Supplier<PackResources> supplier,
			PackMetadataSection packMetadataSection,
			Pack.Position position,
			PackSource packSource
		);
	}

	public static enum Position {
		TOP,
		BOTTOM;

		public <T> int insert(List<T> list, T object, Function<T, Pack> function, boolean bl) {
			Pack.Position position = bl ? this.opposite() : this;
			if (position == BOTTOM) {
				int i;
				for(i = 0; i < list.size(); ++i) {
					Pack pack = (Pack)function.apply(list.get(i));
					if (!pack.isFixedPosition() || pack.getDefaultPosition() != this) {
						break;
					}
				}

				list.add(i, object);
				return i;
			} else {
				int i;
				for(i = list.size() - 1; i >= 0; --i) {
					Pack pack = (Pack)function.apply(list.get(i));
					if (!pack.isFixedPosition() || pack.getDefaultPosition() != this) {
						break;
					}
				}

				list.add(i + 1, object);
				return i + 1;
			}
		}

		public Pack.Position opposite() {
			return this == TOP ? BOTTOM : TOP;
		}
	}
}

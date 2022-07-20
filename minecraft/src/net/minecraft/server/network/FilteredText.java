package net.minecraft.server.network;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public record FilteredText<T>(T raw, @Nullable T filtered) {
	public static final FilteredText<String> EMPTY_STRING = passThrough((T)"");

	public static <T> FilteredText<T> passThrough(T object) {
		return new FilteredText<>(object, object);
	}

	public static <T> FilteredText<T> fullyFiltered(T object) {
		return new FilteredText<>(object, (T)null);
	}

	public <U> FilteredText<U> map(Function<T, U> function) {
		return this.map(function, function);
	}

	public <U> FilteredText<U> map(Function<T, U> function, Function<T, U> function2) {
		return new FilteredText<>((U)function.apply(this.raw), Util.mapNullable(this.filtered, function2));
	}

	public <U> FilteredText<U> rebuildIfNeeded(U object, Function<T, U> function) {
		return !this.isFiltered() ? passThrough(object) : new FilteredText<>(object, Util.mapNullable(this.filtered, function));
	}

	public <U> CompletableFuture<FilteredText<U>> rebuildIfNeededAsync(U object, Function<T, CompletableFuture<U>> function) {
		if (this.filtered() == null) {
			return CompletableFuture.completedFuture(fullyFiltered(object));
		} else {
			return !this.isFiltered()
				? CompletableFuture.completedFuture(passThrough(object))
				: ((CompletableFuture)function.apply(this.filtered())).thenApply(object2 -> new FilteredText<>(object, (U)object2));
		}
	}

	public boolean isFiltered() {
		return !this.raw.equals(this.filtered);
	}

	public boolean isFullyFiltered() {
		return this.filtered == null;
	}

	public T filteredOrElse(T object) {
		return (T)(this.filtered != null ? this.filtered : object);
	}

	@Nullable
	public T filter(ServerPlayer serverPlayer, ServerPlayer serverPlayer2) {
		return (T)(serverPlayer.shouldFilterMessageTo(serverPlayer2) ? this.filtered : this.raw);
	}

	@Nullable
	public T filter(CommandSourceStack commandSourceStack, ServerPlayer serverPlayer) {
		ServerPlayer serverPlayer2 = commandSourceStack.getPlayer();
		return (T)(serverPlayer2 != null ? this.filter(serverPlayer2, serverPlayer) : this.raw);
	}

	@Nullable
	public T select(boolean bl) {
		return (T)(bl ? this.filtered : this.raw);
	}
}

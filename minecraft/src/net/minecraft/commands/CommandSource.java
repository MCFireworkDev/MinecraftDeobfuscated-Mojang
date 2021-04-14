package net.minecraft.commands;

import java.util.UUID;
import net.minecraft.network.chat.Component;

public interface CommandSource {
	CommandSource NULL = new CommandSource() {
		@Override
		public void sendMessage(Component component, UUID uUID) {
		}

		@Override
		public boolean acceptsSuccess() {
			return false;
		}

		@Override
		public boolean acceptsFailure() {
			return false;
		}

		@Override
		public boolean shouldInformAdmins() {
			return false;
		}
	};

	void sendMessage(Component component, UUID uUID);

	boolean acceptsSuccess();

	boolean acceptsFailure();

	boolean shouldInformAdmins();

	default boolean alwaysAccepts() {
		return false;
	}
}

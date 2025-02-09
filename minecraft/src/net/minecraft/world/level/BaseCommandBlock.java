package net.minecraft.world.level;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public abstract class BaseCommandBlock implements CommandSource {
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final Component DEFAULT_NAME = Component.literal("@");
	private long lastExecution = -1L;
	private boolean updateLastExecution = true;
	private int successCount;
	private boolean trackOutput = true;
	@Nullable
	private Component lastOutput;
	private String command = "";
	private Component name = DEFAULT_NAME;

	public int getSuccessCount() {
		return this.successCount;
	}

	public void setSuccessCount(int i) {
		this.successCount = i;
	}

	public Component getLastOutput() {
		return this.lastOutput == null ? CommonComponents.EMPTY : this.lastOutput;
	}

	public CompoundTag save(CompoundTag compoundTag) {
		compoundTag.putString("Command", this.command);
		compoundTag.putInt("SuccessCount", this.successCount);
		compoundTag.putString("CustomName", Component.Serializer.toJson(this.name));
		compoundTag.putBoolean("TrackOutput", this.trackOutput);
		if (this.lastOutput != null && this.trackOutput) {
			compoundTag.putString("LastOutput", Component.Serializer.toJson(this.lastOutput));
		}

		compoundTag.putBoolean("UpdateLastExecution", this.updateLastExecution);
		if (this.updateLastExecution && this.lastExecution > 0L) {
			compoundTag.putLong("LastExecution", this.lastExecution);
		}

		return compoundTag;
	}

	public void load(CompoundTag compoundTag) {
		this.command = compoundTag.getString("Command");
		this.successCount = compoundTag.getInt("SuccessCount");
		if (compoundTag.contains("CustomName", 8)) {
			this.setName(Component.Serializer.fromJson(compoundTag.getString("CustomName")));
		}

		if (compoundTag.contains("TrackOutput", 1)) {
			this.trackOutput = compoundTag.getBoolean("TrackOutput");
		}

		if (compoundTag.contains("LastOutput", 8) && this.trackOutput) {
			try {
				this.lastOutput = Component.Serializer.fromJson(compoundTag.getString("LastOutput"));
			} catch (Throwable var3) {
				this.lastOutput = Component.literal(var3.getMessage());
			}
		} else {
			this.lastOutput = null;
		}

		if (compoundTag.contains("UpdateLastExecution")) {
			this.updateLastExecution = compoundTag.getBoolean("UpdateLastExecution");
		}

		if (this.updateLastExecution && compoundTag.contains("LastExecution")) {
			this.lastExecution = compoundTag.getLong("LastExecution");
		} else {
			this.lastExecution = -1L;
		}
	}

	public void setCommand(String string) {
		this.command = string;
		this.successCount = 0;
	}

	public String getCommand() {
		return this.command;
	}

	public boolean performCommand(Level level) {
		if (level.isClientSide || level.getGameTime() == this.lastExecution) {
			return false;
		} else if ("Searge".equalsIgnoreCase(this.command)) {
			this.lastOutput = Component.literal("#itzlipofutzli");
			this.successCount = 1;
			return true;
		} else {
			this.successCount = 0;
			MinecraftServer minecraftServer = this.getLevel().getServer();
			if (minecraftServer.isCommandBlockEnabled() && !StringUtil.isNullOrEmpty(this.command)) {
				try {
					this.lastOutput = null;
					CommandSourceStack commandSourceStack = this.createCommandSourceStack().withCallback((bl, i) -> {
						if (bl) {
							++this.successCount;
						}
					});
					minecraftServer.getCommands().performPrefixedCommand(commandSourceStack, this.command);
				} catch (Throwable var6) {
					CrashReport crashReport = CrashReport.forThrowable(var6, "Executing command block");
					CrashReportCategory crashReportCategory = crashReport.addCategory("Command to be executed");
					crashReportCategory.setDetail("Command", this::getCommand);
					crashReportCategory.setDetail("Name", (CrashReportDetail<String>)(() -> this.getName().getString()));
					throw new ReportedException(crashReport);
				}
			}

			if (this.updateLastExecution) {
				this.lastExecution = level.getGameTime();
			} else {
				this.lastExecution = -1L;
			}

			return true;
		}
	}

	public Component getName() {
		return this.name;
	}

	public void setName(@Nullable Component component) {
		if (component != null) {
			this.name = component;
		} else {
			this.name = DEFAULT_NAME;
		}
	}

	@Override
	public void sendSystemMessage(Component component) {
		if (this.trackOutput) {
			this.lastOutput = Component.literal("[" + TIME_FORMAT.format(new Date()) + "] ").append(component);
			this.onUpdated();
		}
	}

	public abstract ServerLevel getLevel();

	public abstract void onUpdated();

	public void setLastOutput(@Nullable Component component) {
		this.lastOutput = component;
	}

	public void setTrackOutput(boolean bl) {
		this.trackOutput = bl;
	}

	public boolean isTrackOutput() {
		return this.trackOutput;
	}

	public InteractionResult usedBy(Player player) {
		if (!player.canUseGameMasterBlocks()) {
			return InteractionResult.PASS;
		} else {
			if (player.getCommandSenderWorld().isClientSide) {
				player.openMinecartCommandBlock(this);
			}

			return InteractionResult.sidedSuccess(player.level().isClientSide);
		}
	}

	public abstract Vec3 getPosition();

	public abstract CommandSourceStack createCommandSourceStack();

	@Override
	public boolean acceptsSuccess() {
		return this.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK) && this.trackOutput;
	}

	@Override
	public boolean acceptsFailure() {
		return this.trackOutput;
	}

	@Override
	public boolean shouldInformAdmins() {
		return this.getLevel().getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT);
	}

	public abstract boolean isValid();
}

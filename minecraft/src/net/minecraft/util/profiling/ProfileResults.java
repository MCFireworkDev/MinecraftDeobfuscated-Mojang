package net.minecraft.util.profiling;

import java.nio.file.Path;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface ProfileResults {
	@Environment(EnvType.CLIENT)
	List<ResultField> getTimes(String string);

	boolean saveResults(Path path);

	long getStartTimeNano();

	int getStartTimeTicks();

	long getEndTimeNano();

	int getEndTimeTicks();

	default long getNanoDuration() {
		return this.getEndTimeNano() - this.getStartTimeNano();
	}

	default int getTickDuration() {
		return this.getEndTimeTicks() - this.getStartTimeTicks();
	}

	static String demanglePath(String string) {
		return string.replace('\u001e', '.');
	}
}

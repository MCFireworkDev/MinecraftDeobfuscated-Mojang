package net.minecraft.server.rcon.thread;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.DefaultUncaughtExceptionHandlerWithName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class GenericThread implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
	protected volatile boolean running;
	protected final String name;
	protected Thread thread;

	protected GenericThread(String string) {
		this.name = string;
	}

	public synchronized void start() {
		this.running = true;
		this.thread = new Thread(this, this.name + " #" + UNIQUE_THREAD_ID.incrementAndGet());
		this.thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandlerWithName(LOGGER));
		this.thread.start();
		LOGGER.info("Thread {} started", this.name);
	}

	public synchronized void stop() {
		this.running = false;
		if (null != this.thread) {
			int i = 0;

			while(this.thread.isAlive()) {
				try {
					this.thread.join(1000L);
					if (++i >= 5) {
						LOGGER.warn("Waited {} seconds attempting force stop!", i);
					} else if (this.thread.isAlive()) {
						LOGGER.warn("Thread {} ({}) failed to exit after {} second(s)", this, this.thread.getState(), i, new Exception("Stack:"));
						this.thread.interrupt();
					}
				} catch (InterruptedException var3) {
				}
			}

			LOGGER.info("Thread {} stopped", this.name);
			this.thread = null;
		}
	}

	public boolean isRunning() {
		return this.running;
	}
}

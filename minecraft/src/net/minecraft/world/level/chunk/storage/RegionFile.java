package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class RegionFile implements AutoCloseable {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int SECTOR_BYTES = 4096;
	@VisibleForTesting
	protected static final int SECTOR_INTS = 1024;
	private static final int CHUNK_HEADER_SIZE = 5;
	private static final int HEADER_OFFSET = 0;
	private static final ByteBuffer PADDING_BUFFER = ByteBuffer.allocateDirect(1);
	private static final String EXTERNAL_FILE_EXTENSION = ".mcc";
	private static final int EXTERNAL_STREAM_FLAG = 128;
	private static final int EXTERNAL_CHUNK_THRESHOLD = 256;
	private static final int CHUNK_NOT_PRESENT = 0;
	private final FileChannel file;
	private final Path externalFileDir;
	final RegionFileVersion version;
	private final ByteBuffer header = ByteBuffer.allocateDirect(8192);
	private final IntBuffer offsets;
	private final IntBuffer timestamps;
	@VisibleForTesting
	protected final RegionBitmap usedSectors = new RegionBitmap();

	public RegionFile(Path path, Path path2, boolean bl) throws IOException {
		this(path, path2, RegionFileVersion.VERSION_DEFLATE, bl);
	}

	public RegionFile(Path path, Path path2, RegionFileVersion regionFileVersion, boolean bl) throws IOException {
		this.version = regionFileVersion;
		if (!Files.isDirectory(path2, new LinkOption[0])) {
			throw new IllegalArgumentException("Expected directory, got " + path2.toAbsolutePath());
		} else {
			this.externalFileDir = path2;
			this.offsets = this.header.asIntBuffer();
			this.offsets.limit(1024);
			this.header.position(4096);
			this.timestamps = this.header.asIntBuffer();
			if (bl) {
				this.file = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DSYNC);
			} else {
				this.file = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
			}

			this.usedSectors.force(0, 2);
			this.header.position(0);
			int i = this.file.read(this.header, 0L);
			if (i != -1) {
				if (i != 8192) {
					LOGGER.warn("Region file {} has truncated header: {}", path, i);
				}

				long l = Files.size(path);

				for(int j = 0; j < 1024; ++j) {
					int k = this.offsets.get(j);
					if (k != 0) {
						int m = getSectorNumber(k);
						int n = getNumSectors(k);
						if (m < 2) {
							LOGGER.warn("Region file {} has invalid sector at index: {}; sector {} overlaps with header", path, j, m);
							this.offsets.put(j, 0);
						} else if (n == 0) {
							LOGGER.warn("Region file {} has an invalid sector at index: {}; size has to be > 0", path, j);
							this.offsets.put(j, 0);
						} else if ((long)m * 4096L > l) {
							LOGGER.warn("Region file {} has an invalid sector at index: {}; sector {} is out of bounds", path, j, m);
							this.offsets.put(j, 0);
						} else {
							this.usedSectors.force(m, n);
						}
					}
				}
			}
		}
	}

	private Path getExternalChunkPath(ChunkPos chunkPos) {
		String string = "c." + chunkPos.x + "." + chunkPos.z + ".mcc";
		return this.externalFileDir.resolve(string);
	}

	@Nullable
	public synchronized DataInputStream getChunkDataInputStream(ChunkPos chunkPos) throws IOException {
		int i = this.getOffset(chunkPos);
		if (i == 0) {
			return null;
		} else {
			int j = getSectorNumber(i);
			int k = getNumSectors(i);
			int l = k * 4096;
			ByteBuffer byteBuffer = ByteBuffer.allocate(l);
			this.file.read(byteBuffer, (long)(j * 4096));
			byteBuffer.flip();
			if (byteBuffer.remaining() < 5) {
				LOGGER.error("Chunk {} header is truncated: expected {} but read {}", chunkPos, l, byteBuffer.remaining());
				return null;
			} else {
				int m = byteBuffer.getInt();
				byte b = byteBuffer.get();
				if (m == 0) {
					LOGGER.warn("Chunk {} is allocated, but stream is missing", chunkPos);
					return null;
				} else {
					int n = m - 1;
					if (isExternalStreamChunk(b)) {
						if (n != 0) {
							LOGGER.warn("Chunk has both internal and external streams");
						}

						return this.createExternalChunkInputStream(chunkPos, getExternalChunkVersion(b));
					} else if (n > byteBuffer.remaining()) {
						LOGGER.error("Chunk {} stream is truncated: expected {} but read {}", chunkPos, n, byteBuffer.remaining());
						return null;
					} else if (n < 0) {
						LOGGER.error("Declared size {} of chunk {} is negative", m, chunkPos);
						return null;
					} else {
						return this.createChunkInputStream(chunkPos, b, createStream(byteBuffer, n));
					}
				}
			}
		}
	}

	private static int getTimestamp() {
		return (int)(Util.getEpochMillis() / 1000L);
	}

	private static boolean isExternalStreamChunk(byte b) {
		return (b & 128) != 0;
	}

	private static byte getExternalChunkVersion(byte b) {
		return (byte)(b & -129);
	}

	@Nullable
	private DataInputStream createChunkInputStream(ChunkPos chunkPos, byte b, InputStream inputStream) throws IOException {
		RegionFileVersion regionFileVersion = RegionFileVersion.fromId(b);
		if (regionFileVersion == null) {
			LOGGER.error("Chunk {} has invalid chunk stream version {}", chunkPos, b);
			return null;
		} else {
			return new DataInputStream(regionFileVersion.wrap(inputStream));
		}
	}

	@Nullable
	private DataInputStream createExternalChunkInputStream(ChunkPos chunkPos, byte b) throws IOException {
		Path path = this.getExternalChunkPath(chunkPos);
		if (!Files.isRegularFile(path, new LinkOption[0])) {
			LOGGER.error("External chunk path {} is not file", path);
			return null;
		} else {
			return this.createChunkInputStream(chunkPos, b, Files.newInputStream(path));
		}
	}

	private static ByteArrayInputStream createStream(ByteBuffer byteBuffer, int i) {
		return new ByteArrayInputStream(byteBuffer.array(), byteBuffer.position(), i);
	}

	private int packSectorOffset(int i, int j) {
		return i << 8 | j;
	}

	private static int getNumSectors(int i) {
		return i & 0xFF;
	}

	private static int getSectorNumber(int i) {
		return i >> 8 & 16777215;
	}

	private static int sizeToSectors(int i) {
		return (i + 4096 - 1) / 4096;
	}

	public boolean doesChunkExist(ChunkPos chunkPos) {
		int i = this.getOffset(chunkPos);
		if (i == 0) {
			return false;
		} else {
			int j = getSectorNumber(i);
			int k = getNumSectors(i);
			ByteBuffer byteBuffer = ByteBuffer.allocate(5);

			try {
				this.file.read(byteBuffer, (long)(j * 4096));
				byteBuffer.flip();
				if (byteBuffer.remaining() != 5) {
					return false;
				} else {
					int l = byteBuffer.getInt();
					byte b = byteBuffer.get();
					if (isExternalStreamChunk(b)) {
						if (!RegionFileVersion.isValidVersion(getExternalChunkVersion(b))) {
							return false;
						}

						if (!Files.isRegularFile(this.getExternalChunkPath(chunkPos), new LinkOption[0])) {
							return false;
						}
					} else {
						if (!RegionFileVersion.isValidVersion(b)) {
							return false;
						}

						if (l == 0) {
							return false;
						}

						int m = l - 1;
						if (m < 0 || m > 4096 * k) {
							return false;
						}
					}

					return true;
				}
			} catch (IOException var9) {
				return false;
			}
		}
	}

	public DataOutputStream getChunkDataOutputStream(ChunkPos chunkPos) throws IOException {
		return new DataOutputStream(this.version.wrap(new RegionFile.ChunkBuffer(chunkPos)));
	}

	public void flush() throws IOException {
		this.file.force(true);
	}

	public void clear(ChunkPos chunkPos) throws IOException {
		int i = getOffsetIndex(chunkPos);
		int j = this.offsets.get(i);
		if (j != 0) {
			this.offsets.put(i, 0);
			this.timestamps.put(i, getTimestamp());
			this.writeHeader();
			Files.deleteIfExists(this.getExternalChunkPath(chunkPos));
			this.usedSectors.free(getSectorNumber(j), getNumSectors(j));
		}
	}

	protected synchronized void write(ChunkPos chunkPos, ByteBuffer byteBuffer) throws IOException {
		int i = getOffsetIndex(chunkPos);
		int j = this.offsets.get(i);
		int k = getSectorNumber(j);
		int l = getNumSectors(j);
		int m = byteBuffer.remaining();
		int n = sizeToSectors(m);
		int o;
		RegionFile.CommitOp commitOp;
		if (n >= 256) {
			Path path = this.getExternalChunkPath(chunkPos);
			LOGGER.warn("Saving oversized chunk {} ({} bytes} to external file {}", chunkPos, m, path);
			n = 1;
			o = this.usedSectors.allocate(n);
			commitOp = this.writeToExternalFile(path, byteBuffer);
			ByteBuffer byteBuffer2 = this.createExternalStub();
			this.file.write(byteBuffer2, (long)(o * 4096));
		} else {
			o = this.usedSectors.allocate(n);
			commitOp = () -> Files.deleteIfExists(this.getExternalChunkPath(chunkPos));
			this.file.write(byteBuffer, (long)(o * 4096));
		}

		this.offsets.put(i, this.packSectorOffset(o, n));
		this.timestamps.put(i, getTimestamp());
		this.writeHeader();
		commitOp.run();
		if (k != 0) {
			this.usedSectors.free(k, l);
		}
	}

	private ByteBuffer createExternalStub() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(5);
		byteBuffer.putInt(1);
		byteBuffer.put((byte)(this.version.getId() | 128));
		byteBuffer.flip();
		return byteBuffer;
	}

	private RegionFile.CommitOp writeToExternalFile(Path path, ByteBuffer byteBuffer) throws IOException {
		Path path2 = Files.createTempFile(this.externalFileDir, "tmp", null);
		FileChannel fileChannel = FileChannel.open(path2, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

		try {
			byteBuffer.position(5);
			fileChannel.write(byteBuffer);
		} catch (Throwable var8) {
			if (fileChannel != null) {
				try {
					fileChannel.close();
				} catch (Throwable var7) {
					var8.addSuppressed(var7);
				}
			}

			throw var8;
		}

		if (fileChannel != null) {
			fileChannel.close();
		}

		return () -> Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING);
	}

	private void writeHeader() throws IOException {
		this.header.position(0);
		this.file.write(this.header, 0L);
	}

	private int getOffset(ChunkPos chunkPos) {
		return this.offsets.get(getOffsetIndex(chunkPos));
	}

	public boolean hasChunk(ChunkPos chunkPos) {
		return this.getOffset(chunkPos) != 0;
	}

	private static int getOffsetIndex(ChunkPos chunkPos) {
		return chunkPos.getRegionLocalX() + chunkPos.getRegionLocalZ() * 32;
	}

	public void close() throws IOException {
		try {
			this.padToFullSector();
		} finally {
			try {
				this.file.force(true);
			} finally {
				this.file.close();
			}
		}
	}

	private void padToFullSector() throws IOException {
		int i = (int)this.file.size();
		int j = sizeToSectors(i) * 4096;
		if (i != j) {
			ByteBuffer byteBuffer = PADDING_BUFFER.duplicate();
			byteBuffer.position(0);
			this.file.write(byteBuffer, (long)(j - 1));
		}
	}

	class ChunkBuffer extends ByteArrayOutputStream {
		private final ChunkPos pos;

		public ChunkBuffer(ChunkPos chunkPos) {
			super(8096);
			super.write(0);
			super.write(0);
			super.write(0);
			super.write(0);
			super.write(RegionFile.this.version.getId());
			this.pos = chunkPos;
		}

		public void close() throws IOException {
			ByteBuffer byteBuffer = ByteBuffer.wrap(this.buf, 0, this.count);
			byteBuffer.putInt(0, this.count - 5 + 1);
			RegionFile.this.write(this.pos, byteBuffer);
		}
	}

	interface CommitOp {
		void run() throws IOException;
	}
}

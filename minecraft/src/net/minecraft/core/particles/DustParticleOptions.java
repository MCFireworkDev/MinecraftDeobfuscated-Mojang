package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DustParticleOptions extends DustParticleOptionsBase {
	public static final Vector3f REDSTONE_PARTICLE_COLOR = Vec3.fromRGB24(16711680).toVector3f();
	public static final DustParticleOptions REDSTONE = new DustParticleOptions(REDSTONE_PARTICLE_COLOR, 1.0F);
	public static final Codec<DustParticleOptions> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					ExtraCodecs.VECTOR3F.fieldOf("color").forGetter(dustParticleOptions -> dustParticleOptions.color),
					Codec.FLOAT.fieldOf("scale").forGetter(dustParticleOptions -> dustParticleOptions.scale)
				)
				.apply(instance, DustParticleOptions::new)
	);
	public static final ParticleOptions.Deserializer<DustParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<DustParticleOptions>() {
		public DustParticleOptions fromCommand(ParticleType<DustParticleOptions> particleType, StringReader stringReader) throws CommandSyntaxException {
			Vector3f vector3f = DustParticleOptionsBase.readVector3f(stringReader);
			stringReader.expect(' ');
			float f = stringReader.readFloat();
			return new DustParticleOptions(vector3f, f);
		}

		public DustParticleOptions fromNetwork(ParticleType<DustParticleOptions> particleType, FriendlyByteBuf friendlyByteBuf) {
			return new DustParticleOptions(DustParticleOptionsBase.readVector3f(friendlyByteBuf), friendlyByteBuf.readFloat());
		}
	};

	public DustParticleOptions(Vector3f vector3f, float f) {
		super(vector3f, f);
	}

	@Override
	public ParticleType<DustParticleOptions> getType() {
		return ParticleTypes.DUST;
	}
}

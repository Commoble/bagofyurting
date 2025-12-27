package net.commoble.bagofyurting;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.commoble.bagofyurting.client.ClientProxy;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OptionalSpawnParticlesPacket(
	ParticleOptions particle,
	Vec3 vec,
	Vector3fc dist,
	int count) implements CustomPacketPayload
{	
	public static final CustomPacketPayload.Type<OptionalSpawnParticlesPacket> TYPE = new CustomPacketPayload.Type<>(BagOfYurtingMod.id("optional_spawn_particles"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OptionalSpawnParticlesPacket> STREAM_CODEC = StreamCodec.composite(
		ParticleTypes.STREAM_CODEC, OptionalSpawnParticlesPacket::particle,
		StreamCodec.composite(
			ByteBufCodecs.DOUBLE, Vec3::x,
			ByteBufCodecs.DOUBLE, Vec3::y,
			ByteBufCodecs.DOUBLE, Vec3::z,
			Vec3::new), OptionalSpawnParticlesPacket::vec,
		ByteBufCodecs.VECTOR3F, OptionalSpawnParticlesPacket::dist,
		ByteBufCodecs.INT, OptionalSpawnParticlesPacket::count,
		OptionalSpawnParticlesPacket::new);
	
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void spawnParticlesFromServer(ServerLevel level, ParticleOptions particle, Vec3 vec, Vector3f dist, int particleCount)
	{
		OptionalSpawnParticlesPacket packet = new OptionalSpawnParticlesPacket(particle, vec, dist, particleCount);
		PacketDistributor.sendToPlayersNear(level, null, vec.x, vec.y, vec.z, 32D, packet);
	}
	
	public void handle(IPayloadContext context)
	{
		if (FMLEnvironment.getDist().isClient())
		{
			ClientProxy.onHandleOptionalSpawnParticlePacket(this);
		}
	}

	public ClientboundLevelParticlesPacket toVanillaPacket()
	{
		return new ClientboundLevelParticlesPacket(
			this.particle,
			false,
			false,
			this.vec.x,
			this.vec.y,
			this.vec.z,
			this.dist.x(),
			this.dist.y(),
			this.dist.z(),
			0,
			this.count);
	}
}

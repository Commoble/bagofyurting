package commoble.bagofyurting;

import java.util.function.Supplier;

import commoble.bagofyurting.client.ClientEvents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.network.PacketDistributor.TargetPoint;

public class OptionalSpawnParticlePacket extends ClientboundLevelParticlesPacket
{	
	public <T extends ParticleOptions> OptionalSpawnParticlePacket(T type, boolean b, double posX, double posY, double posZ, float xOffset, float yOffset, float zOffset, float speed, int particleCount)
	{
		super(type,b,posX,posY,posZ,xOffset,yOffset,zOffset,speed,particleCount);
	}
	
	protected OptionalSpawnParticlePacket(FriendlyByteBuf buffer)
	{
		super(buffer);
	}
	
	public static OptionalSpawnParticlePacket read(FriendlyByteBuf buffer)
	{
	      return new OptionalSpawnParticlePacket(buffer);
	}

	public static <T extends ParticleOptions> void spawnParticlesFromServer(ServerLevel level, T particleType, double posX, double posY, double posZ, int particleCount, double xOffset,
		double yOffset, double zOffset, double speed)
	{
		OptionalSpawnParticlePacket packet = new OptionalSpawnParticlePacket(particleType, false, posX, posY, posZ, (float) xOffset, (float) yOffset, (float) zOffset, (float) speed,
			particleCount);
		BagOfYurtingMod.CHANNEL.send(PacketDistributor.NEAR.with(() -> new TargetPoint(posX, posY, posZ, 32D, level.dimension())), packet);
	}

	@Override
	public void handle(ClientGamePacketListener handler)
	{
		if (FMLEnvironment.dist == Dist.CLIENT && ClientEvents.canSpawnBagParticles())
		{
			super.handle(handler);
		}
	}
	
	public void handle(Supplier<NetworkEvent.Context> contextGetter)
	{
		NetworkEvent.Context context = contextGetter.get();
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientEvents.onHandleOptionalSpawnParticlePacket(this);
		}
		context.setPacketHandled(true);
	}

}

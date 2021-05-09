package commoble.bagofyurting;

import java.io.IOException;
import java.util.function.Supplier;

import commoble.bagofyurting.client.ClientEvents;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SSpawnParticlePacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.PacketDistributor.TargetPoint;

public class OptionalSpawnParticlePacket extends SSpawnParticlePacket
{
	public OptionalSpawnParticlePacket()
	{
		super();
	}
	
	public <T extends IParticleData> OptionalSpawnParticlePacket(T type, boolean b, double posX, double posY, double posZ, float xOffset, float yOffset, float zOffset, float speed, int particleCount)
	{
		super(type,b,posX,posY,posZ,xOffset,yOffset,zOffset,speed,particleCount);
	}
	
	public static OptionalSpawnParticlePacket read(PacketBuffer buffer)
	{
	      OptionalSpawnParticlePacket packet = new OptionalSpawnParticlePacket();
	      try
	      {
		      packet.readPacketData(buffer);
	      }
	      catch(IOException e)
	      {
	    	  // noop
	      }
	      return packet;
	}
	
	public void write(PacketBuffer buffer)
	{
		try
		{
			this.writePacketData(buffer);
		}
		catch(IOException e)
		{
			// noop
		}
	}

	public static <T extends IParticleData> void spawnParticlesFromServer(ServerWorld world, T particleType, double posX, double posY, double posZ, int particleCount, double xOffset,
		double yOffset, double zOffset, double speed)
	{
		OptionalSpawnParticlePacket packet = new OptionalSpawnParticlePacket(particleType, false, posX, posY, posZ, (float) xOffset, (float) yOffset, (float) zOffset, (float) speed,
			particleCount);
		BagOfYurtingMod.CHANNEL.send(PacketDistributor.NEAR.with(() -> new TargetPoint(posX, posY, posZ, 32D, world.getDimensionKey())), packet);
	}

	@Override
	public void processPacket(IClientPlayNetHandler handler)
	{
		if (FMLEnvironment.dist == Dist.CLIENT && ClientEvents.canSpawnBagParticles())
		{
			super.processPacket(handler);
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

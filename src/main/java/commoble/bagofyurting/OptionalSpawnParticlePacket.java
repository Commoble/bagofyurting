package commoble.bagofyurting;

import java.util.List;

import commoble.bagofyurting.client.ClientEvents;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SSpawnParticlePacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class OptionalSpawnParticlePacket extends SSpawnParticlePacket
{
	public <T extends IParticleData> OptionalSpawnParticlePacket(T type, boolean b, double posX, double posY, double posZ, float xOffset, float yOffset, float zOffset, float speed, int particleCount)
	{
		super(type,b,posX,posY,posZ,xOffset,yOffset,zOffset,speed,particleCount);
	}

	public static <T extends IParticleData> void spawnParticlesFromServer(ServerWorld world, T particleType, double posX, double posY, double posZ, int particleCount, double xOffset,
		double yOffset, double zOffset, double speed)
	{
		OptionalSpawnParticlePacket packet = new OptionalSpawnParticlePacket(particleType, false, posX, posY, posZ, (float) xOffset, (float) yOffset, (float) zOffset, (float) speed,
			particleCount);

		List<ServerPlayerEntity> players = world.getPlayers();
		int playerCount = players.size();
		for (int playerIndex = 0; playerIndex < playerCount; ++playerIndex)
		{
			ServerPlayerEntity player = players.get(playerIndex);
			BlockPos playerPos = player.getPosition();
			if (playerPos.withinDistance(new Vector3d(posX, posY, posZ), 32.0D))
			{
				player.connection.sendPacket(packet);
			}
		}
	}

	@Override
	public void processPacket(IClientPlayNetHandler handler)
	{
		if (FMLEnvironment.dist == Dist.CLIENT && ClientEvents.canSpawnBagParticles())
		{
			super.processPacket(handler);
		}
	}

}

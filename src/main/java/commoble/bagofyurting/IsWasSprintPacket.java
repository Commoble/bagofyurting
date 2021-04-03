package commoble.bagofyurting;

import java.util.function.Supplier;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class IsWasSprintPacket
{
	private boolean isSprintHeld;

	public IsWasSprintPacket(boolean isSprintHeld)
	{
		this.isSprintHeld = isSprintHeld;
	}

	public void write(PacketBuffer buf)
	{
		buf.writeByte(this.isSprintHeld ? 1 : 0);
	}

	public static IsWasSprintPacket read(PacketBuffer buf)
	{
		return new IsWasSprintPacket(buf.readByte() > 0);
	}

	public void handle(Supplier<NetworkEvent.Context> contextGetter)
	{
		NetworkEvent.Context context = contextGetter.get();
		// PlayerData needs to be threadsafed, packet handling is done on worker
		// threads, delegate to main thread
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player != null)
			{
				TransientPlayerData.setOverridingSafetyList(player.getUniqueID(), this.isSprintHeld);
			}
		});
		context.setPacketHandled(true);
	}
}

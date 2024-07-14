package commoble.bagofyurting;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record IsWasSprintPacket(boolean isSprintHeld) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<IsWasSprintPacket> TYPE = new CustomPacketPayload.Type<>(BagOfYurtingMod.id("is_was_sprint"));
	
	public static final StreamCodec<ByteBuf, IsWasSprintPacket> STREAM_CODEC = ByteBufCodecs.BOOL.map(IsWasSprintPacket::new, IsWasSprintPacket::isSprintHeld);
	
	public void handle(IPayloadContext context)
	{
		// PlayerData needs to be threadsafed, packet handling is done on worker
		// threads, delegate to main thread
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer serverPlayer)
			{
				TransientPlayerData.setOverridingSafetyList(serverPlayer.getUUID(), this.isSprintHeld);
			}
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
}

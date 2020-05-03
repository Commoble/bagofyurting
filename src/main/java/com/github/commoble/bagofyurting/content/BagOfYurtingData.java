package com.github.commoble.bagofyurting.content;

import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.github.commoble.bagofyurting.Config;
import com.github.commoble.bagofyurting.TagWrappers;
import com.github.commoble.bagofyurting.util.BagOperation;
import com.github.commoble.bagofyurting.util.NBTMapHelper;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;

public class BagOfYurtingData
{
	public static final String NBT_KEY = "map";
	public static final Direction BASE_DIRECTION = Direction.SOUTH;	// south is arbitrarily chosen for having horizontal index 0
	
	private static final NBTMapHelper<BlockPos, CompoundNBT, BlockPosEntry, CompoundNBT> mapper = new NBTMapHelper<>(
		NBT_KEY,
		NBTUtil::writeBlockPos,
		NBTUtil::readBlockPos,
		BlockPosEntry::write,
		BlockPosEntry::read);
	
	private final Map<BlockPos, BlockPosEntry> map;
	
	/** Removes blocks from the world and stores them as yurt data, returning the data **/
	public static BagOfYurtingData yurtBlocksAndConvertToData(ItemUseContext context, int radius)
	{
		
		// this is the block that the player used the item on
		BlockPos origin = context.getPos();
		
		BlockPos vertexA = origin.add(-radius, 0, -radius);
		BlockPos vertexB = origin.add(radius, 2*radius, radius);
		
		// map each position in the loading area to a rotated offset from the player
		// don't get any blocks that we aren't allowed to get
		
		Map<BlockPos, BlockPosEntry> transformedCoords = BlockPos.getAllInBox(vertexA, vertexB)
			.filter(pos -> canBlockBeStored(context, pos))
			.collect(Collectors.toMap(
				pos -> transformBlockPos(context, pos, origin, BagOperation.LOAD),
				pos -> removeBlockAndGetPosEntry(context, pos)));
		
		transformedCoords.keySet().forEach(pos -> context.getWorld().removeBlock(pos, false));
		
		return new BagOfYurtingData(transformedCoords);
	}
	
	private static boolean canBlockBeStored(ItemUseContext context, BlockPos pos)
	{
		World world = context.getWorld();
		BlockState state = world.getBlockState(pos);
		PlayerEntity player = context.getPlayer();
		return !World.isOutsideBuildHeight(pos) && canBlockBeYurted(player, state, pos) && doesBreakEventSucceed(world, pos, state, player);
	}
	
	/** 
	 * Returns true if the whitelist/blacklist do not forbid the block from being yurted, or if the player
	 * has sufficient permission to ignore these
	 */
	private static boolean canBlockBeYurted(@Nullable PlayerEntity player, BlockState state, BlockPos pos)
	{
		if (player != null && player.hasPermissionLevel(Config.INSTANCE.minPermissionToYurtUnyurtableBlocks.get()))
		{
			return true;
		}
		else
		{
			return !state.isIn(TagWrappers.blacklist)
				&& (TagWrappers.whitelist.getAllElements().isEmpty() || state.isIn(TagWrappers.whitelist));
		}
	}
	
	private static boolean doesBreakEventSucceed(World world, BlockPos pos, BlockState state, PlayerEntity player)
	{
		if (!(world instanceof ServerWorld))
		{
			return false;
		}
		// item use context can have null player
		// but break event requires nonnull player
		// so we use a fake player if player is null
		PlayerEntity breakingPlayer = player != null ? player : FakePlayerFactory.getMinecraft((ServerWorld)world);
		BreakEvent event = new BreakEvent(world, pos, state, breakingPlayer);
		MinecraftForge.EVENT_BUS.post(event);
		return !event.isCanceled();
	}
	
	/**
	 * First, we take the offset of the block pos relative to the origin of the yurt
	 * 
	 * Second, we rotate this offset around the origin's y-axis
	 * 
	 * The rotation is the difference angle between the player's orientation and some constant direction
	 * 
	 * The difference when we unload the yurt is reversed from the difference when we load the yurt (b-a instead of a-b)
	 * 
	 * The result is that when we unload, the rotation of the yurt relative to the player is preserved
	 * 
	 * some examples:
	 * 
player facing south both times -- no rotation
player facing west both times -- first rotate 90 degrees, then -90 degrees -- no rotation

player facing south first, then west
	first -- no rotation -- block offsets are original positions
	second -- rotate from SOUTH to WEST = +90 degrees
	a block that was originally south of the player will now be west of the player
	a block that was originally in front of the player will still be in front of the player

player facing west first, then east
	first -- rotate from west to south -- -90 degrees
	second -- rotate from south to east -- -90 degrees
	all blocks will be rotated 180 degrees around the player
	 */
	private static BlockPos transformBlockPos(ItemUseContext context, BlockPos pos, BlockPos origin, BagOperation operation)
	{
		Direction orientation = context.getPlacementHorizontalFacing();
		
		BlockPos offset = pos.subtract(origin);	// the difference between the given pos and the yurt origin

		return offset.rotate(operation.getDataRotation(orientation));
	}
	
	private static BlockPosEntry removeBlockAndGetPosEntry(ItemUseContext context, BlockPos pos)
	{
		BlockPosEntry entry = getPosEntry(context, pos);
		context.getWorld().removeBlock(pos, false);
		return entry;
	}
	
	/** The position here is the untransformed position whose data is to be stored **/
	private static BlockPosEntry getPosEntry(ItemUseContext context, BlockPos pos)
	{
		CompoundNBT nbt = new CompoundNBT();
		World world = context.getWorld();
		BlockState state = world.getBlockState(pos);
		TileEntity te = world.getTileEntity(pos);
		if (te != null)
		{
			te.write(nbt);
		}
		return new BlockPosEntry(state, nbt);
		
	}
	
	/** Creates a new instance from an NBT compound. This assumes that the given nbt has the "yurt" key within **/ 
	public static BagOfYurtingData read(CompoundNBT nbt)
	{
		return new BagOfYurtingData(mapper.read(nbt));
	}
	
	public BagOfYurtingData(Map<BlockPos, BlockPosEntry> map)
	{
		this.map = map;
	}
	
	public boolean isEmpty()
	{
		return this.map.isEmpty();
	}
	
	public CompoundNBT toNBT()
	{
		return mapper.write(this.map, new CompoundNBT());
	}
	
	public static class BlockPosEntry
	{
		public static final String BLOCKSTATE = "state";
		public static final String TILE ="te";
		
		private final BlockState state;
		private final CompoundNBT tileEntityData;
		
		public BlockPosEntry(BlockState state, CompoundNBT tileEntityData)
		{
			this.state = state;
			this.tileEntityData = tileEntityData;
		}
		
		public CompoundNBT write()
		{
			CompoundNBT nbt = new CompoundNBT();
			
			nbt.put(BLOCKSTATE, NBTUtil.writeBlockState(this.state));
			nbt.put(TILE, this.tileEntityData);
			
			return nbt;
		}
		
		public static BlockPosEntry read(CompoundNBT nbt)
		{
			BlockState state = NBTUtil.readBlockState(nbt.getCompound(BLOCKSTATE));
			CompoundNBT te = nbt.getCompound(TILE);
			
			return new BlockPosEntry(state, te);
		}
	}
}

package com.github.commoble.bagofyurting;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.github.commoble.bagofyurting.util.NBTMapHelper;
import com.github.commoble.bagofyurting.util.RotationUtil;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityMultiPlaceEvent;

public class BagOfYurtingData
{
	public static final String NBT_KEY = "yurtdata";
	public static final Direction BASE_DIRECTION = Direction.SOUTH;	// south is arbitrarily chosen for having horizontal index 0
	
	private static final NBTMapHelper<BlockPos, CompoundNBT, StateData, CompoundNBT> mapper = new NBTMapHelper<>(
		NBT_KEY,
		NBTUtil::writeBlockPos,
		NBTUtil::readBlockPos,
		StateData::write,
		StateData::read);
	
	private final Map<BlockPos, StateData> map;
	
	public BagOfYurtingData(Map<BlockPos, StateData> map)
	{
		this.map = map;
	}
	
	/** Removes blocks from the world and stores them as yurt data, returning the data **/
	public static BagOfYurtingData yurtBlocksAndConvertToData(ItemUseContext context, int radius)
	{		
		// this is the block that the player used the item on
		BlockPos origin = context.getPos();
		Direction orientation = context.getPlacementHorizontalFacing();
		World world = context.getWorld();
		
		BlockPos vertexA = origin.add(-radius, 0, -radius);
		BlockPos vertexB = origin.add(radius, 2*radius, radius);
		
		
		
		// map each position in the loading area to a rotated offset from the player
		// don't get any blocks that we aren't allowed to get
		
		Map<BlockPos, Pair<BlockPos, StateData>> transformPairs = BlockPos.getAllInBox(vertexA, vertexB)
			.filter(pos -> canBlockBeStored(context, pos))
			.map(BlockPos::toImmutable)
			.sorted(new BlockRemovalSorter(world))
			.collect(Collectors.toMap(
				pos -> pos,
				// maps a blockpos in worldspace to a relative position based on origin pos and player facing
				pos -> Pair.of(transformBlockPos(orientation, pos, origin), getPosEntryAndRemoveBlock(context, pos))));
		
		Map<BlockPos, StateData> transformData = transformPairs.values().stream()
			.collect(Collectors.toMap(
				pair -> pair.getLeft(),
				pair -> pair.getRight()));
		
		if (transformPairs.size() > 0 && world instanceof ServerWorld)
		{
			doPoofEffects((ServerWorld) world, transformPairs.keySet());
			
			// we don't cause block updates while removing blocks, wait until the end and then notify all blocks at once
			for (Entry<BlockPos, Pair<BlockPos, StateData>> entry : transformPairs.entrySet())
			{
				BlockPos pos = entry.getKey();
				BlockState oldState = entry.getValue().getRight().state;
				sendBlockUpdateAfterRemoval(world, pos, oldState);
			}
		}
		
		return new BagOfYurtingData(transformData);
	}
	
	private static void sendBlockUpdateAfterRemoval(World world, BlockPos pos, BlockState oldState)
	{
		world.notifyBlockUpdate(pos, oldState, Blocks.AIR.getDefaultState(), 3);
		world.notifyNeighborsOfStateChange(pos, oldState.getBlock());
	}
	
	public boolean attemptUnloadIntoWorld(ItemUseContext context, int radius)
	{
		World world = context.getWorld();
		
		// this is the block adjacent to the face that the player used the item on
		// unless the block we use it on is replaceable
		// in which case the origin is that block
		BlockPos hitPos = context.getPos();
		boolean hitBlockReplaceable = (world.getBlockState(hitPos).getMaterial().isReplaceable());
		BlockPos origin = hitBlockReplaceable ? hitPos : hitPos.offset(context.getFace());
		Direction orientation = context.getPlacementHorizontalFacing();
		Rotation unrotation = RotationUtil.getUntransformRotation(orientation);
		PlayerEntity player = context.getPlayer();
		
		Map<BlockPos, StateData> worldPositions = this.map.entrySet()
			.stream()
			.collect(Collectors.toMap(
				entry -> untransformBlockPos(unrotation, entry.getKey(), origin), // untransform transformed offset
				entry -> entry.getValue()));
		
		// make sure all blocks we want to place are placeable
		boolean success = worldPositions.entrySet().stream()
			.allMatch(entry -> canBlockBeUnloadedAt(entry.getKey(), world, player))
			&& doesPlaceEventSucceed(context, world, player, worldPositions);
		
		if (success)
		{
			worldPositions.entrySet().stream()
				.sorted(BlockUnloadSorter.INSTANCE)
				.forEach(entry -> entry.getValue().setBlockAndTE(world, entry.getKey(), unrotation));
			
			if (world instanceof ServerWorld)
			{
				doPoofEffects((ServerWorld)world, worldPositions.keySet());
			}
		}
		
		return success;		
	}
	
	public static final AxisAlignedBB EMPTY_AABB = new AxisAlignedBB(0,0,0,0,0,0);
	
	private static void doPoofEffects(ServerWorld world, Collection<BlockPos> changedPositions)
	{
		AxisAlignedBB aabb = changedPositions.stream()
			.map(AxisAlignedBB::new)
			.reduce(AxisAlignedBB::union)
			.orElse(EMPTY_AABB);
		if (aabb.getAverageEdgeLength() > 0.5D)
		{
			Vector3d center = aabb.getCenter();
			double xRadius = aabb.getXSize()*0.5;
			double yRadius = aabb.getYSize()*0.5;
			double zRadius = aabb.getZSize()*0.5;
			
			double volume = xRadius * yRadius * zRadius * 8D;
			
			int particles = Math.max(5000, (int)volume*5);
			
			world.playSound(null, new BlockPos(center), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1, 1f);
			
			world.spawnParticle(ParticleTypes.EXPLOSION, center.getX(), center.getY(), center.getZ(), particles, xRadius, yRadius, zRadius, 0);
		
		}
	}
	
	private static boolean canBlockBeStored(ItemUseContext context, BlockPos pos)
	{
		World world = context.getWorld();
		BlockState state = world.getBlockState(pos);
		PlayerEntity player = context.getPlayer();
		return !state.isAir(context.getWorld(), pos)
			&& !World.isOutsideBuildHeight(pos)
			&& isBlockYurtingAllowedByTags(player, state, pos)
			&& doesBreakEventSucceed(world, pos, state, player);
	}
	
	/** 
	 * Returns true if the whitelist/blacklist do not forbid the block from being yurted, or if the player
	 * has sufficient permission to ignore these, or if the player is creative mode
	 */
	private static boolean isBlockYurtingAllowedByTags(@Nullable PlayerEntity player, BlockState state, BlockPos pos)
	{
		if (player != null && (player.isCreative() ||player.hasPermissionLevel(Config.INSTANCE.minPermissionToYurtUnyurtableBlocks.get())))
		{
			return true;
		}
		else
		{
			Block block = state.getBlock();
			// tags allow this block if the block isn't blacklisted, and if either the whitelist contains the block or is empty
			return (!TagWrappers.blacklist.contains(block))
				&& (TagWrappers.whitelist.getAllElements().isEmpty() || TagWrappers.whitelist.contains(block));
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
		PlayerEntity eventPlayer = player != null ? player : FakePlayerFactory.getMinecraft((ServerWorld)world);
		BreakEvent event = new BreakEvent(world, pos, state, eventPlayer);
		MinecraftForge.EVENT_BUS.post(event);
		return !event.isCanceled();
	}
	
	private static boolean doesPlaceEventSucceed(ItemUseContext context, World world, PlayerEntity player, Map<BlockPos, StateData> worldPositions)
	{
		if (!(world instanceof ServerWorld))
		{
			return false;
		}
		// item use context can have null player
		// but break event requires nonnull player
		// so we use a fake player if player is null
		PlayerEntity eventPlayer = player != null ? player : FakePlayerFactory.getMinecraft((ServerWorld)world);
		List<BlockSnapshot> snapshots = worldPositions.keySet().stream()
			.map(pos -> BlockSnapshot.create(world, pos))
			.collect(Collectors.toList());
		
		
		BlockState statePlacedAgainst = world.getBlockState(context.getPos());
		EntityMultiPlaceEvent event = new EntityMultiPlaceEvent(snapshots, statePlacedAgainst, eventPlayer);
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
	private static BlockPos transformBlockPos(Direction orientation, BlockPos pos, BlockPos origin)
	{		
		BlockPos offset = pos.subtract(origin);	// the difference between the given pos and the yurt origin

		return offset.rotate(RotationUtil.getTransformRotation(orientation));
	}
	
	private static BlockPos untransformBlockPos(Rotation unrotation, BlockPos offset, BlockPos origin)
	{
		BlockPos unRotatedOffset = offset.rotate(unrotation);
		
		return origin.add(unRotatedOffset);
	}
	
	/** The position here is the untransformed position whose data is to be stored **/
	private static StateData getPosEntryAndRemoveBlock(ItemUseContext context, BlockPos pos)
	{
		CompoundNBT nbt = new CompoundNBT();
		World world = context.getWorld();
		BlockState state = world.getBlockState(pos);
		TileEntity te = world.getTileEntity(pos);
		Rotation rotation = RotationUtil.getTransformRotation(context.getPlacementHorizontalFacing());
		if (te != null)
		{
			te.write(nbt);
		}
		world.removeTileEntity(pos);
		world.setBlockState(pos, Blocks.AIR.getDefaultState(), 0);	// don't notify block update on remove
		return new StateData(state.rotate(rotation), nbt);
		
	}
	
	private static boolean canBlockBeUnloadedAt(BlockPos pos, World world, @Nonnull PlayerEntity player)
	{
		if (player != null && player.isCreative() || player.hasPermissionLevel(Config.INSTANCE.minPermissionToYurtUnyurtableBlocks.get()))
		{
			return true;
		}
		else
		{
			BlockState oldState = world.getBlockState(pos);
			return oldState.isAir(world, pos)
				|| TagWrappers.replaceable.contains(oldState.getBlock())
				|| oldState.getMaterial().isReplaceable();
		}
	}
	
	public static boolean doesNBTContainYurtData(CompoundNBT nbt)
	{
		return !nbt.getList(NBT_KEY, 10).isEmpty();
	}
	
	public boolean isEmpty()
	{
		return this.map.isEmpty();
	}
	
	public CompoundNBT writeIntoNBT(CompoundNBT nbt)
	{
		mapper.write(this.map, nbt);
		return nbt;
	}
	
	/** Creates a new instance from an NBT compound. This assumes that the given nbt has the "yurt" key within **/ 
	public static BagOfYurtingData read(CompoundNBT nbt)
	{
		return new BagOfYurtingData(mapper.read(nbt));
	}
	
	public static class StateData
	{
		public static final String BLOCKSTATE = "state";
		public static final String TILE ="te";
		
		private final @Nonnull BlockState state;
		private final @Nonnull CompoundNBT tileEntityData;
		
		public StateData(@Nonnull BlockState state, @Nonnull CompoundNBT tileEntityData)
		{
			this.state = state;
			this.tileEntityData = tileEntityData;
		}
		
		public BlockState getState()
		{
			return this.state;
		}
		
		public void setBlockAndTE(World world, BlockPos pos, Rotation unrotation)
		{
			world.setBlockState(pos, this.state.rotate(unrotation));
			
			if (!this.tileEntityData.isEmpty())
			{
				TileEntity te = world.getTileEntity(pos);
				if (te != null)
				{
					te.read(this.state, this.tileEntityData);
					// copying the data like this also overwrites the pos
					te.setWorldAndPos(world, pos);
				}
			}
		}
		
		public CompoundNBT write()
		{
			CompoundNBT nbt = new CompoundNBT();
			
			nbt.put(BLOCKSTATE, NBTUtil.writeBlockState(this.state));
			nbt.put(TILE, this.tileEntityData);
			
			return nbt;
		}
		
		public static StateData read(CompoundNBT nbt)
		{
			BlockState state = NBTUtil.readBlockState(nbt.getCompound(BLOCKSTATE));
			CompoundNBT te = nbt.getCompound(TILE);
			
			return new StateData(state, te);
		}
	}
}

package commoble.bagofyurting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import commoble.bagofyurting.CompressedBagOfYurtingData.CompressedStateData;
import commoble.bagofyurting.api.BagOfYurtingAPI;
import commoble.bagofyurting.api.BlockDataDeserializer;
import commoble.bagofyurting.api.BlockDataSerializer;
import commoble.bagofyurting.api.RotationUtil;
import commoble.bagofyurting.api.internal.DataTransformers;
import commoble.bagofyurting.util.NBTMapHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityMultiPlaceEvent;

public class BagOfYurtingData
{
	public static final String NBT_KEY = "yurtdata"; // For compat reasons
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
		PlayerEntity player = context.getPlayer();
		boolean canPlayerOverrideSafetyLists = canPlayerOverrideSafetyLists(player);
		// this is the block that the player used the item on
		BlockPos origin = context.getPos();
		Direction orientation = context.getPlacementHorizontalFacing();
		World world = context.getWorld();
		Rotation rotation = RotationUtil.getTransformRotation(orientation);

		BlockPos minYurt = origin.add(-radius, 0, -radius);
		BlockPos maxYurt = origin.add(radius, 2*radius, radius);

		// map each position in the loading area to a rotated offset from the player
		// don't get any blocks that we aren't allowed to get

		// we need to change this up a bit to make the API work better
		// currently, we write block data into yurt data when we remove each block, one at a time
		// we want to first read all the relevant data, *then* remove all the blocks
		List<Pair<BlockPos, Pair<BlockPos, StateData>>> transformPairs = BlockPos.getAllInBox(minYurt, maxYurt) // get every pos in yurt zone
			.filter(pos -> canBlockBeStored(canPlayerOverrideSafetyLists, context, pos)) // only get the blocks the player can yurt
			.map(BlockPos::toImmutable) // make sure positions are immutable since we're using them as map keys
			.sorted(new BlockRemovalSorter(world))
			.map(pos -> getTransformedPosAndStateData(world, pos, rotation, minYurt, maxYurt, origin))
			.collect(Collectors.toList());

		// now that we have all of the state data, we can safely remove the blocks
		BlockState air = Blocks.AIR.getDefaultState();
		transformPairs.forEach(pair ->
		{
			BlockPos pos = pair.getLeft();
			world.removeTileEntity(pos);
			world.setBlockState(pos, air, 0); // don't cause block updates until all of the blocks have been removed
		});

		List<BlockPos> removedPositions = transformPairs.stream().map(Pair::getLeft).collect(Collectors.toList());
		Map<BlockPos, StateData> transformedData = transformPairs.stream()
			.map(Pair::getRight)
			.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

		if (transformPairs.size() > 0 && world instanceof ServerWorld)
		{
			doPoofEffects((ServerWorld) world, removedPositions);

			// we don't cause block updates while removing blocks, wait until the end and then notify all blocks at once
			for (Pair<BlockPos, Pair<BlockPos, StateData>> entry : transformPairs)
			{
				BlockPos pos = entry.getKey();
				BlockState oldState = entry.getValue().getRight().state;
				sendBlockUpdateAfterRemoval(world, pos, oldState);
			}
		}

		return new BagOfYurtingData(transformedData);
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
		boolean canPlayerOverrideSafetyLists = canPlayerOverrideSafetyLists(player);
		
		// we want to do things in this order:
			// collect all of the data to place
			// make sure ALL of the data is placeable (don't unload the bag if we can't)
			// set all of the blocks at once
			// write all of the extant TE data

		// collect all of the data to place
		Map<BlockPos, StateData> worldPositions = this.map.entrySet()
			.stream()
			.collect(Collectors.toMap(
				entry -> RotationUtil.untransformBlockPos(unrotation, entry.getKey(), origin), // untransform transformed offset
				entry -> entry.getValue()));

		// make sure all blocks we want to place are placeable
		boolean success = worldPositions.entrySet().stream()
			.allMatch(entry -> canBlockBeUnloadedAt(canPlayerOverrideSafetyLists, entry.getKey(), world))
			&& doesPlaceEventSucceed(context, world, player, worldPositions);

		if (success)
		{
			BlockPos minYurt = origin.add(-radius,0,-radius);
			BlockPos maxYurt = origin.add(radius,2*radius,radius);
			
			// set all of the blocks at once, then set all of the block entity data at once
			List<Entry<BlockPos, StateData>> worldPositionList = worldPositions.entrySet().stream()
				.sorted(BlockUnloadSorter.INSTANCE)
				.collect(Collectors.toList());
			
			worldPositionList.forEach(entry -> entry.getValue().setBlockIntoWorld(world, entry.getKey(), unrotation));
			worldPositionList.forEach(entry -> entry.getValue().setBlockEntityData(world, entry.getKey(), unrotation, minYurt, maxYurt, origin));

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

			OptionalSpawnParticlePacket.spawnParticlesFromServer(world, ParticleTypes.EXPLOSION, center.getX(), center.getY(), center.getZ(), particles, xRadius, yRadius, zRadius, 0);

		}
	}
	
	private static boolean canPlayerOverrideSafetyLists(@Nullable PlayerEntity player)
	{
		if (player != null && (player.isCreative() || player.hasPermissionLevel(ServerConfig.INSTANCE.minPermissionToYurtUnyurtableBlocks.get())))
		{
			return TransientPlayerData.isPlayerOverridingSafetyList(PlayerEntity.getUUID(player.getGameProfile()));
		}
		else
		{
			return false;
		}
	}

	private static boolean canBlockBeStored(boolean canPlayerOverrideSafetyLists, ItemUseContext context, BlockPos pos)
	{
		World world = context.getWorld();
		BlockState state = world.getBlockState(pos);
		PlayerEntity player = context.getPlayer();
		return !state.isAir(context.getWorld(), pos)
			&& !World.isOutsideBuildHeight(pos)
			&& isBlockYurtingAllowedByTags(canPlayerOverrideSafetyLists, state, pos)
			&& doesBreakEventSucceed(world, pos, state, player);
	}

	/**
	 * Returns true if the whitelist/blacklist do not forbid the block from being yurted, or if the player
	 * has sufficient permission to ignore these, or if the player is creative mode
	 */
	private static boolean isBlockYurtingAllowedByTags(boolean canPlayerOverrideSafetyLists, BlockState state, BlockPos pos)
	{
		if (canPlayerOverrideSafetyLists)
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
			.map(pos -> BlockSnapshot.create(world.getDimensionKey(), world, pos))
			.collect(Collectors.toList());


		BlockState statePlacedAgainst = world.getBlockState(context.getPos());
		EntityMultiPlaceEvent event = new EntityMultiPlaceEvent(snapshots, statePlacedAgainst, eventPlayer);
		MinecraftForge.EVENT_BUS.post(event);
		return !event.isCanceled();
	}

//	/** The position here is the untransformed position whose data is to be stored **/
//	@SuppressWarnings("unchecked")
//	private static StateData getPosEntryAndRemoveBlock(ItemUseContext context, BlockPos pos, BlockPos minYurt, BlockPos maxYurt)
//	{
//		CompoundNBT nbt = new CompoundNBT();
//		World world = context.getWorld();
//		BlockState state = world.getBlockState(pos);
//		TileEntity te = world.getTileEntity(pos);
//		Rotation rotation = RotationUtil.getTransformRotation(context.getPlacementHorizontalFacing());
//		if (te != null)
//		{
//			@SuppressWarnings("rawtypes")
//			// need raw type for this to compile, type correctness has been enforced elsewhere
//			BlockDataSerializer serializer = DataTransformers.transformers.getOrDefault(te.getType(), BagOfYurtingAPI.DEFAULT_TRANSFORMER)
//				.getSerializer();
//			serializer.writeWithYurtContext(te, nbt, rotation, minYurt, maxYurt);
//		}
//		world.removeTileEntity(pos);
//		world.setBlockState(pos, Blocks.AIR.getDefaultState(), 0);	// don't notify block update on remove
//		return new StateData(state.rotate(rotation), nbt);
//
//	}
	
	private static Pair<BlockPos, Pair<BlockPos, StateData>> getTransformedPosAndStateData(IWorld world, BlockPos absolutePos, Rotation rotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin)
	{
		BlockPos transformedPos = RotationUtil.transformBlockPos(rotation, absolutePos, origin);
		StateData stateData = getYurtedStateData(world, absolutePos, rotation, minYurt, maxYurt, origin, transformedPos);
		return Pair.of(absolutePos, Pair.of(transformedPos, stateData));
	}
	
	@SuppressWarnings("unchecked")
	private static StateData getYurtedStateData(IWorld world, BlockPos pos, Rotation rotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin, BlockPos transformedPos)
	{
		CompoundNBT nbt = new CompoundNBT();
		BlockState state = world.getBlockState(pos);
		BlockState rotatedState = state.rotate(world, pos, rotation);
		TileEntity te = world.getTileEntity(pos);
		if (te != null)
		{
			@SuppressWarnings("rawtypes")
			// need raw type for this to compile, type correctness has been enforced elsewhere
			BlockDataSerializer serializer = DataTransformers.transformers.getOrDefault(te.getType(), BagOfYurtingAPI.DEFAULT_TRANSFORMER)
				.getSerializer();
			serializer.writeWithYurtContext(te, nbt, rotation, minYurt, maxYurt, origin, transformedPos);
		}
		return new StateData(rotatedState, nbt);
	}

	private static boolean canBlockBeUnloadedAt(boolean canPlayerOverrideSafetyLists, BlockPos pos, World world)
	{
		if (canPlayerOverrideSafetyLists)
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
	
	public CompressedBagOfYurtingData compress()
	{
		final Object2IntMap<BlockState> indexMap = new Object2IntOpenHashMap<>();
		final List<BlockState> states = new ArrayList<>();
		final List<CompressedStateData> data = new ArrayList<>();
		
		this.map.forEach((pos,stateData) ->
		{
			final BlockState state = stateData.state;
			@Nullable CompoundNBT nbt = stateData.tileEntityData;
			// uncompressed (old) format uses empty nbts if no data is present, compressed format uses optionals
			final Optional<CompoundNBT> optionalNBT = nbt == null || nbt.isEmpty() ? Optional.empty() : Optional.of(nbt);
			// if we haven't stored this specific blockstate yet,
			// then add it to the state-list-pallette, and use its index in the list as that index
			final int index = indexMap.computeIfAbsent(state, newState ->
			{
				int newIndex = states.size();
				states.add(state);
				return newIndex;
			});
			
			CompressedStateData compressedData = new CompressedStateData(pos, index, optionalNBT);
			data.add(compressedData);
		});
		
		return new CompressedBagOfYurtingData(states,data);
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
		
		public void setBlockIntoWorld(World world, BlockPos pos, Rotation unrotation)
		{
			world.setBlockState(pos, this.state.rotate(unrotation));
		}
		
		@SuppressWarnings("unchecked")
		public void setBlockEntityData(World world, BlockPos pos, Rotation unrotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin)
		{
			if (!this.tileEntityData.isEmpty())
			{
				TileEntity te = world.getTileEntity(pos);
				if (te != null)
				{
					// need raw types to get the data transformer to compile here
					@SuppressWarnings("rawtypes")
					BlockDataDeserializer x = DataTransformers.transformers.getOrDefault(te.getType(), BagOfYurtingAPI.DEFAULT_TRANSFORMER)
						.getDeserializer();
					x.readWithYurtContext(te, this.tileEntityData, world, pos, this.state, unrotation, minYurt, maxYurt, origin);
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

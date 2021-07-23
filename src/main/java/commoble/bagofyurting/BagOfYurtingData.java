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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.EntityMultiPlaceEvent;

public class BagOfYurtingData
{
	public static final String NBT_KEY = "yurtdata"; // For compat reasons
	public static final Direction BASE_DIRECTION = Direction.SOUTH;	// south is arbitrarily chosen for having horizontal index 0

	private static final NBTMapHelper<BlockPos, CompoundTag, StateData, CompoundTag> mapper = new NBTMapHelper<>(
		NBT_KEY,
		NbtUtils::writeBlockPos,
		NbtUtils::readBlockPos,
		StateData::write,
		StateData::read);

	private final Map<BlockPos, StateData> map;

	public BagOfYurtingData(Map<BlockPos, StateData> map)
	{
		this.map = map;
	}

	/** Removes blocks from the Level and stores them as yurt data, returning the data **/
	public static BagOfYurtingData yurtBlocksAndConvertToData(UseOnContext context, int radius)
	{
		Player player = context.getPlayer();
		boolean canPlayerOverrideSafetyLists = canPlayerOverrideSafetyLists(player);
		// this is the block that the player used the item on
		BlockPos origin = context.getClickedPos();
		Direction orientation = context.getHorizontalDirection();
		Level level = context.getLevel();
		Rotation rotation = RotationUtil.getTransformRotation(orientation);

		BlockPos minYurt = origin.offset(-radius, 0, -radius);
		BlockPos maxYurt = origin.offset(radius, 2*radius, radius);

		// map each position in the loading area to a rotated offset from the player
		// don't get any blocks that we aren't allowed to get

		// we need to change this up a bit to make the API work better
		// currently, we write block data into yurt data when we remove each block, one at a time
		// we want to first read all the relevant data, *then* remove all the blocks
		List<Pair<BlockPos, Pair<BlockPos, StateData>>> transformPairs = BlockPos.betweenClosedStream(minYurt, maxYurt) // get every pos in yurt zone
			.filter(pos -> canBlockBeStored(canPlayerOverrideSafetyLists, context, pos)) // only get the blocks the player can yurt
			.map(BlockPos::immutable) // make sure positions are immutable since we're using them as map keys
			.sorted(new BlockRemovalSorter(level))
			.map(pos -> getTransformedPosAndStateData(level, pos, rotation, minYurt, maxYurt, origin))
			.collect(Collectors.toList());

		// now that we have all of the state data, we can safely remove the blocks
		BlockState air = Blocks.AIR.defaultBlockState();
		transformPairs.forEach(pair ->
		{
			BlockPos pos = pair.getLeft();
			level.removeBlockEntity(pos);
			level.setBlock(pos, air, 0); // don't cause block updates until all of the blocks have been removed
		});

		List<BlockPos> removedPositions = transformPairs.stream().map(Pair::getLeft).collect(Collectors.toList());
		Map<BlockPos, StateData> transformedData = transformPairs.stream()
			.map(Pair::getRight)
			.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

		if (transformPairs.size() > 0 && level instanceof ServerLevel serverLevel)
		{
			doPoofEffects(serverLevel, removedPositions);

			// we don't cause block updates while removing blocks, wait until the end and then notify all blocks at once
			for (Pair<BlockPos, Pair<BlockPos, StateData>> entry : transformPairs)
			{
				BlockPos pos = entry.getKey();
				BlockState oldState = entry.getValue().getRight().state;
				sendBlockUpdateAfterRemoval(level, pos, oldState);
			}
		}

		return new BagOfYurtingData(transformedData);
	}

	private static void sendBlockUpdateAfterRemoval(Level level, BlockPos pos, BlockState oldState)
	{
		level.sendBlockUpdated(pos, oldState, Blocks.AIR.defaultBlockState(), 3);
		level.updateNeighborsAt(pos, oldState.getBlock());
	}

	public boolean attemptUnloadIntoLevel(UseOnContext context, int radius)
	{
		Level level = context.getLevel();

		// this is the block adjacent to the face that the player used the item on
		// unless the block we use it on is replaceable
		// in which case the origin is that block
		BlockPos hitPos = context.getClickedPos();
		boolean hitBlockReplaceable = (level.getBlockState(hitPos).getMaterial().isReplaceable());
		BlockPos origin = hitBlockReplaceable ? hitPos : hitPos.relative(context.getClickedFace());
		Direction orientation = context.getHorizontalDirection();
		Rotation unrotation = RotationUtil.getUntransformRotation(orientation);
		Player player = context.getPlayer();
		boolean canPlayerOverrideSafetyLists = canPlayerOverrideSafetyLists(player);
		
		// we want to do things in this order:
			// collect all of the data to place
			// make sure ALL of the data is placeable (don't unload the bag if we can't)
			// set all of the blocks at once
			// write all of the extant TE data

		// collect all of the data to place
		Map<BlockPos, StateData> LevelPositions = this.map.entrySet()
			.stream()
			.collect(Collectors.toMap(
				entry -> RotationUtil.untransformBlockPos(unrotation, entry.getKey(), origin), // untransform transformed offset
				entry -> entry.getValue()));

		// make sure all blocks we want to place are placeable
		boolean success = LevelPositions.entrySet().stream()
			.allMatch(entry -> canBlockBeUnloadedAt(canPlayerOverrideSafetyLists, entry.getKey(), level))
			&& doesPlaceEventSucceed(context, level, player, LevelPositions);

		if (success)
		{
			BlockPos minYurt = origin.offset(-radius,0,-radius);
			BlockPos maxYurt = origin.offset(radius,2*radius,radius);
			
			// set all of the blocks at once, then set all of the block entity data at once
			List<Entry<BlockPos, StateData>> LevelPositionList = LevelPositions.entrySet().stream()
				.sorted(BlockUnloadSorter.INSTANCE)
				.collect(Collectors.toList());
			
			LevelPositionList.forEach(entry -> entry.getValue().setBlockIntoLevel(level, entry.getKey(), unrotation));
			LevelPositionList.forEach(entry -> entry.getValue().setBlockEntityData(level, entry.getKey(), unrotation, minYurt, maxYurt, origin));

			if (level instanceof ServerLevel)
			{
				doPoofEffects((ServerLevel)level, LevelPositions.keySet());
			}
		}

		return success;
	}

	public static final AABB EMPTY_AABB = new AABB(0,0,0,0,0,0);

	private static void doPoofEffects(ServerLevel Level, Collection<BlockPos> changedPositions)
	{
		AABB aabb = changedPositions.stream()
			.map(AABB::new)
			.reduce(AABB::minmax)
			.orElse(EMPTY_AABB);
		if (aabb.getSize() > 0.5D)
		{
			Vec3 center = aabb.getCenter();
			double xRadius = aabb.getXsize()*0.5;
			double yRadius = aabb.getYsize()*0.5;
			double zRadius = aabb.getZsize()*0.5;

			double volume = xRadius * yRadius * zRadius * 8D;

			int particles = Math.max(5000, (int)volume*5);

			Level.playSound(null, new BlockPos(center), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1, 1f);

			OptionalSpawnParticlePacket.spawnParticlesFromServer(Level, ParticleTypes.EXPLOSION, center.x(), center.y(), center.z(), particles, xRadius, yRadius, zRadius, 0);

		}
	}
	
	private static boolean canPlayerOverrideSafetyLists(@Nullable Player player)
	{
		if (player != null && (player.isCreative() || player.hasPermissions(ServerConfig.INSTANCE.minPermissionToYurtUnyurtableBlocks.get())))
		{
			return TransientPlayerData.isPlayerOverridingSafetyList(Player.createPlayerUUID(player.getGameProfile()));
		}
		else
		{
			return false;
		}
	}

	private static boolean canBlockBeStored(boolean canPlayerOverrideSafetyLists, UseOnContext context, BlockPos pos)
	{
		Level Level = context.getLevel();
		BlockState state = Level.getBlockState(pos);
		Player player = context.getPlayer();
		return !state.isAir()
			&& !Level.isOutsideBuildHeight(pos)
			&& isBlockYurtingAllowedByTags(canPlayerOverrideSafetyLists, state, pos)
			&& doesBreakEventSucceed(Level, pos, state, player);
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
				&& (TagWrappers.whitelist.getValues().isEmpty() || TagWrappers.whitelist.contains(block));
		}
	}

	private static boolean doesBreakEventSucceed(Level Level, BlockPos pos, BlockState state, Player player)
	{
		if (!(Level instanceof ServerLevel))
		{
			return false;
		}
		// item use context can have null player
		// but break event requires nonnull player
		// so we use a fake player if player is null
		Player eventPlayer = player != null ? player : FakePlayerFactory.getMinecraft((ServerLevel)Level);
		BreakEvent event = new BreakEvent(Level, pos, state, eventPlayer);
		MinecraftForge.EVENT_BUS.post(event);
		return !event.isCanceled();
	}

	private static boolean doesPlaceEventSucceed(UseOnContext context, Level level, Player player, Map<BlockPos, StateData> levelPositions)
	{
		if (!(level instanceof ServerLevel))
		{
			return false;
		}
		// item use context can have null player
		// but break event requires nonnull player
		// so we use a fake player if player is null
		Player eventPlayer = player != null ? player : FakePlayerFactory.getMinecraft((ServerLevel)level);
		List<BlockSnapshot> snapshots = levelPositions.keySet().stream()
			.map(pos -> BlockSnapshot.create(level.dimension(), level, pos))
			.collect(Collectors.toList());


		BlockState statePlacedAgainst = level.getBlockState(context.getClickedPos());
		EntityMultiPlaceEvent event = new EntityMultiPlaceEvent(snapshots, statePlacedAgainst, eventPlayer);
		MinecraftForge.EVENT_BUS.post(event);
		return !event.isCanceled();
	}

//	/** The position here is the untransformed position whose data is to be stored **/
//	@SuppressWarnings("unchecked")
//	private static StateData getPosEntryAndRemoveBlock(UseOnContext context, BlockPos pos, BlockPos minYurt, BlockPos maxYurt)
//	{
//		CompoundTag nbt = new CompoundTag();
//		Level Level = context.getLevel();
//		BlockState state = Level.getBlockState(pos);
//		BlockEntity te = Level.getBlockEntity(pos);
//		Rotation rotation = RotationUtil.getTransformRotation(context.getPlacementHorizontalFacing());
//		if (te != null)
//		{
//			@SuppressWarnings("rawtypes")
//			// need raw type for this to compile, type correctness has been enforced elsewhere
//			BlockDataSerializer serializer = DataTransformers.transformers.getOrDefault(te.getType(), BagOfYurtingAPI.DEFAULT_TRANSFORMER)
//				.getSerializer();
//			serializer.writeWithYurtContext(te, nbt, rotation, minYurt, maxYurt);
//		}
//		Level.removeBlockEntity(pos);
//		Level.setBlockState(pos, Blocks.AIR.getDefaultState(), 0);	// don't notify block update on remove
//		return new StateData(state.rotate(rotation), nbt);
//
//	}
	
	private static Pair<BlockPos, Pair<BlockPos, StateData>> getTransformedPosAndStateData(LevelAccessor Level, BlockPos absolutePos, Rotation rotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin)
	{
		BlockPos transformedPos = RotationUtil.transformBlockPos(rotation, absolutePos, origin);
		StateData stateData = getYurtedStateData(Level, absolutePos, rotation, minYurt, maxYurt, origin, transformedPos);
		return Pair.of(absolutePos, Pair.of(transformedPos, stateData));
	}
	
	@SuppressWarnings("unchecked")
	private static StateData getYurtedStateData(LevelAccessor Level, BlockPos pos, Rotation rotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin, BlockPos transformedPos)
	{
		CompoundTag nbt = new CompoundTag();
		BlockState state = Level.getBlockState(pos);
		BlockState rotatedState = state.rotate(Level, pos, rotation);
		BlockEntity te = Level.getBlockEntity(pos);
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

	private static boolean canBlockBeUnloadedAt(boolean canPlayerOverrideSafetyLists, BlockPos pos, Level Level)
	{
		if (canPlayerOverrideSafetyLists)
		{
			return true;
		}
		else
		{
			BlockState oldState = Level.getBlockState(pos);
			return oldState.isAir()
				|| TagWrappers.replaceable.contains(oldState.getBlock())
				|| oldState.getMaterial().isReplaceable();
		}
	}

	public static boolean doesNBTContainYurtData(CompoundTag nbt)
	{
		return !nbt.getList(NBT_KEY, 10).isEmpty();
	}

	public boolean isEmpty()
	{
		return this.map.isEmpty();
	}

	public CompoundTag writeIntoNBT(CompoundTag nbt)
	{
		mapper.write(this.map, nbt);
		return nbt;
	}

	/** Creates a new instance from an NBT compound. This assumes that the given nbt has the "yurt" key within **/
	public static BagOfYurtingData read(CompoundTag nbt)
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
			@Nullable CompoundTag nbt = stateData.BlockEntityData;
			// uncompressed (old) format uses empty nbts if no data is present, compressed format uses optionals
			final Optional<CompoundTag> optionalNBT = nbt == null || nbt.isEmpty() ? Optional.empty() : Optional.of(nbt);
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
		private final @Nonnull CompoundTag BlockEntityData;

		public StateData(@Nonnull BlockState state, @Nonnull CompoundTag BlockEntityData)
		{
			this.state = state;
			this.BlockEntityData = BlockEntityData;
		}

		public BlockState getState()
		{
			return this.state;
		}
		
		public void setBlockIntoLevel(Level level, BlockPos pos, Rotation unrotation)
		{
			level.setBlockAndUpdate(pos, this.state.rotate(unrotation));
		}
		
		@SuppressWarnings("unchecked")
		public void setBlockEntityData(Level level, BlockPos pos, Rotation unrotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin)
		{
			if (!this.BlockEntityData.isEmpty())
			{
				BlockEntity te = level.getBlockEntity(pos);
				if (te != null)
				{
					// need raw types to get the data transformer to compile here
					@SuppressWarnings("rawtypes")
					BlockDataDeserializer x = DataTransformers.transformers.getOrDefault(te.getType(), BagOfYurtingAPI.DEFAULT_TRANSFORMER)
						.getDeserializer();
					x.readWithYurtContext(te, this.BlockEntityData, level, pos, this.state, unrotation, minYurt, maxYurt, origin);
					te.setLevel(level);
				}
			}
		}

		public CompoundTag write()
		{
			CompoundTag nbt = new CompoundTag();

			nbt.put(BLOCKSTATE, NbtUtils.writeBlockState(this.state));
			nbt.put(TILE, this.BlockEntityData);

			return nbt;
		}

		public static StateData read(CompoundTag nbt)
		{
			BlockState state = NbtUtils.readBlockState(nbt.getCompound(BLOCKSTATE));
			CompoundTag te = nbt.getCompound(TILE);

			return new StateData(state, te);
		}
	}
}

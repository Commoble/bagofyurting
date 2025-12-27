package net.commoble.bagofyurting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.bagofyurting.BagOfYurtingData.StateData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.state.BlockState;

public record CompressedBagOfYurtingData(List<BlockState> states, List<CompressedStateData> data)
{
	public static final Logger LOGGER = LogManager.getLogger();
	
	public static final Codec<CompressedBagOfYurtingData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			BlockState.CODEC.listOf().fieldOf("states").forGetter(CompressedBagOfYurtingData::states),
			CompressedStateData.CODEC.listOf().fieldOf("data").forGetter(CompressedBagOfYurtingData::data)
		).apply(builder, CompressedBagOfYurtingData::new));
	
	public BagOfYurtingData uncompress()
	{
		final Map<BlockPos, StateData> map = new HashMap<>();
		this.data.forEach(compressedData ->
		{
			final BlockPos pos = compressedData.pos;
			final int index = compressedData.stateIndex;
			final BlockState state = this.states.get(index);
			// compressed data uses empty optionals for empty nbt, uncompressed uses empty CompoundTags
			final CompoundTag nbt = compressedData.getNBT()
				.orElseGet(CompoundTag::new);
			StateData data = new StateData(state, nbt);
			map.put(pos, data);
		});
		
		return new BagOfYurtingData(map);
	}
	
	public CompoundTag toNBT()
	{
		// RecordCodecBuilder codecs encode as CompoundTag, so the cast is safe enough
		return (CompoundTag)CompressedBagOfYurtingData.CODEC.encodeStart(NbtOps.INSTANCE, this)
			// if encode fails, resultOrPartial logs the error and returns an empty optional
			.resultOrPartial(LOGGER::error)
			.orElseGet(CompoundTag::new);
	}
	
	/**
	 * Reads data from nbt, logs an error if it fails to do so
	 * @param nbt The nbt to read from
	 * @return A present data object if successful, or an empty data  if it failed
	 */
	public static CompressedBagOfYurtingData fromNBT(CompoundTag nbt)
	{
		return CompressedBagOfYurtingData.CODEC.parse(NbtOps.INSTANCE, nbt)
			.resultOrPartial(LOGGER::error)
			.orElseGet(() -> new CompressedBagOfYurtingData(new ArrayList<>(), new ArrayList<>()));
	}
	
	public static class CompressedStateData
	{
		public static final Codec<CompressedStateData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(CompressedStateData::getPos),
				Codec.INT.fieldOf("state").forGetter(CompressedStateData::getStateIndex),
				CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(CompressedStateData::getNBT)
			).apply(builder, CompressedStateData::new));
		
		private final BlockPos pos;
		public BlockPos getPos() { return this.pos; }
		
		private final int stateIndex;
		public int getStateIndex() { return this.stateIndex; }
		
		private final Optional<CompoundTag> nbt;
		public Optional<CompoundTag> getNBT() { return this.nbt; }
		
		public CompressedStateData(BlockPos pos, int stateIndex, Optional<CompoundTag> nbt)
		{
			this.pos = pos;
			this.stateIndex = stateIndex;
			this.nbt = nbt;
		}
	}
}

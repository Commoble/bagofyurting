package commoble.bagofyurting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.bagofyurting.BagOfYurtingData.StateData;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.math.BlockPos;

public class CompressedBagOfYurtingData
{
	public static final Logger LOGGER = LogManager.getLogger();
	
	public static final Codec<CompressedBagOfYurtingData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			BlockState.CODEC.listOf().fieldOf("states").forGetter(CompressedBagOfYurtingData::getStates),
			CompressedStateData.CODEC.listOf().fieldOf("data").forGetter(CompressedBagOfYurtingData::getData)
		).apply(builder, CompressedBagOfYurtingData::new));
	
	private final List<BlockState> states;
	public List<BlockState> getStates() { return this.states; }
	
	private final List<CompressedStateData> data;
	public List<CompressedStateData> getData() { return this.data; }
	
	/**
	 * @param states A list of blockstates
	 * @param data A list of CompressedStateDatas, where the stateindex of each data object refers to an index in the states list
	 */
	public CompressedBagOfYurtingData(List<BlockState> states, List<CompressedStateData> data)
	{
		this.states = states;
		this.data = data;
	}
	
	public BagOfYurtingData uncompress()
	{
		final Map<BlockPos, StateData> map = new HashMap<>();
		this.data.forEach(compressedData ->
		{
			final BlockPos pos = compressedData.pos;
			final int index = compressedData.stateIndex;
			final BlockState state = this.states.get(index);
			// compressed data uses empty optionals for empty nbt, uncompressed uses empty compoundnbts
			final CompoundNBT nbt = compressedData.getNBT()
				.orElseGet(CompoundNBT::new);
			StateData data = new StateData(state, nbt);
			map.put(pos, data);
		});
		
		return new BagOfYurtingData(map);
	}
	
	public CompoundNBT toNBT()
	{
		// RecordCodecBuilder codecs encode as CompoundNBT, so the cast is safe enough
		return (CompoundNBT)CompressedBagOfYurtingData.CODEC.encodeStart(NBTDynamicOps.INSTANCE, this)
			// if encode fails, resultOrPartial logs the error and returns an empty optional
			.resultOrPartial(LOGGER::error)
			.orElseGet(CompoundNBT::new);
	}
	
	/**
	 * Reads data from nbt, logs an error if it fails to do so
	 * @param nbt The nbt to read from
	 * @return A present data object if successful, or an empty data  if it failed
	 */
	public static CompressedBagOfYurtingData fromNBT(CompoundNBT nbt)
	{
		return CompressedBagOfYurtingData.CODEC.parse(NBTDynamicOps.INSTANCE, nbt)
			.resultOrPartial(LOGGER::error)
			.orElseGet(() -> new CompressedBagOfYurtingData(new ArrayList<>(), new ArrayList<>()));
	}
	
	public static class CompressedStateData
	{
		public static final Codec<CompressedStateData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(CompressedStateData::getPos),
				Codec.INT.fieldOf("state").forGetter(CompressedStateData::getStateIndex),
				CompoundNBT.CODEC.optionalFieldOf("nbt").forGetter(CompressedStateData::getNBT)
			).apply(builder, CompressedStateData::new));
		
		private final BlockPos pos;
		public BlockPos getPos() { return this.pos; }
		
		private final int stateIndex;
		public int getStateIndex() { return this.stateIndex; }
		
		private final Optional<CompoundNBT> nbt;
		public Optional<CompoundNBT> getNBT() { return this.nbt; }
		
		public CompressedStateData(BlockPos pos, int stateIndex, Optional<CompoundNBT> nbt)
		{
			this.pos = pos;
			this.stateIndex = stateIndex;
			this.nbt = nbt;
		}
	}
}

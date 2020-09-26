package commoble.bagofyurting.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.IntStream;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;

/**
 * 
The MIT License (MIT)

Copyright (c) 2019 Joseph Bettendorff aka "Commoble"

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */

/**
 * Helper class for writing a Map into a CompoundNBT
 * example usage for use in e.g. a WorldSavedData, a TileEntity, etc:
 * 
 * 	private static final String BLOCKS = "blocks";
	private static final String BLOCKPOS = "blockpos";
	private static final String BLOCKSTATE = "blockstate";
	
	private Map<BlockPos, BlockState> map = new HashMap<>();
	
	private static final NBTMapHelper<BlockPos, BlockState> BLOCKS_MAPPER = new NBTMapHelper<>(
			BLOCKS,
			(nbt, blockPos) -> nbt.put(BLOCKPOS, NBTUtil.writeBlockPos(blockPos)),
			nbt -> NBTUtil.readBlockPos(nbt.getCompound(BLOCKPOS)),
			(nbt, blockState) -> nbt.put(BLOCKSTATE, NBTUtil.writeBlockState(blockState)),
			nbt -> NBTUtil.readBlockState(nbt.getCompound(BLOCKSTATE))
			);
			
	@Override
	public void read(CompoundNBT nbt)
	{
		this.map = BLOCKS_MAPPER.read(nbt);
	}

	@Override
	public CompoundNBT write(CompoundNBT compound)
	{
		BLOCKS_MAPPER.write(this.map, compound);
		return compound;
	}
			
			
 * 
 * @author Joseph aka Commoble
 */
public class NBTMapHelper<K, KNBT extends INBT, V, VNBT extends INBT>
{
	private static final String KEY = "k";
	private static final String VALUE = "v";
	
	private final String name;
	private final Function<K, KNBT> keyWriter;
	private final Function<KNBT, K> keyReader;
	private final Function<V, VNBT> valueWriter;
	private final Function<VNBT, V> valueReader;
	
	/**
	 * @param name A unique identifier for the hashmap, allowing the map to be written into a CompoundNBT alongside other data
	 * @param keyWriter A function that, given a Key, returns an nbt object representing that key
	 * @param keyReader A function that, given an nbt object, returns the Key represented by that nbt
	 * @param valueWriter A function that, given a Value, returns an nbt object representing that value
	 * @param valueReader A Function that, given an nbt object, returns the Value represented by that nbt
	 */
	public NBTMapHelper(
			String name,
			Function<K, KNBT> keyWriter,
			Function<KNBT, K> keyReader,
			Function<V, VNBT> valueWriter,
			Function<VNBT, V> valueReader)
	{
		this.name = name;
		this.keyReader = keyReader;
		this.keyWriter = keyWriter;
		this.valueReader = valueReader;
		this.valueWriter = valueWriter;
	}
	
	public boolean hasData(final CompoundNBT nbt)
	{
		return nbt.contains(this.name);
	}
	
	/**
	 * Reconstructs and returns a Map<K,V> from a CompoundNBT
	 * If the nbt used was given by this.write(map), the map returned will be a reconstruction of the original Map
	 * @param nbt The CompoundNBT to read and construct the Map from
	 * @return A Map that the data contained in the CompoundNBT represents
	 */
	public Map<K, V> read(final CompoundNBT nbt)
	{
		final Map<K, V> newMap = new HashMap<>();

		final ListNBT entryList = nbt.getList(this.name, 10);
		if (entryList == null)
			return newMap;
		
		final int entryCount = entryList.size();
		
		if (entryCount <= 0)
			return newMap;

		IntStream.range(0, entryCount).mapToObj(i -> entryList.getCompound(i))
			.forEach(entryNBT -> this.writeEntry(newMap, entryNBT));

		return newMap;
	}
	
	private void writeEntry(Map<K,V> map, CompoundNBT entryNBT)
	{
		@SuppressWarnings("unchecked")
		final K key = this.keyReader.apply((KNBT) entryNBT.get(KEY));
		@SuppressWarnings("unchecked")
		final V value = this.valueReader.apply((VNBT) entryNBT.get(VALUE));
		
		map.put(key, value);
	}
	
	/**
	 * Given a map and a CompoundNBT, writes the map into the NBT
	 * The same compoundNBT can be given to this.read to retrieve that map
	 * @param map A Map<K,V>
	 * @param compound A CompoundNBT to write the map into
	 * @return a CompoundNBT that, when used as the argument to this.read(nbt), will cause that function to reconstruct and return a copy of the original map
	 */
	public CompoundNBT write(final Map<K,V> map, final CompoundNBT compound)
	{
		final ListNBT entryListNBT = new ListNBT();
		map.entrySet().forEach(entry -> this.writeEntry(entryListNBT, entry));
		
		compound.put(this.name, entryListNBT);
		
		return compound;
	}
	
	private void writeEntry(ListNBT entryListNBT, Entry<K,V> entry)
	{
		final CompoundNBT entryNBT = new CompoundNBT();
		entryNBT.put(KEY, this.keyWriter.apply(entry.getKey()));
		entryNBT.put(VALUE, this.valueWriter.apply(entry.getValue()));
		
		entryListNBT.add(entryNBT);
	}
}
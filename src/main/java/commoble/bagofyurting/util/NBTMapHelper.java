package commoble.bagofyurting.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.IntStream;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;

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
 * Helper class for writing a Map into a CompoundTag
 * example usage for use in e.g. a LevelSavedData, a BlockEntity, etc:
 *
<pre><code>
	private static final String BLOCKS = "blocks";
	private static final String BLOCKPOS = "blockpos";
	private static final String BLOCKSTATE = "blockstate";
	
	{@code private Map<BlockPos, BlockState> map = new HashMap<>();}
	
	{@code private static final NBTMapHelper<BlockPos, BlockState> BLOCKS_MAPPER = new NBTMapHelper<>(}
			BLOCKS,
	{@code 		(nbt, blockPos) -> nbt.put(BLOCKPOS, NbtUtils.writeBlockPos(blockPos)),}
	{@code 		nbt -> NbtUtils.readBlockPos(nbt.getCompound(BLOCKPOS)),}
	{@code 		(nbt, blockState) -> nbt.put(BLOCKSTATE, NbtUtils.writeBlockState(blockState)),}
	{@code 		nbt -> NbtUtils.readBlockState(nbt.getCompound(BLOCKSTATE))}
			);
			
	{@literal @}Override
	public void read(CompoundTag nbt)
	{
		this.map = BLOCKS_MAPPER.read(nbt);
	}

	{@literal @}Override
	public CompoundTag write(CompoundTag compound)
	{
		BLOCKS_MAPPER.write(this.map, compound);
		return compound;
	}
</code></pre>
			
 * 
 * @author Joseph aka Commoble
 */
public class NBTMapHelper<K, KNBT extends Tag, V, VNBT extends Tag>
{
	private static final String KEY = "k";
	private static final String VALUE = "v";
	
	private final String name;
	private final Function<K, KNBT> keyWriter;
	private final Function<KNBT, K> keyReader;
	private final Function<V, VNBT> valueWriter;
	private final Function<VNBT, V> valueReader;
	
	/**
	 * @param name A unique identifier for the hashmap, allowing the map to be written into a CompoundTag alongside other data
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
	
	public boolean hasData(final CompoundTag nbt)
	{
		return nbt.contains(this.name);
	}
	
	/**
	 * Reconstructs and returns a {@code Map<K,V>} from a CompoundTag
	 * If the nbt used was given by this.write(map), the map returned will be a reconstruction of the original Map
	 * @param nbt The CompoundTag to read and construct the Map from
	 * @return A Map that the data contained in the CompoundTag represents
	 */
	public Map<K, V> read(final CompoundTag nbt)
	{
		final Map<K, V> newMap = new HashMap<>();

		final ListTag entryList = nbt.getList(this.name, 10);
		if (entryList == null)
			return newMap;
		
		final int entryCount = entryList.size();
		
		if (entryCount <= 0)
			return newMap;

		IntStream.range(0, entryCount).mapToObj(i -> entryList.getCompound(i))
			.forEach(entryNBT -> this.writeEntry(newMap, entryNBT));

		return newMap;
	}
	
	private void writeEntry(Map<K,V> map, CompoundTag entryNBT)
	{
		@SuppressWarnings("unchecked")
		final K key = this.keyReader.apply((KNBT) entryNBT.get(KEY));
		@SuppressWarnings("unchecked")
		final V value = this.valueReader.apply((VNBT) entryNBT.get(VALUE));
		
		map.put(key, value);
	}
	
	/**
	 * Given a map and a CompoundTag, writes the map into the NBT
	 * The same CompoundTag can be given to this.read to retrieve that map
	 * @param map A {@code Map<K,V>}
	 * @param compound A CompoundTag to write the map into
	 * @return a CompoundTag that, when used as the argument to this.read(nbt), will cause that function to reconstruct and return a copy of the original map
	 */
	public CompoundTag write(final Map<K,V> map, final CompoundTag compound)
	{
		final ListTag entryListTag = new ListTag();
		map.entrySet().forEach(entry -> this.writeEntry(entryListTag, entry));
		
		compound.put(this.name, entryListTag);
		
		return compound;
	}
	
	private void writeEntry(ListTag entryListTag, Entry<K,V> entry)
	{
		final CompoundTag entryNBT = new CompoundTag();
		entryNBT.put(KEY, this.keyWriter.apply(entry.getKey()));
		entryNBT.put(VALUE, this.valueWriter.apply(entry.getValue()));
		
		entryListTag.add(entryNBT);
	}
}
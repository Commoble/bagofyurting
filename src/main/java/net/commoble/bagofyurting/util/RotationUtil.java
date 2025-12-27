package net.commoble.bagofyurting.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

public class RotationUtil
{
	/** SOUTH is chosen because it's the 0-indexed direction for Direction's array of the four horizontal directions **/
	public static final Direction BASE_DIRECTION = Direction.SOUTH;
	
	/**
	 * Get the rotation Bag of Yurting uses to serialize data consistently given the horizontal facing of the player
	 * when they use the bag to store blocks in the back
	 * @param orientation The facing of the player at the time of yurting-blocks-into-bag. Must be a horizontal direction.
	 * @return The rotation used to transform positions when serializing them
	 */
	public static Rotation getTransformRotation(Direction orientation)
	{
		return getHorizontalDifferenceRotation(orientation, BASE_DIRECTION);
	}
	
	/**
	 * Get the rotation Bag of Yurting uses to deserialize data consistently given the horizontal facing of the player
	 * when they use the bag to unload blocks into the Level
	 * @param orientation The facing of the player at the time of unloading-blocks-from-back. Must be a horizontal direction.
	 * @return The rotation used to untransform positions when deserializing them
	 */
	public static Rotation getUntransformRotation(Direction orientation)
	{
		return getHorizontalDifferenceRotation(BASE_DIRECTION, orientation);
	}
	
	/**
	 * Returns a rotation value needed to rotate from one horizontal direction to another.
	 * // horizontal indices are South, West, North, East (from 0 to 3 respectively)
	 * // degrees of each Rotation enum value is enum index * 90 (clockwise)
	 * @param from Starting direction
	 * @param to Ending direction
	 * @return Rotation needed to rotate from from to to
	 */
	public static Rotation getHorizontalDifferenceRotation(Direction from, Direction to)
	{
		// e.g. if we are going from west to north,
		// difference = 2 - 1 = 1 = Rotation.CLOCKWISE_90
		int indexDifference = to.get2DDataValue() - from.get2DDataValue();
		
		// java is weird about moduloing negative numbers, so make sure we're positive first
		if (indexDifference < 0)
		{
			indexDifference += 4;
		}
		
		return Rotation.values()[indexDifference % 4];
	}

	/**
	 * First, we take the offset of the block pos relative to the origin of the yurt
	 *
	 * Second, we rotate this offset around the origin's y-axis
	 *
	 * The rotation is the difference angle between the player's orientation and
	 * some constant direction
	 *
	 * The difference when we unload the yurt is reversed from the difference when
	 * we load the yurt (b-a instead of a-b)
	 *
	 * The result is that when we unload, the rotation of the yurt relative to the
	 * player is preserved
	 *
	 * some examples:
	 *
	 * player facing south both times -- no rotation player facing west both times
	 * -- first rotate 90 degrees, then -90 degrees -- no rotation
	 * 
	 * player facing south first, then west first -- no rotation -- block offsets
	 * are original positions second -- rotate from SOUTH to WEST = +90 degrees a
	 * block that was originally south of the player will now be west of the player
	 * a block that was originally in front of the player will still be in front of
	 * the player
	 * 
	 * player facing west first, then east first -- rotate from west to south -- -90
	 * degrees second -- rotate from south to east -- -90 degrees all blocks will be
	 * rotated 180 degrees around the player
	 * @param rotation the rotation we use to transform the block
	 * @param pos The position (in absolute Levelspace) we want to transform to a rotated offset to the origin 
	 * @param origin the origin to rotate pos around (usually the position a bag of yurting was used on)
	 * @return pos as a rotated relative offset to the origin pos (pos is translated first and then rotated)
	 */
	public static BlockPos transformBlockPos(Rotation rotation, BlockPos pos, BlockPos origin)
	{
		BlockPos offset = pos.subtract(origin); // the difference between the given pos and the yurt origin

		return offset.rotate(rotation);
	}

	/**
	 * Undoes the transformBlockPos function, converting a (relative) offset to an (absolute) origin to an absolute position in Levelspace
	 * (applying the given rotation before transforming)
	 * @param unrotation The rotation to apply to the offset
	 * @param offset The relative offset to the origin pos
	 * @param origin The absolute position we're rotating offset around
	 * @return A position in absolute Levelspace, consisting of the offset rotated around the origin by the specified rotation
	 */
	public static BlockPos untransformBlockPos(Rotation unrotation, BlockPos offset, BlockPos origin)
	{
		BlockPos unRotatedOffset = offset.rotate(unrotation);
	
		return origin.offset(unRotatedOffset);
	}
}

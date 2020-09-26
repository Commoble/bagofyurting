package commoble.bagofyurting.util;

import net.minecraft.util.Direction;
import net.minecraft.util.Rotation;

public class RotationUtil
{
	public static final Direction BASE_DIRECTION = Direction.SOUTH;
	
	public static Rotation getTransformRotation(Direction orientation)
	{
		return getHorizontalDifferenceRotation(orientation, BASE_DIRECTION);
	}
	
	public static Rotation getUntransformRotation(Direction orientation)
	{
		return getHorizontalDifferenceRotation(BASE_DIRECTION, orientation);
	}
	
	// horizontal indices are South, West, North, East (from 0 to 3 respectively)
	// degrees of each Rotation enum value is enum index * 90 (clockwise)
	public static Rotation getHorizontalDifferenceRotation(Direction from, Direction to)
	{
		// e.g. if we are going from west to north,
		// difference = 2 - 1 = 1 = Rotation.CLOCKWISE_90
		int indexDifference = to.getHorizontalIndex() - from.getHorizontalIndex();
		
		// java is weird about moduloing negative numbers, so make sure we're positive first
		if (indexDifference < 0)
		{
			indexDifference += 4;
		}
		
		return Rotation.values()[indexDifference % 4];
	}
}

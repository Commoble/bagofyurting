package com.github.commoble.bagofyurting.util;

import java.util.function.Function;

import net.minecraft.util.Direction;
import net.minecraft.util.Rotation;

public enum BagOperation
{
	// south is arbitrarily chosen as the base direction for having horizontal index 0
	LOAD(orientation -> RotationUtil.getHorizontalDifferenceRotation(orientation, Direction.SOUTH)),
	UNLOAD(orientation -> RotationUtil.getHorizontalDifferenceRotation(Direction.SOUTH, orientation));
	
	private final Function<Direction, Rotation> rotator;
	
	private BagOperation(Function<Direction, Rotation> rotator)
	{
		this.rotator = rotator;
	}
	
	public Rotation getDataRotation(Direction playerOrientation)
	{
		return this.rotator.apply(playerOrientation);
	}
}

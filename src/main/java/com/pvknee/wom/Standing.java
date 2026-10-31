package com.pvknee.wom;

import lombok.Value;

/**
 * One player's progress in a Wise Old Man competition.
 */
@Value
public class Standing
{
	String name;
	long gained;
}

package com.pvknee.wom;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * One player's progress in a Wise Old Man competition.
 */
@Value
public class Standing
{
	String name;
	long gained;

	/**
	 * The standings that count as on the board: everyone who has gained, in order.
	 */
	public static List<Standing> gainers(List<Standing> standings)
	{
		return standings.stream()
			.filter(s -> s.getGained() > 0)
			.collect(Collectors.toList());
	}
}

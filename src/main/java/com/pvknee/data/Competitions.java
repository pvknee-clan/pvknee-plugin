package com.pvknee.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Competitions
{
	private List<Competition> current = new ArrayList<>();
	private List<Competition> upcoming = new ArrayList<>();

	/**
	 * The current competition of this type, e.g. skill or boss; null if there is none.
	 */
	public Competition current(String type)
	{
		if (type == null || current == null)
		{
			return null;
		}
		return current.stream()
			.filter(c -> type.equalsIgnoreCase(c.getType()))
			.findFirst()
			.orElse(null);
	}
}

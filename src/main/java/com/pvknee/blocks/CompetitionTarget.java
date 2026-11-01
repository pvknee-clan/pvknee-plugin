package com.pvknee.blocks;

import com.pvknee.data.Competition;
import lombok.Value;

/**
 * The WOM competition a block's "competition" setting names, with its type and name when it is one of the current
 * competitions.
 */
@Value
class CompetitionTarget
{
	long womId;
	String type;
	String name;

	/**
	 * current_skill and current_boss pick from the current competitions; anything else is a WOM id. Null if there is
	 * no such competition.
	 */
	static CompetitionTarget from(String setting, BlockContext context)
	{
		Competition current = context.currentCompetition(setting);
		if (current != null)
		{
			return current.getWomId() > 0 ? new CompetitionTarget(current.getWomId(), current.getType().toLowerCase(), current.getName()) : null;
		}

		try
		{
			return new CompetitionTarget(Long.parseLong(setting), null, null);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	boolean isSkill()
	{
		return "skill".equals(type);
	}

	boolean isBoss()
	{
		return "boss".equals(type);
	}
}

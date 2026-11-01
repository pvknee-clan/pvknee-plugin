package com.pvknee.chat;

import com.pvknee.blocks.Times;
import com.pvknee.data.ClanData;
import com.pvknee.data.Competition;
import com.pvknee.data.Competitions;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Fills the sheet's chat message templates, e.g. "Skill of the Week is {skill}".
 */
final class Messages
{
	private Messages()
	{
	}

	/**
	 * The placeholders every message can use: {skill}, {boss} and {timeLeft}, for the current challenges. Missing
	 * ones are left out, so they stay as typed.
	 */
	static Map<String, String> challengeValues(ClanData data)
	{
		Map<String, String> values = new HashMap<>();
		Competitions competitions = data.getCompetitions();
		Competition skill = competitions == null ? null : competitions.current("skill");
		Competition boss = competitions == null ? null : competitions.current("boss");
		if (skill != null && skill.getName() != null)
		{
			values.put("skill", skill.getName());
		}
		if (boss != null && boss.getName() != null)
		{
			values.put("boss", boss.getName());
		}

		Competition timed = skill != null ? skill : boss;
		Instant end = timed == null ? null : Times.parse(timed.getEndsAt());
		if (end != null)
		{
			values.put("timeLeft", Times.duration(Duration.between(Instant.now(), end)));
		}
		return values;
	}

	/**
	 * Replaces each {name} that has a value; any other {...} stays as typed.
	 */
	static String fill(String template, Map<String, String> values)
	{
		String message = template;
		for (Map.Entry<String, String> value : values.entrySet())
		{
			message = message.replace("{" + value.getKey() + "}", value.getValue());
		}
		return message;
	}
}

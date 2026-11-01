package com.pvknee.chat;

import com.pvknee.blocks.Times;
import com.pvknee.data.ClanData;
import com.pvknee.data.Competition;
import com.pvknee.data.Competitions;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;

/**
 * Builds chat messages from the sheet's templates, e.g. "{skill} | {timeLeft} left": the wording in RuneLite's normal
 * chat colour and each placeholder's value highlighted, so RuneLite's chat colour settings decide the colours. A
 * placeholder can hold several values, e.g. {challenges}, each highlighted and joined with ", ". A placeholder
 * without a value, or one the plugin doesn't know, stays as typed.
 */
final class Messages
{
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");
	private static final String SEPARATOR = ", ";
	/** The wording used when the sheet has none for a key. */
	private static final Map<String, String> DEFAULTS = Map.of(
		"login", "PvKnee | Skill: {skill} | Boss: {boss} | {timeLeft} left",
		"cmd_sotw", "{skill} | {timeLeft} left | You: {you}",
		"cmd_botw", "{boss} | {timeLeft} left | You: {you}",
		"alert_start", "PvKnee | Started: {challenges}",
		"alert_hour", "PvKnee | 1 hour left: {challenges}",
		"alert_end", "PvKnee | Ended: {challenges}");

	private Messages()
	{
	}

	/**
	 * The sheet's wording for a message, or the built-in default when it is missing or blank.
	 */
	static String template(ClanData data, String key)
	{
		String wording = data.getMessages() == null ? null : data.getMessages().get(key);
		return wording == null || wording.trim().isEmpty() ? DEFAULTS.get(key) : wording.trim();
	}

	/**
	 * The placeholders every message can use: {skill}, {boss} and {timeLeft}, for the current challenges. Missing
	 * ones are left out, so they stay as typed.
	 */
	static Map<String, List<String>> challengeValues(ClanData data)
	{
		Map<String, List<String>> values = new HashMap<>();
		Competitions competitions = data.getCompetitions();
		Competition skill = competitions == null ? null : competitions.current("skill");
		Competition boss = competitions == null ? null : competitions.current("boss");
		if (skill != null && skill.getName() != null)
		{
			values.put("skill", one(skill.getName()));
		}
		if (boss != null && boss.getName() != null)
		{
			values.put("boss", one(boss.getName()));
		}

		Competition timed = skill != null ? skill : boss;
		Instant end = timed == null ? null : Times.parse(timed.getEndsAt());
		if (end != null)
		{
			values.put("timeLeft", one(Times.duration(Duration.between(Instant.now(), end))));
		}
		return values;
	}

	static List<String> one(String value)
	{
		return Collections.singletonList(value);
	}

	/**
	 * The filled template as plain text, e.g. for the dev log.
	 */
	static String plain(String template, Map<String, List<String>> values)
	{
		StringBuilder text = new StringBuilder();
		Matcher matcher = PLACEHOLDER.matcher(template);
		int at = 0;
		while (matcher.find())
		{
			List<String> value = values.get(matcher.group(1));
			text.append(template, at, matcher.start()).append(value == null ? matcher.group() : String.join(SEPARATOR, value));
			at = matcher.end();
		}
		return text.append(template.substring(at)).toString();
	}

	/**
	 * A game message only the member sees: the wording in the normal chat colour, the values highlighted. All text is
	 * escaped, so the sheet cannot add chat formatting.
	 */
	static QueuedMessage message(String template, Map<String, List<String>> values)
	{
		ChatMessageBuilder builder = new ChatMessageBuilder().append(ChatColorType.NORMAL);
		Matcher matcher = PLACEHOLDER.matcher(template);
		int at = 0;
		while (matcher.find())
		{
			builder.append(template.substring(at, matcher.start()));
			List<String> value = values.get(matcher.group(1));
			if (value == null)
			{
				builder.append(matcher.group());
			}
			else
			{
				for (int i = 0; i < value.size(); i++)
				{
					builder.append(i == 0 ? "" : SEPARATOR)
						.append(ChatColorType.HIGHLIGHT)
						.append(value.get(i))
						.append(ChatColorType.NORMAL);
				}
			}
			at = matcher.end();
		}
		builder.append(template.substring(at));

		return QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(builder.build())
			.build();
	}
}

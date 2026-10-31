package com.pvknee.blocks;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Parses endpoint times and formats them for display, always in UTC.
 */
public final class Times
{
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm 'UTC'", Locale.ENGLISH)
		.withZone(ZoneOffset.UTC);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
		.withZone(ZoneOffset.UTC);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm 'UTC'", Locale.ENGLISH)
		.withZone(ZoneOffset.UTC);

	private Times()
	{
	}

	/**
	 * An ISO-8601 time, or null if it is missing or invalid.
	 */
	public static Instant parse(String iso)
	{
		if (iso == null || iso.isEmpty())
		{
			return null;
		}

		try
		{
			return Instant.parse(iso);
		}
		catch (DateTimeParseException e)
		{
			return null;
		}
	}

	/**
	 * e.g. "Fri 6 Nov, 17:00 UTC", or "Fri 6 Nov" when the time of day is not known.
	 */
	public static String date(Instant instant, boolean withTime)
	{
		return (withTime ? DATE_TIME : DATE).format(instant);
	}

	/**
	 * e.g. "17:05 UTC".
	 */
	public static String time(Instant instant)
	{
		return TIME.format(instant);
	}

	/**
	 * e.g. "6d 4h", "5h 12m" or "12m".
	 */
	public static String duration(Duration duration)
	{
		long minutes = Math.max(0, duration.toMinutes());
		long days = minutes / (24 * 60);
		long hours = minutes / 60 % 24;

		if (days > 0)
		{
			return days + "d " + hours + "h";
		}
		if (hours > 0)
		{
			return hours + "h " + minutes % 60 + "m";
		}
		return minutes > 0 ? minutes + "m" : "<1m";
	}
}

package com.pvknee.blocks;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Parses endpoint times and formats them for display, in the member's local time zone or in UTC.
 */
public final class Times
{
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("EEE d MMM uuuu, HH:mm 'UTC'", Locale.ENGLISH)
		.withZone(ZoneOffset.UTC);
	/** An item is ending soon in its final 24 hours. */
	private static final Duration ENDING_SOON = Duration.ofHours(24);
	/** Spans longer than this get one bar segment per week instead of per day. */
	private static final Duration DAILY_SEGMENTS = Duration.ofDays(14);
	/** Longer spans get one plain bar: day or week segments would be too thin to read. */
	private static final Duration MAX_SEGMENTED = Duration.ofDays(60);

	private static volatile boolean utc;

	private Times()
	{
	}

	/**
	 * Shows times in UTC, with a "UTC" suffix, instead of the local time zone.
	 */
	public static void setUtc(boolean utc)
	{
		Times.utc = utc;
	}

	/**
	 * The date and time to the minute, always in UTC whatever the time setting, e.g. "Tue 3 Nov 2026, 21:35 UTC".
	 */
	public static String utcStamp(Instant instant)
	{
		return STAMP.format(instant);
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
	 * e.g. "Today, 17:00", "Tomorrow, 17:00" or "Fri 6 Nov, 17:00", or just the day when the time of day is not
	 * known.
	 */
	public static String date(Instant instant, boolean withTime)
	{
		String dayText = day(instant.atZone(zone()).toLocalDate());
		return withTime ? dayText + ", " + time(instant) : dayText;
	}

	/**
	 * A calendar day relative to today in the member's time zone: "Today", "Tomorrow" or e.g. "Sat 7 Nov".
	 */
	public static String day(LocalDate day)
	{
		LocalDate today = LocalDate.now(zone());
		return day.equals(today) ? "Today"
			: day.equals(today.plusDays(1)) ? "Tomorrow"
			: DAY.format(day);
	}

	/**
	 * The calendar day a date-only sheet time stands for. The sheet sends such days as midnight UTC, so the day is
	 * read in UTC whatever the member's time zone.
	 */
	public static LocalDate sheetDay(Instant instant)
	{
		return instant.atZone(ZoneOffset.UTC).toLocalDate();
	}

	/**
	 * When a calendar day starts in the member's time zone (or in UTC when times are shown in UTC).
	 */
	public static Instant startOfDay(LocalDate day)
	{
		return day.atStartOfDay(zone()).toInstant();
	}

	/**
	 * The days from start to end, e.g. "Fri 30 Oct – Fri 6 Nov", always as dates rather than "Today" or "Tomorrow".
	 */
	public static String range(Instant start, Instant end)
	{
		ZoneId zone = zone();
		return DAY.format(start.atZone(zone)) + " – " + DAY.format(end.atZone(zone));
	}

	/**
	 * e.g. "17:05", or "17:05 UTC" when times are shown in UTC.
	 */
	public static String time(Instant instant)
	{
		return TIME.format(instant.atZone(zone())) + (utc ? " UTC" : "");
	}

	/**
	 * How far now is through the span from start to end, between 0 and 1.
	 */
	public static double progress(Instant start, Instant end)
	{
		long total = Duration.between(start, end).toMillis();
		return total <= 0 ? 1 : (double) Duration.between(start, Instant.now()).toMillis() / total;
	}

	/**
	 * The number of bar segments for a span: one per day, one per week when it is over 14 days, and a single plain bar
	 * when it is over 60 days.
	 */
	public static int segments(Instant start, Instant end)
	{
		Duration span = Duration.between(start, end);
		if (span.compareTo(MAX_SEGMENTED) > 0)
		{
			return 1;
		}
		Duration unit = span.compareTo(DAILY_SEGMENTS) > 0 ? Duration.ofDays(7) : Duration.ofDays(1);
		return (int) Math.max(1, (span.toMillis() + unit.toMillis() - 1) / unit.toMillis());
	}

	/**
	 * Whether an item ending at this time is in its final 24 hours.
	 */
	public static boolean endingSoon(Instant end)
	{
		Instant now = Instant.now();
		return now.isBefore(end) && Duration.between(now, end).compareTo(ENDING_SOON) < 0;
	}

	/**
	 * e.g. "5d 20h left", or "Ends in 5h 12m" in the final 24 hours.
	 */
	public static String left(Instant end)
	{
		String duration = duration(Duration.between(Instant.now(), end));
		return endingSoon(end) ? "Ends in " + duration : duration + " left";
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

	private static ZoneId zone()
	{
		return utc ? ZoneOffset.UTC : ZoneId.systemDefault();
	}
}

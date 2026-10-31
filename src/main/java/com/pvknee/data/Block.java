package com.pvknee.data;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class Block
{
	private String type;
	private Map<String, String> settings = new HashMap<>();

	/**
	 * A trimmed setting, or the default if it is missing or blank.
	 */
	public String setting(String key, String fallback)
	{
		String value = settings == null ? null : settings.get(key);
		return value == null || value.trim().isEmpty() ? fallback : value.trim();
	}

	/**
	 * A whole-number setting, or the default if it is missing or not a number.
	 */
	public int intSetting(String key, int fallback)
	{
		try
		{
			return Integer.parseInt(setting(key, ""));
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}
}

package com.pvknee.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class Block
{
	private String type;
	private Map<String, String> settings = new HashMap<>();
	/** The values of the block's sheet range, for blocks that show one (e.g. table); null for the others. */
	private List<List<String>> rows;

	/**
	 * A trimmed setting, or the default if it is missing or blank.
	 */
	public String setting(String key, String fallback)
	{
		String value = settings == null ? null : settings.get(key);
		return value == null || value.trim().isEmpty() ? fallback : value.trim();
	}

	/**
	 * A true or false setting, or the default if it is missing or neither.
	 */
	public boolean booleanSetting(String key, boolean fallback)
	{
		String value = setting(key, "");
		return "true".equalsIgnoreCase(value) || (!"false".equalsIgnoreCase(value) && fallback);
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

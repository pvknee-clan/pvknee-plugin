package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.ui.DataTable;
import com.pvknee.ui.Section;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * A sheet range as a table in a card. The endpoint sends the range's values as the block's rows. Settings:
 * <ul>
 * <li>header: TRUE to use the range's first row as the header, or the header cells separated by commas</li>
 * <li>columns: which columns to show, counting from 1, e.g. 1,3 (default all)</li>
 * <li>widths: each shown column's share of the width, e.g. 2,1 (default equal)</li>
 * </ul>
 */
@Slf4j
class TableBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		List<List<String>> rows = block.getRows() == null ? new ArrayList<>() : block.getRows().stream()
			.filter(row -> row != null && row.stream().anyMatch(cell -> cell != null && !cell.trim().isEmpty()))
			.collect(Collectors.toList());

		String headerSetting = block.setting("header", null);
		boolean headerFromRange = "true".equalsIgnoreCase(headerSetting);
		List<String> header = null;
		if (headerFromRange)
		{
			header = rows.isEmpty() ? null : rows.remove(0);
		}
		else if (headerSetting != null && !"false".equalsIgnoreCase(headerSetting))
		{
			header = split(headerSetting);
		}

		if (rows.isEmpty())
		{
			log.debug("No rows for table {}", block.getSettings());
			return null;
		}

		int width = rows.stream().mapToInt(List::size).max().orElse(0);
		int[] columns = columns(block.setting("columns", null), width);
		double[] weights = weights(block.setting("widths", null), columns.length);
		List<List<String>> shown = rows.stream().map(row -> pick(row, columns)).collect(Collectors.toList());
		// A header from the range follows the column choice; one typed in the setting already names the shown columns.
		List<String> shownHeader = headerFromRange && header != null ? pick(header, columns) : header;

		return Section.titled(block.setting("title", null), new DataTable(shownHeader, shown, weights, context.getColours()), context.getColours());
	}

	/**
	 * The 0-based columns to show: the setting's 1-based numbers within the range, or every column.
	 */
	private static int[] columns(String setting, int width)
	{
		int[] picked = setting == null ? new int[0] : split(setting).stream()
			.mapToInt(TableBlock::number)
			.filter(n -> n >= 1 && n <= width)
			.map(n -> n - 1)
			.toArray();
		if (picked.length > 0)
		{
			return picked;
		}

		int[] all = new int[width];
		Arrays.setAll(all, i -> i);
		return all;
	}

	/**
	 * Each column's share of the width from the setting; missing or invalid ones get an equal share.
	 */
	private static double[] weights(String setting, int count)
	{
		List<String> parts = setting == null ? new ArrayList<>() : split(setting);
		double[] weights = new double[count];
		for (int i = 0; i < count; i++)
		{
			int weight = i < parts.size() ? number(parts.get(i)) : 0;
			weights[i] = weight > 0 ? weight : 1;
		}
		return weights;
	}

	private static List<String> pick(List<String> row, int[] columns)
	{
		List<String> cells = new ArrayList<>();
		for (int column : columns)
		{
			cells.add(column < row.size() ? row.get(column) : "");
		}
		return cells;
	}

	private static List<String> split(String setting)
	{
		return Arrays.stream(setting.split(","))
			.map(String::trim)
			.collect(Collectors.toList());
	}

	private static int number(String text)
	{
		try
		{
			return Integer.parseInt(text.trim());
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}
}

package com.pvknee.blocks;

import com.pvknee.data.Bingo;
import com.pvknee.ui.Card;
import com.pvknee.ui.Centred;
import com.pvknee.ui.CheckMark;
import com.pvknee.ui.Divider;
import com.pvknee.ui.Fonts;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Pill;
import com.pvknee.ui.ProgressBar;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import java.awt.Color;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * The card under the bingo board for the selected tile: what it needs and the viewed team's progress on it. A dashed
 * hint when no tile is selected.
 */
final class BingoTileDetail
{
	private static final int INNER_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;
	private static final int ICON_SIZE = 26;
	private static final int PART_ICON_SIZE = 16;
	private static final int MARK_SIZE = 14;
	private static final int ROW_GAP = 8;
	private static final int HEAD_GAP = 10;

	private BingoTileDetail()
	{
	}

	static Card of(Bingo bingo, Bingo.Team team, Bingo.Tile tile, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		if (tile == null)
		{
			Card hint = new Card(new BorderLayout());
			hint.setDashed(true);
			hint.add(small("Select a tile to view it", colours.getMuted(), true));
			return hint;
		}

		Bingo.Progress progress = team.getProgress().stream()
			.filter(p -> p.getNum() == tile.getNum())
			.findFirst()
			.orElse(null);

		Card card = new Card(new DynamicGridLayout(0, 1, 0, ROW_GAP));
		card.add(head(tile, progress != null && progress.getStatus() == Bingo.Status.DONE, context));
		if (tile.getDescription() != null && !tile.getDescription().trim().isEmpty())
		{
			card.add(Labels.label(tile.getDescription(), ThemeColours.TEXT, Fonts.body(), INNER_WIDTH, false));
		}
		card.add(new Divider(colours.getMuted()));

		if (progress == null)
		{
			notStarted(card, bingo, colours);
		}
		else if (progress.getStatus() == Bingo.Status.DONE)
		{
			done(card, tile, progress, colours);
		}
		else
		{
			inProgress(card, tile, progress, context);
		}
		return card;
	}

	/**
	 * The icon, name and tile number, and the points pill; the name and points are green once the tile is done.
	 */
	private static JComponent head(Bingo.Tile tile, boolean done, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		Color highlight = done ? ThemeColours.DONE : colours.getValue();
		Pill pill = new Pill("+" + Labels.decimal(tile.getPoints()), highlight, null).bold();
		int textWidth = INNER_WIDTH - ICON_SIZE - pill.getPreferredSize().width - 2 * HEAD_GAP;

		JPanel text = new JPanel(new BorderLayout(0, 1));
		text.setOpaque(false);
		text.add(Labels.label(Objects.toString(tile.getName(), ""), highlight, Fonts.boldName(), textWidth, false), BorderLayout.CENTER);
		text.add(small("Tile " + tile.getNum(), colours.getMuted(), false), BorderLayout.SOUTH);

		JPanel head = new JPanel(new BorderLayout(HEAD_GAP, 0));
		head.setOpaque(false);
		head.add(context.getIcons().item(tile.getItemId(), ICON_SIZE), BorderLayout.WEST);
		head.add(text, BorderLayout.CENTER);
		head.add(new Centred(pill), BorderLayout.EAST);
		return head;
	}

	private static void notStarted(Card card, Bingo bingo, ThemeColours colours)
	{
		String submit = bingo.getSubmit() == null || bingo.getSubmit().trim().isEmpty() ? "" : " in " + bingo.getSubmit().trim();
		card.add(Labels.plain("Not started", colours.getMuted(), FontManager.getRunescapeBoldFont()));
		card.add(small("Got it? Post a screenshot" + submit + " on Discord.", colours.getMuted(), false));
	}

	/**
	 * "Completed" with the points, then when and by whom, e.g. "Sun 1 Nov, 19:12 · by Iron Aeria".
	 */
	private static void done(Card card, Bingo.Tile tile, Bingo.Progress progress, ThemeColours colours)
	{
		card.add(statusLine("Completed", ThemeColours.DONE, "+" + Labels.decimal(tile.getPoints()) + " pts", ThemeColours.DONE));

		Instant at = Times.parse(progress.getCompletedAt());
		String when = at == null ? null : Times.date(at, true);
		String by = progress.getCompletedBy();
		String line = when != null && by != null ? when + " · by " + by
			: when != null ? when
			: by != null ? "By " + by
			: null;
		if (line != null)
		{
			card.add(small(line, colours.getMuted(), false));
		}
	}

	/**
	 * "In progress" with how much is in and a bar, then who got what, or for a tile with parts, a checklist of parts.
	 */
	private static void inProgress(Card card, Bingo.Tile tile, Bingo.Progress progress, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		long needed = Math.max(1, tile.getNeeded());
		long amount = Math.min(progress.getAmount(), needed);
		card.add(statusLine("In progress", ThemeColours.IN_PROGRESS, Labels.fraction(amount, needed, tile.getUnit()), colours.getValue()));
		card.add(new ProgressBar((double) amount / needed, ThemeColours.IN_PROGRESS));

		if (tile.getParts().isEmpty())
		{
			for (Bingo.Contributor contributor : progress.getContributors())
			{
				String note = contributor.getNote();
				String what = note != null && !note.trim().isEmpty() ? note.trim() : Labels.amount(contributor.getAmount(), tile.getUnit());
				card.add(row(null, contributor.getName(), what, colours));
			}
			return;
		}

		Map<String, Bingo.PartProgress> parts = progress.getParts().stream()
			.filter(part -> part.getName() != null)
			.collect(Collectors.toMap(Bingo.PartProgress::getName, Function.identity(), (a, b) -> a));
		for (Bingo.Part part : tile.getParts())
		{
			card.add(partRow(part, parts.get(part.getName()), context));
		}
	}

	/**
	 * A part's icon, a tick when it is in (or how much, e.g. "1 / 2", when it needs more than one), its name and who
	 * got it.
	 */
	private static JComponent partRow(Bingo.Part part, Bingo.PartProgress progress, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		boolean done = progress != null && progress.isDone();
		long needed = Math.max(1, part.getNeeded());

		JPanel lead = new JPanel(new BorderLayout(Spacing.ITEM_GAP, 0));
		lead.setOpaque(false);
		if (part.getItemId() > 0)
		{
			lead.add(context.getIcons().item(part.getItemId(), PART_ICON_SIZE), BorderLayout.WEST);
		}
		lead.add(done || needed == 1 ? new JLabel(new CheckMark(done, MARK_SIZE))
			: Labels.plain((progress == null ? 0 : progress.getAmount()) + " / " + needed, colours.getMuted(), FontManager.getRunescapeSmallFont()),
			BorderLayout.CENTER);

		String who = progress == null ? "" : progress.getContributors().stream()
			.map(Bingo.Contributor::getName)
			.filter(Objects::nonNull)
			.distinct()
			.collect(Collectors.joining(", "));
		return row(lead, part.getName(), who, colours);
	}

	/**
	 * A name on the left, after an optional lead, with a muted note on the right.
	 */
	private static JComponent row(JComponent lead, String name, String note, ThemeColours colours)
	{
		JPanel row = new JPanel(new BorderLayout(ROW_GAP, 0));
		row.setOpaque(false);
		if (lead != null)
		{
			row.add(lead, BorderLayout.WEST);
		}
		row.add(Labels.plain(Objects.toString(name, ""), ThemeColours.TEXT, Fonts.name()), BorderLayout.CENTER);
		row.add(Labels.plain(note, colours.getMuted(), FontManager.getRunescapeSmallFont()), BorderLayout.EAST);
		return row;
	}

	/**
	 * The status in its colour on the left, with a value on the right.
	 */
	private static JComponent statusLine(String status, Color colour, String value, Color valueColour)
	{
		JPanel row = new JPanel(new BorderLayout(ROW_GAP, 0));
		row.setOpaque(false);
		row.add(Labels.plain(status, colour, FontManager.getRunescapeBoldFont()), BorderLayout.WEST);
		row.add(Labels.plain(value, valueColour, FontManager.getRunescapeSmallFont()), BorderLayout.EAST);
		return row;
	}

	private static JLabel small(String text, Color colour, boolean centred)
	{
		return Labels.label(text, colour, FontManager.getRunescapeSmallFont(), INNER_WIDTH, centred);
	}
}

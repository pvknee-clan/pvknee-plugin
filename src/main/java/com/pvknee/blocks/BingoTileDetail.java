package com.pvknee.blocks;

import com.pvknee.data.Bingo;
import com.pvknee.ui.Card;
import com.pvknee.ui.Centred;
import com.pvknee.ui.CheckMark;
import com.pvknee.ui.Divider;
import com.pvknee.ui.Fonts;
import com.pvknee.ui.IconCells;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Pill;
import com.pvknee.ui.ProgressBar;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * The card under the bingo board for the selected tile: its status, then what it needs, shown before anything is
 * logged: a checklist of parts (one route, or several with "or" between), the items that count, or for a plain
 * count, who got what. Rules in brackets in the description are in red. Progress is the viewed team's (none before
 * teams are set). A dashed hint when no tile is selected.
 */
final class BingoTileDetail
{
	private static final int INNER_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;
	private static final int ICON_SIZE = 26;
	private static final int PART_ICON_SIZE = 16;
	private static final int MARK_SIZE = 14;
	private static final int ROW_GAP = 8;
	private static final int HEAD_GAP = 10;
	private static final int ITEM_COLUMNS = 6;
	/** A rule in a description: text in round brackets, brackets included. A bracket without its pair is not one. */
	private static final Pattern RULE = Pattern.compile("\\([^()]*\\)");

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

		Bingo.Progress progress = team == null ? null : team.getProgress().stream()
			.filter(p -> p.getNum() == tile.getNum())
			.findFirst()
			.orElse(null);
		boolean done = progress != null && progress.getStatus() == Bingo.Status.DONE;
		Bingo.Mode mode = tile.mode();
		List<List<Bingo.Part>> routes = tile.eachRoutes();
		if (mode == Bingo.Mode.EACH && routes.isEmpty())
		{
			mode = Bingo.Mode.COUNT;
		}

		Card card = new Card(new DynamicGridLayout(0, 1, 0, ROW_GAP));
		card.add(head(tile, done, context));
		if (tile.getDescription() != null && !tile.getDescription().trim().isEmpty())
		{
			card.add(Labels.highlighted(tile.getDescription(), RULE, ThemeColours.RULE, ThemeColours.TEXT, Fonts.body(), INNER_WIDTH, false));
		}
		card.add(new Divider(colours.getMuted()));

		if (progress == null)
		{
			notStarted(card, tile, mode, routes, colours);
		}
		else if (done)
		{
			done(card, tile, progress, colours);
		}
		else
		{
			inProgress(card, tile, mode, progress, colours);
		}

		switch (mode)
		{
			case EACH:
				routes(card, routes, progress, context);
				break;
			case ANY:
				items(card, tile, progress, context);
				break;
			default:
				if (progress != null && !done)
				{
					log(card, tile, progress, colours);
				}
		}

		if (progress == null)
		{
			String submit = bingo.getSubmit() == null || bingo.getSubmit().trim().isEmpty() ? "" : " in " + bingo.getSubmit().trim();
			card.add(small("Got one? Post a screenshot" + submit + " on Discord.", colours.getMuted(), false));
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

	/**
	 * What the tile needs, e.g. "Needs one of each", "Needs either" or "Needs any 2", with "0 of 2"; just "Not
	 * started" for a plain count.
	 */
	private static void notStarted(Card card, Bingo.Tile tile, Bingo.Mode mode, List<List<Bingo.Part>> routes, ThemeColours colours)
	{
		long needed = Math.max(1, tile.getNeeded());
		String needs = mode == Bingo.Mode.ANY ? "Needs any " + needed
			: mode == Bingo.Mode.EACH ? routes.size() == 1 ? "Needs one of each" : "Needs either"
			: null;
		if (needs == null)
		{
			card.add(Labels.plain("Not started", colours.getMuted(), FontManager.getRunescapeBoldFont()));
			return;
		}
		card.add(statusLine(needs, colours.getMuted(), "0 of " + needed, colours.getMuted()));
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
	 * "In progress" with how much is in, e.g. "2 of 4" ("1 / 2 pieces" for a plain count), and a bar.
	 */
	private static void inProgress(Card card, Bingo.Tile tile, Bingo.Mode mode, Bingo.Progress progress, ThemeColours colours)
	{
		long needed = Math.max(1, tile.getNeeded());
		long amount = Math.min(progress.getAmount(), needed);
		String count = mode == Bingo.Mode.COUNT ? Labels.fraction(amount, needed, tile.getUnit()) : amount + " of " + needed;
		card.add(statusLine("In progress", ThemeColours.IN_PROGRESS, count, colours.getValue()));
		card.add(new ProgressBar((double) amount / needed, ThemeColours.IN_PROGRESS));
	}

	/**
	 * Who got what, one line each, e.g. "Pgywgy" with "Magic fang" (or the amount when there is no note).
	 */
	private static void log(Card card, Bingo.Tile tile, Bingo.Progress progress, ThemeColours colours)
	{
		for (Bingo.Contributor contributor : progress.getContributors())
		{
			String note = contributor.getNote();
			String what = note != null && !note.trim().isEmpty() ? note.trim() : Labels.amount(contributor.getAmount(), tile.getUnit());
			card.add(row(contributor.getName(), what, colours));
		}
	}

	/**
	 * Each route's checklist, with "or" between routes. Once the tile is done, the routes that did not complete it
	 * are muted.
	 */
	private static void routes(Card card, List<List<Bingo.Part>> routes, Bingo.Progress progress, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		boolean done = progress != null && progress.getStatus() == Bingo.Status.DONE;
		for (int i = 0; i < routes.size(); i++)
		{
			if (i > 0)
			{
				card.add(Divider.withText("or", colours.getMuted()));
			}
			boolean completedIt = progress != null && i < progress.getRoutes().size() && progress.getRoutes().get(i).isDone();
			boolean muted = done && routes.size() > 1 && !completedIt;

			Map<String, Bingo.PartProgress> parts = partProgress(progress, i).stream()
				.filter(part -> part.getName() != null)
				.collect(Collectors.toMap(Bingo.PartProgress::getName, Function.identity(), (a, b) -> a));
			for (Bingo.Part part : routes.get(i))
			{
				slots(card, part, parts.get(part.getName()), muted, context);
			}
		}
	}

	/**
	 * The logged parts of one route: the route's own progress, or for a one-route tile from an older script, the
	 * tile's parts.
	 */
	private static List<Bingo.PartProgress> partProgress(Bingo.Progress progress, int route)
	{
		if (progress == null)
		{
			return Collections.emptyList();
		}
		if (route < progress.getRoutes().size())
		{
			return progress.getRoutes().get(route).getParts();
		}
		return route == 0 ? progress.getParts() : Collections.emptyList();
	}

	/**
	 * A row per unit the part needs, ticked in turn, each with the member who filled it: contributors in log order,
	 * one slot per unit they got.
	 */
	private static void slots(Card card, Bingo.Part part, Bingo.PartProgress progress, boolean muted, BlockContext context)
	{
		long needed = Math.max(1, part.getNeeded());
		long filled = progress == null ? 0 : progress.isDone() ? needed : Math.min(progress.getAmount(), needed);
		List<Bingo.Contributor> fillers = new ArrayList<>();
		if (progress != null)
		{
			for (Bingo.Contributor contributor : progress.getContributors())
			{
				for (long unit = 0; unit < Math.max(1, contributor.getAmount()) && fillers.size() < needed; unit++)
				{
					fillers.add(contributor);
				}
			}
		}

		for (int slot = 0; slot < needed; slot++)
		{
			Bingo.Contributor filler = slot < filled && slot < fillers.size() ? fillers.get(slot) : null;
			card.add(slotRow(part, slot < filled, filler, muted, context));
		}
	}

	/**
	 * A tick, the part's icon and name, the filler's note under the name, and who filled it on the right.
	 */
	private static JComponent slotRow(Bingo.Part part, boolean filled, Bingo.Contributor filler, boolean muted, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		JPanel lead = new JPanel(new BorderLayout(Spacing.ITEM_GAP, 0));
		lead.setOpaque(false);
		lead.add(new JLabel(new CheckMark(filled, MARK_SIZE, muted ? colours.getMuted() : ThemeColours.DONE)), BorderLayout.WEST);
		lead.add(context.getIcons().item(part.getItemId(), PART_ICON_SIZE), BorderLayout.CENTER);

		JPanel name = new JPanel(new BorderLayout(0, 1));
		name.setOpaque(false);
		name.add(Labels.plain(Objects.toString(part.getName(), ""), muted ? colours.getMuted() : ThemeColours.TEXT, Fonts.name()), BorderLayout.CENTER);
		String note = filler == null ? null : filler.getNote();
		if (note != null && !note.trim().isEmpty())
		{
			name.add(Labels.plain(note.trim(), colours.getMuted(), FontManager.getRunescapeSmallFont()), BorderLayout.SOUTH);
		}

		JPanel row = new JPanel(new BorderLayout(ROW_GAP, 0));
		row.setOpaque(false);
		row.add(lead, BorderLayout.WEST);
		row.add(name, BorderLayout.CENTER);
		row.add(Labels.plain(filler == null ? "" : filler.getName(), colours.getMuted(), FontManager.getRunescapeSmallFont()), BorderLayout.EAST);
		return row;
	}

	/**
	 * The items that count, as icons named on hover; the ones logged are outlined, orange while the tile is in
	 * progress and green once done. Then who got what.
	 */
	private static void items(Card card, Bingo.Tile tile, Bingo.Progress progress, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		Map<String, Long> logged = new HashMap<>();
		if (progress != null)
		{
			for (Bingo.ItemProgress item : progress.getItems())
			{
				if (item.getName() != null)
				{
					logged.merge(key(item.getName()), item.getAmount(), Long::sum);
				}
			}
		}
		Color outline = progress != null && progress.getStatus() == Bingo.Status.DONE ? ThemeColours.DONE : ThemeColours.IN_PROGRESS;

		if (!tile.getItems().isEmpty())
		{
			IconCells cells = new IconCells(ITEM_COLUMNS, INNER_WIDTH);
			for (Bingo.Item item : tile.getItems())
			{
				String name = Objects.toString(item.getName(), "");
				boolean got = logged.getOrDefault(key(name), 0L) > 0;
				context.getIcons().loadItem(item.getItemId(), cells.iconSize(), cells.addCell(name, got ? outline : null));
			}
			card.add(cells);
		}

		if (progress != null && !progress.getContributors().isEmpty())
		{
			card.add(new Divider(colours.getMuted()));
			log(card, tile, progress, colours);
		}
	}

	private static String key(String name)
	{
		return name.trim().toLowerCase();
	}

	/**
	 * A name on the left with a muted note on the right.
	 */
	private static JComponent row(String name, String note, ThemeColours colours)
	{
		JPanel row = new JPanel(new BorderLayout(ROW_GAP, 0));
		row.setOpaque(false);
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

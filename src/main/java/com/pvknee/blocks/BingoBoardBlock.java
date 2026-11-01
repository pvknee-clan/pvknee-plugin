package com.pvknee.blocks;

import com.pvknee.data.Bingo;
import com.pvknee.data.Block;
import com.pvknee.ui.Card;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import com.pvknee.ui.TileGrid;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;

/**
 * The viewed team's bingo board: a tile per place, coloured by the team's progress, with a legend under it, and the
 * selected tile's details under that. Clicking a tile selects it for every bingo block on the page; clicking it
 * again clears the selection.
 */
class BingoBoardBlock implements BlockRenderer
{
	private static final int INNER_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;
	private static final int LEGEND_GAP = 8;
	private static final int CARD_GAP = 6;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		BingoView view = context.getBingoView();
		Bingo bingo = view.getBingo();
		if (bingo == null || view.getTeam() == null)
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		if (bingo.getTiles().isEmpty())
		{
			return new Section(new SectionHeader(block.setting("title", "Board"), colours), placeholder(bingo, context));
		}
		SectionHeader header = new SectionHeader(block.setting("title", "Board"), colours)
			.withAside(() -> "Select a tile", colours);
		JPanel cards = new JPanel(new BorderLayout(0, CARD_GAP));
		cards.setOpaque(false);
		cards.add(board(view, bingo, context), BorderLayout.NORTH);
		cards.add(context.followBingo(() -> BingoTileDetail.of(bingo, view.getTeam(), view.getTile(), context)), BorderLayout.CENTER);
		return new Section(header, cards);
	}

	/**
	 * Two rows of empty places, until the endpoint sends the tiles at the start.
	 */
	private static Card placeholder(Bingo bingo, BlockContext context)
	{
		int size = Math.max(1, bingo.getBoardSize());
		TileGrid grid = new TileGrid(size, Collections.nCopies(2 * size, null), -1, context.getColours().getAccent(), INNER_WIDTH, index ->
		{
		});
		Card card = new Card(new BorderLayout(0, LEGEND_GAP));
		card.add(grid, BorderLayout.CENTER);
		card.add(Labels.label("The board is revealed when the bingo starts.", context.getColours().getMuted(),
			FontManager.getRunescapeSmallFont(), INNER_WIDTH, true), BorderLayout.SOUTH);
		return card;
	}

	/**
	 * The grid and legend. Built once and changed in place when the team or tile changes, so the board doesn't
	 * flicker.
	 */
	private static Card board(BingoView view, Bingo bingo, BlockContext context)
	{
		int size = Math.max(1, bingo.getBoardSize());
		List<Bingo.Tile> places = new ArrayList<>();
		for (int num = 1; num <= size * size; num++)
		{
			places.add(bingo.tile(num));
		}

		TileGrid grid = new TileGrid(size, gridTiles(view, places), selectedIndex(view, places), context.getColours().getAccent(), INNER_WIDTH,
			index -> view.selectTile(places.get(index) == view.getTile() ? null : places.get(index)));
		view.onChange(() -> grid.setTiles(gridTiles(view, places), selectedIndex(view, places)));
		int iconSize = TileGrid.iconSize(INNER_WIDTH, size);
		for (int i = 0; i < places.size(); i++)
		{
			Bingo.Tile tile = places.get(i);
			int index = i;
			if (tile != null)
			{
				context.getIcons().loadItem(tile.getItemId(), iconSize, icon -> grid.setIcon(index, icon.getImage()));
			}
		}

		Card card = new Card(new BorderLayout(0, LEGEND_GAP));
		card.add(grid, BorderLayout.CENTER);
		card.add(TileGrid.legend(context.getColours().getMuted()), BorderLayout.SOUTH);
		return card;
	}

	/**
	 * Each place's look from the viewed team's progress; null for a place without a tile.
	 */
	private static List<TileGrid.Tile> gridTiles(BingoView view, List<Bingo.Tile> places)
	{
		Map<Integer, Bingo.Progress> progress = view.getTeam().getProgress().stream()
			.collect(Collectors.toMap(Bingo.Progress::getNum, Function.identity(), (a, b) -> a));
		List<TileGrid.Tile> tiles = new ArrayList<>();
		for (Bingo.Tile tile : places)
		{
			tiles.add(tile == null ? null : gridTile(tile, progress.get(tile.getNum())));
		}
		return tiles;
	}

	private static int selectedIndex(BingoView view, List<Bingo.Tile> places)
	{
		Bingo.Tile selected = view.getTile();
		return selected == null ? -1 : places.indexOf(selected);
	}

	/**
	 * A tile's look from the team's progress on it, with its name and, when it needs more than one, how much is in
	 * as the hover text, e.g. "Two Bandos pieces · 1 / 2 pieces".
	 */
	private static TileGrid.Tile gridTile(Bingo.Tile tile, Bingo.Progress progress)
	{
		long needed = Math.max(1, tile.getNeeded());
		long amount = progress == null ? 0 : Math.min(progress.getAmount(), needed);
		TileGrid.Status status = progress == null ? TileGrid.Status.NOT_STARTED
			: progress.getStatus() == Bingo.Status.DONE ? TileGrid.Status.DONE
			: TileGrid.Status.PROGRESS;

		String name = tile.getName() == null ? "Tile " + tile.getNum() : tile.getName();
		String tooltip = needed > 1 ? name + " · " + Labels.fraction(amount, needed, tile.getUnit()) : name;
		return new TileGrid.Tile(status, (double) amount / needed, tooltip);
	}
}

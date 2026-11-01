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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JComponent;

/**
 * The viewed team's bingo board: a tile per place, coloured by the team's progress, with a legend under it. Clicking
 * a tile selects it for every bingo block on the page; clicking it again clears the selection.
 */
class BingoBoardBlock implements BlockRenderer
{
	private static final int INNER_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;
	private static final int LEGEND_GAP = 8;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		BingoView view = context.getBingoView();
		Bingo bingo = view.getBingo();
		if (bingo == null || view.getTeam() == null || bingo.getTiles().isEmpty())
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		SectionHeader header = new SectionHeader(block.setting("title", "Board"), colours)
			.withAside(() -> "Select a tile", colours);
		return new Section(header, context.followBingo(() -> card(view, bingo, context)));
	}

	private static Card card(BingoView view, Bingo bingo, BlockContext context)
	{
		int size = Math.max(1, bingo.getBoardSize());
		Map<Integer, Bingo.Progress> progress = view.getTeam().getProgress().stream()
			.collect(Collectors.toMap(Bingo.Progress::getNum, Function.identity(), (a, b) -> a));
		Bingo.Tile selected = view.getTile();

		List<Bingo.Tile> places = new ArrayList<>();
		List<TileGrid.Tile> tiles = new ArrayList<>();
		for (int num = 1; num <= size * size; num++)
		{
			Bingo.Tile tile = bingo.tile(num);
			places.add(tile);
			tiles.add(tile == null ? null : gridTile(tile, progress.get(num)));
		}

		TileGrid grid = new TileGrid(size, tiles, selected == null ? -1 : places.indexOf(selected), context.getColours().getAccent(), INNER_WIDTH,
			index -> view.selectTile(places.get(index) == selected ? null : places.get(index)));
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

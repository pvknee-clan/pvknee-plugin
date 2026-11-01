package com.pvknee.blocks;

import com.pvknee.DevLog;
import com.pvknee.data.Bingo;
import com.pvknee.player.PlayerState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What the bingo page is showing: the selected team and tile, shared by every bingo block on the page so they follow
 * the same selection. Kept while the data reloads and the page is built again. Used on the Swing thread.
 */
public class BingoView
{
	private final DevLog devLog;
	private final List<Runnable> listeners = new ArrayList<>();

	private Bingo bingo;
	private Bingo.Team ownTeam;
	/** The team the member switched to; null to follow their own team (or the top team). */
	private String chosenTeam;
	private Integer selectedTile;
	/** The last state logged, so a reload that changes nothing logs nothing. */
	private String logged;

	public BingoView(DevLog devLog)
	{
		this.devLog = devLog;
	}

	/**
	 * Takes in new data or a change in game, keeping the selection while its team and tile still exist.
	 */
	public void update(Bingo bingo, PlayerState player)
	{
		this.bingo = bingo;
		ownTeam = bingo == null ? null : bingo.getTeams().stream()
			.filter(team -> team.getMembers().stream().anyMatch(member -> player.isNamed(member.getName())))
			.findFirst()
			.orElse(null);
		if (bingo == null || bingo.team(chosenTeam) == null)
		{
			chosenTeam = null;
		}
		if (bingo == null || selectedTile != null && bingo.tile(selectedTile) == null)
		{
			selectedTile = null;
		}

		String state = bingo == null ? "none"
			: bingo.getState() + ", own team " + name(ownTeam) + ", viewing " + name(getTeam()) + ", tile " + (selectedTile == null ? "none" : selectedTile);
		if (!state.equals(logged))
		{
			logged = state;
			devLog.log("bingo: {}", state);
		}
	}

	/**
	 * The bingo, or null if there is none.
	 */
	public Bingo getBingo()
	{
		return bingo;
	}

	/**
	 * The member's own team; null when logged out or not on a team.
	 */
	public Bingo.Team getOwnTeam()
	{
		return ownTeam;
	}

	public boolean isOwnTeam(Bingo.Team team)
	{
		return team != null && team == ownTeam;
	}

	/**
	 * The team being viewed: the one the member switched to, else their own, else the top team. Null with no teams.
	 */
	public Bingo.Team getTeam()
	{
		if (bingo == null)
		{
			return null;
		}

		Bingo.Team chosen = bingo.team(chosenTeam);
		if (chosen != null)
		{
			return chosen;
		}
		if (ownTeam != null)
		{
			return ownTeam;
		}
		return bingo.getTeams().isEmpty() ? null : bingo.getTeams().get(0);
	}

	public void selectTeam(Bingo.Team team)
	{
		if (team == null || team == getTeam())
		{
			return;
		}

		chosenTeam = team == ownTeam ? null : team.getName();
		devLog.log("bingo: viewing {}", team.getName());
		changed();
	}

	/**
	 * The selected tile, or null.
	 */
	public Bingo.Tile getTile()
	{
		return bingo == null || selectedTile == null ? null : bingo.tile(selectedTile);
	}

	/**
	 * Selects the tile, or clears the selection with null.
	 */
	public void selectTile(Bingo.Tile tile)
	{
		Integer num = tile == null ? null : tile.getNum();
		if (Objects.equals(num, selectedTile))
		{
			return;
		}

		selectedTile = num;
		devLog.log("bingo: tile {}", num == null ? "cleared" : num + " selected");
		changed();
	}

	/**
	 * Runs after the selected team or tile changes, until the page is built again.
	 */
	public void onChange(Runnable listener)
	{
		listeners.add(listener);
	}

	/**
	 * Forgets the listeners of the page that was shown, before the next is built.
	 */
	public void clearListeners()
	{
		listeners.clear();
	}

	private void changed()
	{
		new ArrayList<>(listeners).forEach(Runnable::run);
	}

	private static String name(Bingo.Team team)
	{
		return team == null ? "none" : team.getName();
	}
}

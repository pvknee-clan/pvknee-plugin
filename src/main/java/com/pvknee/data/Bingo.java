package com.pvknee.data;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Data;

/**
 * The clan bingo, worked out by the Apps Script from the active bingo sheet. Null in {@link ClanData} when there is none.
 */
@Data
public class Bingo
{
	private String name;
	private String startsAt;
	private String endsAt;
	/** Null if the endpoint sends a state this version doesn't know. */
	private State state;
	private int boardSize = 5;
	/** Where to post proof, e.g. "#bingo-submissions". */
	private String submit;
	/** Empty until the start, so the board isn't revealed early. */
	private List<Tile> tiles = new ArrayList<>();
	/** Sorted by points, best first. */
	private List<Team> teams = new ArrayList<>();

	public enum State
	{
		@SerializedName("upcoming") UPCOMING,
		@SerializedName("running") RUNNING,
		/** Ended, but the results aren't final yet. */
		@SerializedName("tallying") TALLYING,
		@SerializedName("final") FINAL
	}

	/**
	 * How a tile is completed.
	 */
	public enum Mode
	{
		/** Every part of one route; a tile with several routes can be done by any of them. */
		@SerializedName("each") EACH,
		/** The listed items all qualify and add up to the tile's needed. */
		@SerializedName("any") ANY,
		/** A plain count, without parts. */
		@SerializedName("count") COUNT
	}

	public enum Status
	{
		@SerializedName("progress") PROGRESS,
		@SerializedName("done") DONE
	}

	/**
	 * The team of this name, or null.
	 */
	public Team team(String teamName)
	{
		return teams.stream()
			.filter(team -> team.getName() != null && team.getName().equals(teamName))
			.findFirst()
			.orElse(null);
	}

	/**
	 * The tile with this number, or null.
	 */
	public Tile tile(int num)
	{
		return tiles.stream()
			.filter(tile -> tile.getNum() == num)
			.findFirst()
			.orElse(null);
	}

	@Data
	public static class Tile
	{
		/** The tile's place on the board, counting from 1, left to right and top to bottom. */
		private int num;
		private String name;
		private String description;
		private int itemId;
		private double points;
		/** For a tile with parts, the number of parts. */
		private long needed = 1;
		/** E.g. "pieces", "kills", "xp"; "parts" for a tile with parts. */
		private String unit;
		/** As one route; the first route of a tile with several. */
		private List<Part> parts = new ArrayList<>();
		/** Null from an older script, or one this version doesn't know; see {@link #mode()}. */
		private Mode mode;
		/** For an each tile: the alternative sets of parts, any one of which completes the tile. */
		private List<List<Part>> routes = new ArrayList<>();
		/** For an any tile: the items that count towards it. */
		private List<Item> items = new ArrayList<>();

		/**
		 * The tile's mode; when the endpoint doesn't send one, each for a tile with parts, else count.
		 */
		public Mode mode()
		{
			if (mode != null)
			{
				return mode;
			}
			return parts.isEmpty() ? Mode.COUNT : Mode.EACH;
		}

		/**
		 * The routes of an each tile, falling back to its parts as one route; empty for any other tile.
		 */
		public List<List<Part>> eachRoutes()
		{
			if (mode() != Mode.EACH)
			{
				return Collections.emptyList();
			}
			if (!routes.isEmpty())
			{
				return routes;
			}
			return parts.isEmpty() ? Collections.emptyList() : Collections.singletonList(parts);
		}
	}

	@Data
	public static class Item
	{
		private String name;
		/** 0 when the item has no icon. */
		private int itemId;
	}

	@Data
	public static class Part
	{
		private String name;
		private long needed = 1;
		/** 0 when the part has no icon of its own. */
		private int itemId;
	}

	@Data
	public static class Team
	{
		private String name;
		/** Hex colour; null for a default one (see {@code TeamColours}). */
		private String colour;
		/** Teams on the same points share a rank. */
		private int rank;
		private double points;
		private int tilesDone;
		private List<Member> members = new ArrayList<>();
		/** Only tiles with at least one log row; any other tile is not started. */
		private List<Progress> progress = new ArrayList<>();
	}

	@Data
	public static class Member
	{
		private String name;
		/** Null if the endpoint stops sending member points. */
		private Double points;
		/** Done tiles they contributed to. */
		private int tiles;
	}

	@Data
	public static class Progress
	{
		private int num;
		private Status status;
		/** Capped at the tile's needed; for a tile with parts, the parts complete. */
		private long amount;
		private String completedAt;
		private String completedBy;
		private List<Contributor> contributors = new ArrayList<>();
		/** In the tile's part order; empty for a tile without parts. */
		private List<PartProgress> parts = new ArrayList<>();
		/** For an each tile, in the tile's route order. */
		private List<RouteProgress> routes = new ArrayList<>();
		/** For an any tile, in the tile's item order: how much was logged against each. */
		private List<ItemProgress> items = new ArrayList<>();
		/** For a tile with several routes: the logged amount that matched no part, counted towards needed. */
		private long extra;
	}

	@Data
	public static class RouteProgress
	{
		/** True for the route that completed the tile. */
		private boolean done;
		private List<PartProgress> parts = new ArrayList<>();
	}

	@Data
	public static class ItemProgress
	{
		private String name;
		private long amount;
	}

	@Data
	public static class PartProgress
	{
		private String name;
		private long needed = 1;
		private long amount;
		private boolean done;
		private List<Contributor> contributors = new ArrayList<>();
	}

	@Data
	public static class Contributor
	{
		private String name;
		private long amount;
		/** What they got, e.g. "Bandos tassets"; may be empty. */
		private String note;
	}
}

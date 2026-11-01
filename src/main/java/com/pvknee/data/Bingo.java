package com.pvknee.data;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
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
		private List<Part> parts = new ArrayList<>();
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
		/** Hex colour; null for the theme's value colour. */
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

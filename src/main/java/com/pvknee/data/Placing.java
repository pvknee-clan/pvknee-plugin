package com.pvknee.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * A player on a competition's board, with their place. Equal gains share a place and the next place is skipped, e.g.
 * 1st, 1st, 3rd.
 */
@Value
public class Placing
{
	String name;
	long gained;
	int place;

	/**
	 * Everyone in the competition who has gained, highest first. Empty when there are no standings yet, e.g. just
	 * after a changeover.
	 */
	public static List<Placing> board(Competition competition)
	{
		if (competition == null || competition.getStandings() == null)
		{
			return Collections.emptyList();
		}

		List<Standing> gainers = competition.getStandings().stream()
			.filter(s -> s != null && s.getName() != null && s.getGained() > 0)
			.sorted(Comparator.comparingLong(Standing::getGained).reversed())
			.collect(Collectors.toList());

		List<Placing> board = new ArrayList<>(gainers.size());
		for (int i = 0; i < gainers.size(); i++)
		{
			Standing standing = gainers.get(i);
			boolean tied = i > 0 && standing.getGained() == gainers.get(i - 1).getGained();
			int place = tied ? board.get(i - 1).getPlace() : i + 1;
			board.add(new Placing(standing.getName(), standing.getGained(), place));
		}
		return board;
	}

	/**
	 * The first placing whose name matches, e.g. the member's own; null if there is none.
	 */
	public static Placing find(List<Placing> board, Predicate<String> isName)
	{
		return board.stream()
			.filter(p -> isName.test(p.getName()))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Whether this placing shares its place with another, e.g. two players level in 1st.
	 */
	public boolean isShared(List<Placing> board)
	{
		return board.stream().filter(p -> p.getPlace() == place).count() > 1;
	}
}

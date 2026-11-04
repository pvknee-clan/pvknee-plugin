package com.pvknee.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Competition
{
	private static final String WOM_PAGE = "https://wiseoldman.net/competitions/";

	private String type;
	private String name;
	private String metric;
	private long womId;
	private String startsAt;
	private String endsAt;
	private String colour;
	/** Everyone in a current competition, in Wise Old Man's order; empty for upcoming ones. */
	private List<Standing> standings = new ArrayList<>();
	/** When the sheet last copied the standings from Wise Old Man. */
	private String standingsAt;

	/**
	 * The competition's page on the Wise Old Man website.
	 */
	public String womPage()
	{
		return WOM_PAGE + womId;
	}
}

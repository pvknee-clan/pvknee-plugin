package com.pvknee.data;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class ClanData
{
	private int version;
	private String error;
	private String iconBase;
	private Theme theme;
	private List<Page> pages = new ArrayList<>();
	private Competitions competitions = new Competitions();
	private List<ClanEvent> events = new ArrayList<>();
	private List<Link> links = new ArrayList<>();
	private Map<String, String> messages = new HashMap<>();
	/** Null when there is no bingo. */
	private Bingo bingo;
}

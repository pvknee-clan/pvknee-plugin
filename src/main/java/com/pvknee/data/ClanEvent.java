package com.pvknee.data;

import lombok.Data;

@Data
public class ClanEvent
{
	private String name;
	private String kind;
	private String metric;
	private String colour;
	private String startsAt;
	private String endsAt;
	private boolean hasTime;
}

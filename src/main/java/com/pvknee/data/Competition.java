package com.pvknee.data;

import lombok.Data;

@Data
public class Competition
{
	private String type;
	private String name;
	private String metric;
	private long womId;
	private String startsAt;
	private String endsAt;
	private String colour;
}

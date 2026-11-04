package com.pvknee.data;

import lombok.Data;

/**
 * One player's gain in a current competition, as the clan sheet last copied it from Wise Old Man.
 */
@Data
public class Standing
{
	private String name;
	private long gained;
}

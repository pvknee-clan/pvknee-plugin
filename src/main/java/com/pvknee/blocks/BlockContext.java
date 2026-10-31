package com.pvknee.blocks;

import com.pvknee.data.ClanData;
import lombok.Value;

/**
 * Shared state every block renderer can read.
 */
@Value
public class BlockContext
{
	ClanData data;
	ThemeColours colours;
	ImageLoader images;
}

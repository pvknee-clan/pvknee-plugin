package com.pvknee.blocks;

import com.pvknee.data.ClanData;
import com.pvknee.ui.ThemeColours;
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

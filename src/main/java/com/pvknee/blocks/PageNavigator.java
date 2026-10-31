package com.pvknee.blocks;

/**
 * Lets blocks open another page of the panel.
 */
public interface PageNavigator
{
	/**
	 * Whether the page exists and is visible to this member.
	 */
	boolean canOpen(String pageId);

	void open(String pageId);
}

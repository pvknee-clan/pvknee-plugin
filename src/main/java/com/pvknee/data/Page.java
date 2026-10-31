package com.pvknee.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Page
{
	private String id;
	private String title;
	private String icon;
	private boolean show = true;
	private String rank;
	private List<Block> blocks = new ArrayList<>();
}

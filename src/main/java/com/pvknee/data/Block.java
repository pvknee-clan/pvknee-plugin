package com.pvknee.data;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class Block
{
	private String type;
	private Map<String, String> settings = new HashMap<>();
}

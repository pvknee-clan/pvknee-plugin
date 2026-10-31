package com.pvknee.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Competitions
{
	private List<Competition> current = new ArrayList<>();
	private List<Competition> upcoming = new ArrayList<>();
}

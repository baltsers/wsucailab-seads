package QL;

import Dads.DadsQLUtil;

public class Variant {
	public static final int Framework_width =4;
	public static final int Framework_height = 16;
	public static double[][] MAP = { {-99, -99, -99, -99}, {-99, -99, -99, -99}, {-99, -99, -99, -99}, {-99, -99, -99, -99},
			{0, -99, -99, -99}, {0, -99, -99, -99}, {-99, -99, -99, -99}, {-99, -99, -99, -99},
			{ 0, 0, 0, 0 }, {-99, -99, -99, -99},{ 0, 0, 0, 0 }, {-99, -99, -99, -99},
			{ 0, 0, 0, 0 }, { 0, 0, 0, 0 },{ 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
	public static void loadFromFile(String mazeFile) {
		MAP=DadsQLUtil.getMAPFromFile(mazeFile, Framework_width, Framework_height);
	}
}

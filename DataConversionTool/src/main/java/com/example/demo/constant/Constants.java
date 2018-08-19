package com.example.demo.constant;

public final class Constants {

	private Constants() {
	}

	public static class Package {
		public static final String SEPARATOR = ".";
		public static final String CLASS_SUFFIX = ".class";
	}

	public static class Character {
		public static final String SLASH = "/";
		public static final String BLANK = "";
		public static final String COMMA = ",";
		public static final String PERIOD = ".";
		public static final String CRLF = "¥r¥n";
		public static final String LF = "¥n";
	}

	public static class Excel {
		/** エンティティ名称が記載されている行の番号. */
		public static final int ROW_OF_ENTITY_NAME = 2;
		/** セルの値を読み込み開始する列の番号. */
		public static final int COL_OF_ENTITY_NAME = 1;
		/** セルの値を読み込み開始する行の番号. */
		public static final int ROW_OF_CELL_START = 5;
		/** セルの値を読み込み開始する列の番号. */
		public static final int COL_OF_CELL_START = 2;
	}

}

package com.example.demo.form;

import lombok.Data;

@Data
public class ConvertForm {
	// 変換先情報
	private String tableName;
	private String columnName;
	private String typeName;
	// 変換元情報
	private int settingKbn;
	private String settingValue;

}

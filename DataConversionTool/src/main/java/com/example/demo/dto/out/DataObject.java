package com.example.demo.dto.out;

import java.util.Date;

import lombok.Data;

@Data
public class DataObject {
	private Integer id;
	private String name1;
	private String name2;
	private Boolean deleteFlg;
	private Date updatetime;

}

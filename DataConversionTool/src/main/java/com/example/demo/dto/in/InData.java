package com.example.demo.dto.in;

import java.util.Date;

import lombok.Data;

@Data
public class InData {
	private Integer seq;
	private String name;
	private String name2;
	private String kbn1;
	private String kbn2;
	private Boolean deleteFlag;
	private Date updateTime;

}

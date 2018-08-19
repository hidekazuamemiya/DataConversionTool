package com.example.demo.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class Settings {

	@Value("${settings.app.dtoIn}")
	public String inPath;

	@Value("${settings.app.dtoOut}")
	public String outPath;

	@Value("${settings.app.convertFile}")
	public String convertProperties;

	@Value("${settings.app.userFunction}")
	public String userFunction;

	@Value("${settings.app.dataIn}")
	public String inDataPath;

	@Value("${settings.app.afterFunctionService}")
	public String afterFunctionService;

	@Value("${settings.app.afterFunction}")
	public String afterFunction;
}

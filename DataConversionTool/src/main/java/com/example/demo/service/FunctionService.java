package com.example.demo.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

/**
 * ユーザファンクション定義
 */

@Service
public class FunctionService {

	public String substring(String value, String stNum, String cntNum) {
		int stIdx = Integer.parseInt(stNum);
		int cnt = Integer.parseInt(cntNum);
		if (cnt == 0) {
			cnt = value.length() - stIdx;
		}
		String str = value.substring(stIdx, stIdx + cnt);
		return str;
	}

	public String substring(String value, String stNum) {
		int stIdx = Integer.parseInt(stNum);
		String str = value.substring(stIdx);
		return str;
	}

	public String today() throws ParseException {
		String str = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
		return str;
	}

	public Boolean isDeleteFlg(String value) {
		Boolean bool = true;
		if (Integer.parseInt(value) == BooleanUtils.toInteger(false)) {
			bool = false;
		}
		return bool;
	}
}

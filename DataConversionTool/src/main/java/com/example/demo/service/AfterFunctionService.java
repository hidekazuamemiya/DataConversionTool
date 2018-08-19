package com.example.demo.service;

import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * アフターファンクション定義
 */

@Service
public class AfterFunctionService {

	public void afterFunction(List<Object> inData, List<Object> outData) {
		// ↓ PG領域



		// 変換先オブジェクト内容の確認
		for (Iterator iterator = outData.iterator(); iterator.hasNext();) {
			Object obj = (Object) iterator.next();
			System.out.println(obj.toString());
		}

	}

}

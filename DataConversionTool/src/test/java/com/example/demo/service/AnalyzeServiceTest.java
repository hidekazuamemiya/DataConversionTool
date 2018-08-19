package com.example.demo.service;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.demo.AbstractTest;
import com.example.demo.constant.Settings;
import com.example.demo.exception.ApplicationException;
import com.example.demo.util.ConvertUtil;

public class AnalyzeServiceTest extends AbstractTest {

	@Autowired
	AnalyzeService analyzeService;

	@Autowired
	private Settings settings;

	@Autowired
	ConvertUtil convertUtil;

	@Test
	public void test_getAnalyzeInfo_区分値0で何もしない() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "0,1+1";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals(null, result);
	}

	@Test
	public void test_getAnalyzeInfo_足し算の解析() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,1+1";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals(2, result);
	}

	@Test
	public void test_getAnalyzeInfo_引き算の解析() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,2-1";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals(1, result);
	}

	@Test
	public void test_getAnalyzeInfo_掛け算の解析() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,2*5";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals(10, result);
	}

	@Test
	public void test_getAnalyzeInfo_割り算の解析() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,10/2";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals(5, result);
	}

	@Test
	public void test_getAnalyzeInfo_括弧を使用した演算の解析() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,3*4-10/5+(5-4/2)";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals(13, result);
	}

	@Test
	public void test_getAnalyzeInfo_substringユーザファンクションの解析() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,substring(ABCDE,1,2)+(5-4/2)";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals("BC3", result);
	}

	@Test
	public void test_getAnalyzeInfo_substringユーザファンクションで指定インデックスから以降を取得() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,substring(ABCDE,1,0)+(5-4/2)";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals("BCDE3", result);
	}

	@Test
	public void test_getAnalyzeInfo_引数違いsubstringユーザファンクションの解析() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,substring(ABCDE,1)+(5-4/2)";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals("BCDE3", result);
	}

	@Test
	public void test_getAnalyzeInfo_todayユーザファンクションの解析() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,today()";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);

		// 年月日の比較
		Date actual = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").parse(result.toString());
		Date expected = new Date();

		SimpleDateFormat yyyy = new SimpleDateFormat("yyyy");
		SimpleDateFormat mm = new SimpleDateFormat("MM");
		SimpleDateFormat dd = new SimpleDateFormat("dd");

		assertEquals(yyyy.format(expected), yyyy.format(actual));
		assertEquals(mm.format(expected), mm.format(actual));
		assertEquals(dd.format(expected), dd.format(actual));
	}

	@Test
	public void test_getAnalyzeInfo_isDeleteFlgユーザファンクションでfalse() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,isDeleteFlg(0)";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals("false", result);
	}

	@Test
	public void test_getAnalyzeInfo_isDeleteFlgユーザファンクションでtrue() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,isDeleteFlg(1)";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals("true", result);
	}

	@Test
	public void test_文字列の直後にプラス演算子がある場合文字列結合を行う() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,substring(ABCDE,1)+(1-4/2)";

		Object result = analyzeService.getAnalyzeInfo(info, new Object(), functionList);
		assertEquals("BCDE-1", result);
	}

	@Test(expected = ApplicationException.class)
	public void test_文字列の直後にプラス以外の演算子が使用された場合エラー() throws Exception {
		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());
		String info = "1,substring(ABCDE,1)-(1-4/2)";

		analyzeService.getAnalyzeInfo(info, new Object(), functionList);
	}
}

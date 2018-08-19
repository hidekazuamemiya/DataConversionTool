package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.example.demo.AbstractTest;
import com.example.demo.form.ConvertForm;
import com.example.demo.form.ConvertWpForm;

public class ConvertControllerTest extends AbstractTest {

	@Test
	public void test_index_エントリー用の画面を表示する() throws Exception {
		mvc.perform(get("/contents"))
			.andExpect(status().isOk())
			.andExpect(view().name("index/contents"))
			.andReturn();
	}

	@Test
	public void test_messagesPost_画面から実行ボタンを押下しエントリー画面にリダイレクトする() throws Exception {
		List<ConvertForm> convertFormList = new ArrayList<>();
		ConvertForm convertForm = new ConvertForm();
		convertForm.setTableName("table1");
		convertForm.setColumnName("column1");
		convertForm.setTypeName("type1");
		convertForm.setSettingKbn(1);
		convertForm.setSettingValue("1+1");
		convertFormList.add(convertForm);

		ConvertWpForm convertWpForm = new ConvertWpForm();
		convertWpForm.setConvList(convertFormList);
		convertWpForm.setAfCheckBox("on");

		mvc.perform(post("/submit").flashAttr("convertWpForm",convertWpForm))
			.andExpect(redirectedUrl("/contents"))
			.andReturn();
	}



}

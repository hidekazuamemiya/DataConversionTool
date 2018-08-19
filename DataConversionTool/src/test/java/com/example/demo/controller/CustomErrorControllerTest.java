package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Test;

import com.example.demo.AbstractTest;

public class CustomErrorControllerTest extends AbstractTest {

	@Test
	public void test_エラー用の画面を表示する() throws Exception {
		mvc.perform(get("/error"))
			.andExpect(status().isOk())
			.andExpect(view().name("error/error"))
			.andReturn();
	}



}

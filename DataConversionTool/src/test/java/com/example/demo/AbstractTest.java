package com.example.demo;

import java.util.UUID;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractTest {

	protected MockMvc mvc;
	protected MockHttpSession mockHttpSession;

	@Autowired
	protected WebApplicationContext wac;

	@Before
	public void setup() throws Exception {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
        mockHttpSession = new MockHttpSession(wac.getServletContext(), UUID.randomUUID().toString());
	}
}

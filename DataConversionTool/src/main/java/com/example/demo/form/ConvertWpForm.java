package com.example.demo.form;

import java.util.List;

import javax.validation.constraints.Pattern;

import lombok.Data;

@Data
public class ConvertWpForm {

	private List<ConvertForm> convList;

    @Pattern(regexp="on")
    private String afCheckBox;
}

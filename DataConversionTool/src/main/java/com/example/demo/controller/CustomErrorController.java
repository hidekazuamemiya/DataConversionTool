package com.example.demo.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController extends AbstractErrorController {
    private static final String ERROR_PATH=  "/error";

    @Autowired
    public CustomErrorController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }

    @RequestMapping(ERROR_PATH)
    public String handleErrors(HttpServletRequest request, Model model) {
        HttpStatus status = getStatus(request);
        model.addAttribute("exception", getErrorAttributes(request, true));

        if (status.equals(HttpStatus.NOT_FOUND)) {
            return "error/error";
        }
        if (status.equals(HttpStatus.FORBIDDEN)) {
            return "error/error";
        }

        return "error/error";
    }

    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }
}
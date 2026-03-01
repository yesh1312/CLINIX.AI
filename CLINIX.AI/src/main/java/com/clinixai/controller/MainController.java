package com.clinixai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "CLINIX.AI | Advanced Medical Lab");
        return "index";
    }

    @GetMapping("/research")
    public String research() {
        return "research";
    }

    @GetMapping("/lab")
    public String lab() {
        return "lab";
    }
}

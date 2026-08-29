package io.github.aomckin.lifehud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the current single-page shell at every planned Life HUD route. */
@Controller
public final class ShellController {
    @GetMapping({"/", "/dashboard", "/focus", "/tasks", "/dreams", "/rituals", "/life", "/media", "/journal", "/now", "/growth", "/settings"})
    public String shell() { return "forward:/static/v0.2.html"; }
}

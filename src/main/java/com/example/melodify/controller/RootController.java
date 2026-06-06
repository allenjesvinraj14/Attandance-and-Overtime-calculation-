package com.example.melodify.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.io.IOException;

/**
 * Simple controller to serve the single‑page UI.
 */
@Controller
public class RootController {
    @GetMapping("/")
    public void redirectRoot(HttpServletResponse response) throws IOException {
        // Forward to the static index page bundled in resources/static
        response.sendRedirect("/index.html");
    }
}

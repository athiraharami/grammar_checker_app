package com.example.demo;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/grammar")
public class GrammarController {

    @PostMapping("/parse")
    public Map<String, Object> parseGrammar(@RequestBody Map<String, List<String>> grammarInput) {
        Map<String, Object> response = new HashMap<>();
        List<String> rules = grammarInput.get("grammar");

        if (rules == null || rules.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Grammar input cannot be empty.");
            return response;
        }

        // Create a new instance of RecursiveDescentParser for this request
        RecursiveDescentParser parser = new RecursiveDescentParser();
        String result = parser.runParser(rules);

        if (result.contains("error")) {
            response.put("status", "error");
            response.put("message", result);
        } else {
            response.put("status", "success");
            response.put("message", result);
        }

        return response;
    }
}


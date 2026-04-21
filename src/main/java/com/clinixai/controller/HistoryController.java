package com.clinixai.controller;

import com.clinixai.model.AnalysisHistory;
import com.clinixai.model.User;
import com.clinixai.repository.HistoryRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private HistoryRepository historyRepository;

    @GetMapping
    public ResponseEntity<?> getHistory(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }

        List<AnalysisHistory> history = historyRepository.findByUserOrderByTimestampDesc(user);
        return ResponseEntity.ok(history);
    }
}

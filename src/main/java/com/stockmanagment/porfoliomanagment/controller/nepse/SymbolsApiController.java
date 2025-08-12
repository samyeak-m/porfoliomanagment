package com.stockmanagment.porfoliomanagment.controller.nepse;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockmanagment.porfoliomanagment.service.nepse.lstm.database.DatabaseHelper;

@RestController
@RequestMapping("/api/stock")
public class SymbolsApiController {
    private final DatabaseHelper db;

    public SymbolsApiController(DatabaseHelper db) {
        this.db = db;
    }
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getStockSymbols(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        try {
            if (q == null || q.isBlank()) return ResponseEntity.ok(Collections.emptyList());
            return ResponseEntity.ok(db.getStockSymbolsByPrefix(q.trim(), limit));
        } catch (SQLException ex) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}
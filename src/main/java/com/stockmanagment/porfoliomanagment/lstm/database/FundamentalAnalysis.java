package com.stockmanagment.porfoliomanagment.lstm.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FundamentalAnalysis {
    private final DatabaseHelper dbHelper;

    public FundamentalAnalysis(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public double getEPS(String stockSymbol) throws SQLException {
        String query = "SELECT eps FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("eps");
                }
            }
        }
        return 0.0;
    }

    public double getPERatio(String stockSymbol) throws SQLException {
        String query = "SELECT pe_ratio FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("pe_ratio");
                }
            }
        }
        return 0.0;
    }

    public double getDividendYield(String stockSymbol) throws SQLException {
        String query = "SELECT dividend_yield FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("dividend_yield");
                }
            }
        }
        return 0.0;
    }

    public double getRevenueGrowth(String stockSymbol) throws SQLException {
        String query = "SELECT revenue_growth FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("revenue_growth");
                }
            }
        }
        return 0.0;
    }

    public double getEarningsGrowth(String stockSymbol) throws SQLException {
        String query = "SELECT earnings_growth FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("earnings_growth");
                }
            }
        }
        return 0.0;
    }

    public double getDebtToEquityRatio(String stockSymbol) throws SQLException {
        String query = "SELECT debt_to_equity FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("debt_to_equity");
                }
            }
        }
        return 0.0;
    }

    public double getPriceToBookRatio(String stockSymbol) throws SQLException {
        String query = "SELECT price_to_book FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price_to_book");
                }
            }
        }
        return 0.0;
    }

    public double getROE(String stockSymbol) throws SQLException {
        String query = "SELECT roe FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("roe");
                }
            }
        }
        return 0.0;
    }

    public double getFreeCashFlow(String stockSymbol) throws SQLException {
        String query = "SELECT free_cash_flow FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("free_cash_flow");
                }
            }
        }
        return 0.0;
    }

    public double getProfitMargins(String stockSymbol) throws SQLException {
        String query = "SELECT profit_margins FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("profit_margins");
                }
            }
        }
        return 0.0;
    }

    public double getEnterpriseValue(String stockSymbol) throws SQLException {
        String query = "SELECT enterprise_value FROM fundamental_data WHERE stock_symbol = ?";
        try (Connection conn = dbHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockSymbol);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("enterprise_value");
                }
            }
        }
        return 0.0;
    }
}

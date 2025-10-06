package com.stockmanagment.porfoliomanagment.service.nepse;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
@Transactional(readOnly = true)
public class CustomDailyDataRepositoryImpl implements CustomDailyDataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private String sanitize(String symbol) {
        return symbol == null ? null : symbol.replace('/', '_').toLowerCase();
    }

    @Override
    public DailyData getBySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) return null;
        String sym = sanitize(symbol);

        // Prefer today’s row if present
        List<DailyData> today = entityManager.createQuery(
                "SELECT d FROM DailyData d WHERE LOWER(d.symbol)=:sym AND d.date=:today ORDER BY d.date DESC",
                DailyData.class)
                .setParameter("sym", sym)
                .setParameter("today", LocalDate.now())
                .setMaxResults(1)
                .getResultList();
        if (!today.isEmpty()) return today.get(0);

        // Fallback: latest historical
        return entityManager.createQuery(
                "SELECT d FROM DailyData d WHERE LOWER(d.symbol)=:sym ORDER BY d.date DESC",
                DailyData.class)
                .setParameter("sym", sym)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<DailyData> getByDateRangeAndSymbol(String symbol, Timestamp startDate, Timestamp endDate) {
        LocalDate sd = startDate == null ? null : startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate ed = endDate == null ? null : endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        if (symbol != null && !symbol.isBlank()) {
            String sym = sanitize(symbol);
            String tableName = "daily_data_" + sym;

            @SuppressWarnings("unchecked")
            java.util.List<Object> tables = entityManager.createNativeQuery("SHOW TABLES LIKE :tbl")
                    .setParameter("tbl", tableName)
                    .getResultList();

            if (tables != null && !tables.isEmpty()) {
                StringBuilder sql = new StringBuilder("SELECT `date`, `open`, `high`, `low`, `close` FROM ").append(tableName).append(" WHERE 1=1");
                if (sd != null) sql.append(" AND `date` >= :start");
                if (ed != null) sql.append(" AND `date` <= :end");
                sql.append(" ORDER BY `date` ASC");

                jakarta.persistence.Query nativeQ = entityManager.createNativeQuery(sql.toString());
                if (sd != null) nativeQ.setParameter("start", Date.valueOf(sd));
                if (ed != null) nativeQ.setParameter("end", Date.valueOf(ed));

                @SuppressWarnings("unchecked")
                java.util.List<Object[]> rows = nativeQ.getResultList();

                java.util.List<DailyData> result = new java.util.ArrayList<>(rows.size());
                for (Object[] row : rows) {
                    DailyData dd = new DailyData();
                    if (row.length > 0 && row[0] != null) {
                        if (row[0] instanceof java.sql.Date) {
                            dd.setDate(((java.sql.Date) row[0]).toLocalDate());
                        } else if (row[0] instanceof java.util.Date) {
                            dd.setDate(new java.sql.Date(((java.util.Date) row[0]).getTime()).toLocalDate());
                        }
                    }
                    dd.setOpen(toDoubleSafe(row, 1));
                    dd.setHigh(toDoubleSafe(row, 2));
                    dd.setLow(toDoubleSafe(row, 3));
                    dd.setClose(toDoubleSafe(row, 4));
                    dd.setSymbol(symbol == null ? null : symbol.toUpperCase());
                    result.add(dd);
                }
                return result;
            }
        }

        // Fallback: shared table (JPQL) — existing behavior
        StringBuilder jpql = new StringBuilder("SELECT d FROM DailyData d WHERE 1=1");
        boolean hasSymbol = symbol != null && !symbol.isBlank();
        if (hasSymbol) jpql.append(" AND LOWER(d.symbol)=:sym");
        if (sd != null) jpql.append(" AND d.date >= :startDate");
        if (ed != null) jpql.append(" AND d.date <= :endDate");
        jpql.append(" ORDER BY d.date ASC, d.symbol ASC");

        TypedQuery<DailyData> q = entityManager.createQuery(jpql.toString(), DailyData.class);
        if (hasSymbol) q.setParameter("sym", sanitize(symbol));
        if (sd != null) q.setParameter("startDate", sd);
        if (ed != null) q.setParameter("endDate", ed);
        return q.getResultList();
    }

    @Override
    public List<DailyData> getByDate() {
        return entityManager.createQuery(
                "SELECT d FROM DailyData d WHERE d.date=:today ORDER BY d.symbol ASC",
                DailyData.class)
                .setParameter("today", LocalDate.now())
                .getResultList();
    }

    @Override
    public List<String> getAllSymbolsFromDailyData() {
        return entityManager.createQuery(
                        "SELECT DISTINCT d.symbol FROM DailyData d ORDER BY d.symbol ASC",
                        String.class)
                .getResultList();
    }

    // Added for DailyDataService.getLatestSharedDailyData()
    public DailyData getLatestBySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) return null;
        return entityManager.createQuery(
                        "SELECT d FROM DailyData d WHERE LOWER(d.symbol)=:sym ORDER BY d.date DESC",
                        DailyData.class)
                .setParameter("sym", sanitize(symbol))
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private double toDoubleSafe(Object[] row, int idx) {
        if (row == null || idx >= row.length) return 0.0;
        Object o = row[idx];
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}

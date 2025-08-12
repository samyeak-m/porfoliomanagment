package com.stockmanagment.porfoliomanagment.service.nepse;

import java.sql.Timestamp;
import java.time.LocalDate;
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
        StringBuilder jpql = new StringBuilder("SELECT d FROM DailyData d WHERE 1=1");
        boolean hasSymbol = symbol != null && !symbol.isBlank();
        if (hasSymbol) {
            jpql.append(" AND LOWER(d.symbol)=:sym");
        }
        if (startDate != null) {
            jpql.append(" AND d.date >= :startDate");
        }
        if (endDate != null) {
            jpql.append(" AND d.date <= :endDate");
        }
        jpql.append(" ORDER BY d.date ASC, d.symbol ASC");

        TypedQuery<DailyData> q = entityManager.createQuery(jpql.toString(), DailyData.class);

        if (hasSymbol) q.setParameter("sym", sanitize(symbol));
        if (startDate != null) q.setParameter("startDate", startDate.toLocalDateTime().toLocalDate());
        if (endDate != null) q.setParameter("endDate", endDate.toLocalDateTime().toLocalDate());

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
}

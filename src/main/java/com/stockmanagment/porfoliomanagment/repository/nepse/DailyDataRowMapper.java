package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DailyDataRowMapper implements RowMapper<DailyData> {

    @Override
    public DailyData mapRow(ResultSet rs, int rowNum) throws SQLException {
        DailyData dailyData = new DailyData();

        dailyData.setOpen(Double.valueOf(rs.getDouble("open")));
        dailyData.setHigh(Double.valueOf(rs.getDouble("high")));
        dailyData.setLow(Double.valueOf(rs.getDouble("low")));
        dailyData.setClose(Double.valueOf(rs.getDouble("close")));

        try {
            dailyData.setSymbol(rs.getString("symbol")); // Only for `daily_data` table
        } catch (SQLException ignored) {
            // Ignore symbol if not present (dynamic tables)
        }
        return dailyData;
    }
}

package com.adu.importer.repo;

import com.adu.importer.model.CloseoutRecord;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class CloseoutRepository {

    private static final String UPSERT_SQL =
            "INSERT INTO avb_closeout " +
            "  (avb_sku, closeout_id, linq_id, avb_status, closeout_type, avb_brand) " +
            "VALUES (?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "  closeout_id = VALUES(closeout_id), " +
            "  linq_id = VALUES(linq_id), " +
            "  avb_status = VALUES(avb_status), " +
            "  closeout_type = VALUES(closeout_type), " +
            "  avb_brand = VALUES(avb_brand)";

    private final JdbcTemplate jdbc;

    public CloseoutRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int upsertAll(List<CloseoutRecord> records) {
        if (records.isEmpty()) return 0;

        int[] results = jdbc.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CloseoutRecord r = records.get(i);
                ps.setString(1, r.avbSku());
                ps.setString(2, r.closeoutId());
                ps.setInt(3, r.linqId());
                ps.setString(4, r.avbStatus());
                ps.setString(5, r.closeoutType());
                ps.setString(6, r.avbBrand());
            }

            @Override
            public int getBatchSize() {
                return records.size();
            }
        });
        return results.length;
    }
}

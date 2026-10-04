package io.battery.scenario;

import java.sql.SQLException;
import java.util.List;

/**
 * Mixin classifying SQL exceptions as transient by their PostgreSQL SQL state code.
 * Only 40001 (serialization failure) is safe to retry for non-idempotent statements.
 */
public interface SQLExceptionClassifier {
    /**
     * Transient PostgresSQL SQL state codes considered safe to retry.
     * The exceptions are 08004 and 57P01 that are partially transient.
     */
    List<String> TRANSIENT_CODES = List.of(
            "40001", "08001", "08003", "08004", "08006", "08007", "08S01", "57P01", "40P01"
    );

    default boolean isTransient(SQLException ex) {
        String sqlState = ex.getSQLState();
        return sqlState != null && TRANSIENT_CODES.contains(sqlState);
    }
}

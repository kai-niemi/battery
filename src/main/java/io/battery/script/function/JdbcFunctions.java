package io.battery.script.function;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;

/**
 * SQL functions in the {@code jdbc} namespace for querying and updating, and for cached
 * batches of CockroachDB {@code unique_rowid()} IDs. Only registered when a data source
 * is provided.
 *
 * @see DefaultJdbcFunctions
 */
public interface JdbcFunctions {
    String NAMESPACE = "jdbc";

    @Description(value = """
            Executes a SQL query and returns a list of row tuples.
            """, volatility = Volatility.Volatile)
    List<Map<String, Object>> queryForList(String sql) throws DataAccessException;

    @Description(value = """
            Executes a SQL query with placeholder (?) parameters and returns a list of row tuples.
            """, volatility = Volatility.Volatile)
    List<Map<String, Object>> queryForList(String sql, Object... args) throws DataAccessException;

    @Description(value = """
            Execute a query for a result map. Expects a single row result, or none.
            """, volatility = Volatility.Volatile)
    Map<?,?> queryForMap(String sql) throws DataAccessException;

    @Description(value = """
            Execute a query for a result map. Expects a single row result, or none.
            """, volatility = Volatility.Volatile)
    Map<?,?> queryForMap(String sql, Object... args) throws DataAccessException;

    @Description(value = """
            Execute a query for a result object.
            """, volatility = Volatility.Volatile)
    Object queryForObject(String sql) throws DataAccessException;

    @Description(value = """
            Execute a query for a result object.
            """, volatility = Volatility.Volatile)
    Object queryForObject(String sql, Object... args) throws DataAccessException;

    @Description(value = """
            Issue a single SQL update operation (such as an insert, update or delete statement).
            Returns rows affected.
            """, volatility = Volatility.Volatile)
    int update(String sql) throws DataAccessException;

    @Description(value = """
            Issue a single SQL update operation (such as an insert, update or delete statement).
            Returns rows affected.
            """, volatility = Volatility.Volatile)
    int update(String sql, Object... args) throws DataAccessException;

    @Description(value = """
            Issue a single SQL update operation (such as an insert, update or delete statement).
            Returns rows affected.
            """, volatility = Volatility.Volatile)
    int update(String sql, Object[] args, String[] argTypes) throws DataAccessException;

    @Description(value = """
            Issue a single SQL insert operation and returns generated keys.
            Returns rows affected.
            """, volatility = Volatility.Volatile)
    Map<String,Object> updateForKeys(String sql) throws DataAccessException;

    @Description(value = """
            Issue a single SQL insert operation and returns generated keys.
            Returns rows affected.
            """, volatility = Volatility.Volatile)
    Map<String,Object> updateForKeys(String sql, Object... args) throws DataAccessException;

    @Description(value = """
            Issue a single SQL insert operation and returns generated keys.
            Returns rows affected.
            """, volatility = Volatility.Volatile)
    Map<String,Object> updateForKeys(String sql, Object[] args, String[] argTypes) throws DataAccessException;

    @Description(value = """
            Executes the unordered_unique_rowid() function in set-based function and returns
            the next unused value. The set is cached locally.
            """, volatility = Volatility.Volatile)
    long unorderedUniqueRowId(int batchSize);

    @Description(value = """
            Executes the unique_rowid() function in set-based function and returns
            the next unused value. The set is cached locally.
            """, volatility = Volatility.Volatile)
    long uniqueRowId(int batchSize);
}

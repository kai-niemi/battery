package io.battery.script.function;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jdbc.support.JdbcUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import static org.springframework.data.jdbc.support.JdbcUtil.TYPE_UNKNOWN;

/**
 * Default {@link JdbcFunctions} implementation using a {@code JdbcTemplate}, so that SQL
 * executed by scripts participates in any Spring transaction bound to the calling thread.
 * The row ID caches are shared by all threads.
 */
public class DefaultJdbcFunctions implements JdbcFunctions {
    private static final Map<String, Integer> typeIDs = new HashMap<>();

    static {
        try {
            for (Field field : Types.class.getFields()) {
                typeIDs.put(field.getName(), (Integer) field.get(null));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to resolve JDBC Types constants", ex);
        }
    }

    private final JdbcTemplate jdbcTemplate;

    public DefaultJdbcFunctions(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql)
            throws DataAccessException {
        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object... args)
            throws DataAccessException {
        return jdbcTemplate.queryForList(sql, args);
    }

    @Override
    public Map<String, Object> queryForMap(String sql)
            throws DataAccessException {
        try {
            return jdbcTemplate.queryForMap(sql);
        } catch (EmptyResultDataAccessException e) {
            return Map.of();
        }
    }

    @Override
    public Map<String, Object> queryForMap(String sql, Object... args)
            throws DataAccessException {
        try {
            return jdbcTemplate.queryForMap(sql, args);
        } catch (EmptyResultDataAccessException e) {
            return Map.of();
        }
    }

    @Override
    public Object queryForObject(String sql, Object... args)
            throws DataAccessException {
        return jdbcTemplate.queryForObject(sql, Object.class, args);
    }

    @Override
    public Object queryForObject(String sql) throws DataAccessException {
        return jdbcTemplate.queryForObject(sql, Object.class);
    }

    @Override
    public int update(String sql) {
        return jdbcTemplate.update(sql);
    }

    @Override
    public int update(String sql, Object... args) throws DataAccessException {
        return jdbcTemplate.update(sql, ps -> {
            bind(args, new String[0], ps);
        });
    }

    @Override
    public Map<String,Object> updateForKeys(String sql) throws DataAccessException {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        // Use a statement creator, since update(sql, keyHolder) would bind the key holder as a parameter
        jdbcTemplate.update(conn -> conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS), keyHolder);
        return keyHolder.getKeys();
    }

    @Override
    public Map<String,Object> updateForKeys(String sql, Object... args) throws DataAccessException {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(psc -> {
            PreparedStatement ps = psc.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            bind(args, new String[0], ps);
            return ps;
        }, keyHolder);

        return keyHolder.getKeys();
    }

    @Override
    public Map<String,Object> updateForKeys(String sql, Object[] args, String[] argTypes) throws DataAccessException {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(psc -> {
            PreparedStatement ps = psc.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            bind(args, argTypes, ps);
            return ps;
        }, keyHolder);

        return keyHolder.getKeys();
    }

    @Override
    public int update(String sql, Object[] args, String[] argTypes) throws DataAccessException {
        return jdbcTemplate.update(sql, args, sqlTypes(args, argTypes));
    }

    /**
     * Resolves {@link Types} names (case-insensitive) to SQL type codes, one per argument.
     * An empty array of type names means no types are specified.
     */
    private static int[] sqlTypes(Object[] args, String[] argTypes) {
        if (argTypes.length > 0 && argTypes.length != args.length) {
            throw new IllegalArgumentException("Expected %d argument types but got %d"
                    .formatted(args.length, argTypes.length));
        }
        return Arrays.stream(argTypes)
                .mapToInt(name -> {
                    Integer type = typeIDs.get(name.toUpperCase(Locale.ROOT));
                    if (type == null) {
                        throw new IllegalArgumentException(
                                "Unknown SQL type '%s', expected a java.sql.Types name such as INTEGER or VARCHAR"
                                        .formatted(name));
                    }
                    return type;
                })
                .toArray();
    }

    private void bind(Object[] args, String[] argTypes, PreparedStatement ps) throws SQLException {
        int[] sqlTypes = sqlTypes(args, argTypes);

        int parameterPosition = 1;

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Object[] valueArray) {
                // The SQL array type is inferred from the first non-null element
                final int argPosition = i + 1;
                Object firstElement = Arrays.stream(valueArray)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Cannot infer the SQL array type of argument %d from an empty or all-null array"
                                        .formatted(argPosition)));

                SQLType type = JdbcUtil.targetSqlTypeFor(firstElement.getClass());
                String typeName = type.getName();
                if (type.equals(TYPE_UNKNOWN)) {
                    if (firstElement instanceof UUID) {
                        typeName = "UUID";
                    }
                }

                ps.setArray(parameterPosition, ps.getConnection().createArrayOf(typeName, valueArray));
                parameterPosition++;
            } else {
                if (sqlTypes.length > 0) {
                    ps.setObject(parameterPosition, arg, sqlTypes[i]);
                } else {
                    ps.setObject(parameterPosition, arg);
                }
                parameterPosition++;
            }
        }
    }

    // Row ID caches shared by all virtual user threads
    private static final Deque<Long> unorderedUniqueRowIds = new ConcurrentLinkedDeque<>();

    private static final Deque<Long> uniqueRowIds = new ConcurrentLinkedDeque<>();

    @Override
    public long unorderedUniqueRowId(int batchSize) {
        return nextRowId(unorderedUniqueRowIds, "unordered_unique_rowid", batchSize);
    }

    @Override
    public long uniqueRowId(int batchSize) {
        return nextRowId(uniqueRowIds, "unique_rowid", batchSize);
    }

    private long nextRowId(Deque<Long> cache, String function, int batchSize) {
        Long id = cache.poll();
        if (id != null) {
            return id;
        }

        // Concurrent refills may fetch more IDs than needed, but each cached ID is polled by
        // exactly one caller. Keep the first ID of the batch so this caller isn't starved.
        List<Long> ids = jdbcTemplate.queryForList("select %s() from generate_series(1, %d)"
                .formatted(function, batchSize), Long.class);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("No row IDs generated for batch size " + batchSize);
        }
        cache.addAll(ids.subList(1, ids.size()));
        return ids.getFirst();
    }
}


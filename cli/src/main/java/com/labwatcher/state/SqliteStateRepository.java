package com.labwatcher.state;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQLite-backed implementation of {@link StateRepository}. */
public final class SqliteStateRepository implements StateRepository {

    private static final String SCHEMA = """
        CREATE TABLE IF NOT EXISTS processed_files (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            file_path     TEXT NOT NULL,
            file_name     TEXT NOT NULL,
            sha256        TEXT NOT NULL UNIQUE,
            file_size     INTEGER NOT NULL,
            processed_at  TEXT NOT NULL,
            status        TEXT NOT NULL,
            row_count     INTEGER,
            column_count  INTEGER,
            error_count   INTEGER DEFAULT 0,
            warning_count INTEGER DEFAULT 0
        );
        CREATE INDEX IF NOT EXISTS idx_processed_at
            ON processed_files(processed_at DESC);
        CREATE INDEX IF NOT EXISTS idx_status
            ON processed_files(status);
        """;

    private final Connection conn;

    public SqliteStateRepository(String jdbcUrl) {
        try {
            this.conn = DriverManager.getConnection(jdbcUrl);
            try (Statement st = conn.createStatement()) {
                // WAL mode lets the dashboard read concurrently with the
                // watcher's writes — vital because the dashboard polls
                // every 2s and the writer would otherwise block it.
                // Skip for in-memory databases (WAL is meaningless there).
                if (!jdbcUrl.contains(":memory:")) {
                    st.execute("PRAGMA journal_mode=WAL");
                    st.execute("PRAGMA synchronous=NORMAL");
                }
                for (String sql : SCHEMA.split(";")) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) st.execute(trimmed);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("failed to open SQLite at " + jdbcUrl, e);
        }
    }

    /** Convenience constructor for a file path. */
    public static SqliteStateRepository forFile(String dbPath) {
        return new SqliteStateRepository("jdbc:sqlite:" + dbPath);
    }

    /** In-memory database, useful for tests. */
    public static SqliteStateRepository inMemory() {
        return new SqliteStateRepository("jdbc:sqlite::memory:");
    }

    @Override
    public long insert(ProcessedFile e) {
        final String sql = """
            INSERT INTO processed_files
              (file_path, file_name, sha256, file_size, processed_at,
               status, row_count, column_count, error_count, warning_count)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.filePath());
            ps.setString(2, e.fileName());
            ps.setString(3, e.sha256());
            ps.setLong(4, e.fileSize());
            ps.setString(5, e.processedAt().toString());
            ps.setString(6, e.status());
            ps.setLong(7, e.rowCount());
            ps.setLong(8, e.columnCount());
            ps.setLong(9, e.errorCount());
            ps.setLong(10, e.warningCount());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("insert failed", ex);
        }
    }

    @Override
    public Optional<ProcessedFile> findByHash(String sha256) {
        final String sql = "SELECT * FROM processed_files WHERE sha256 = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sha256);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("query failed", ex);
        }
    }

    @Override
    public List<ProcessedFile> recent(int limit) {
        // processed_at is stored as second-precision ISO strings, so two
        // files ingested in the same second would sort non-deterministically
        // and could even be swapped between calls. Break ties on the
        // monotonic primary key so recent() matches the dashboard's
        // id-ordered `recent` query.
        final String sql = "SELECT * FROM processed_files "
                         + "ORDER BY processed_at DESC, id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProcessedFile> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("query failed", ex);
        }
    }

    @Override
    public long countByStatus(String status) {
        return scalar("SELECT COUNT(*) FROM processed_files WHERE status = ?", status);
    }

    @Override
    public long total() {
        return scalar("SELECT COUNT(*) FROM processed_files", null);
    }

    private long scalar(String sql, String arg) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (arg != null) ps.setString(1, arg);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("query failed", ex);
        }
    }

    private static ProcessedFile map(ResultSet rs) throws SQLException {
        return new ProcessedFile(
            rs.getLong("id"),
            rs.getString("file_path"),
            rs.getString("file_name"),
            rs.getString("sha256"),
            rs.getLong("file_size"),
            Instant.parse(rs.getString("processed_at")),
            rs.getString("status"),
            rs.getLong("row_count"),
            rs.getLong("column_count"),
            rs.getLong("error_count"),
            rs.getLong("warning_count")
        );
    }

    @Override
    public void close() {
        try { conn.close(); } catch (SQLException ignored) {}
    }
}

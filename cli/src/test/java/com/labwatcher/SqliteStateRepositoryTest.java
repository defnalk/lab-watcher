package com.labwatcher;

import com.labwatcher.state.ProcessedFile;
import com.labwatcher.state.SqliteStateRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteStateRepositoryTest {

    private static ProcessedFile entry(String name, String sha, String status) {
        return ProcessedFile.newEntry("/tmp/" + name, name, sha, 1024L,
            status, 100, 6, "PASS".equals(status) ? 0 : 3, 0);
    }

    @Test void insertAndQueryByHash() {
        try (SqliteStateRepository r = SqliteStateRepository.inMemory()) {
            long id = r.insert(entry("a.csv", "abc", "PASS"));
            assertThat(id).isPositive();
            assertThat(r.findByHash("abc")).isPresent();
            assertThat(r.findByHash("zzz")).isEmpty();
        }
    }

    @Test void recentAndCounts() {
        try (SqliteStateRepository r = SqliteStateRepository.inMemory()) {
            r.insert(entry("a.csv", "h1", "PASS"));
            r.insert(entry("b.csv", "h2", "FAIL"));
            r.insert(entry("c.csv", "h3", "PASS"));
            assertThat(r.total()).isEqualTo(3);
            assertThat(r.countByStatus("PASS")).isEqualTo(2);
            assertThat(r.countByStatus("FAIL")).isEqualTo(1);
            assertThat(r.recent(10)).hasSize(3);
        }
    }
}

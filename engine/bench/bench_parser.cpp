// Micro-benchmark for valengine::parse_csv_from_string.
// Generates an N-row, M-column CSV in memory and parses it K iterations,
// reporting throughput in rows/sec and MB/sec. No external benchmark
// framework — std::chrono is sufficient and keeps the dep tree empty.

#include "valengine/parser.hpp"
#include <chrono>
#include <cstdio>
#include <random>
#include <sstream>
#include <string>

namespace {

std::string generate_csv(int rows, int extra_cols) {
    std::ostringstream out;
    out << "timestamp,TT101,TT102,FT103,LT101,AT101";
    for (int c = 0; c < extra_cols; ++c) out << ",X" << c;
    out << '\n';
    std::mt19937 rng(0xC0FFEE);
    std::uniform_real_distribution<double> tt(80.0, 90.0);
    std::uniform_real_distribution<double> ft(850.0, 950.0);
    std::uniform_real_distribution<double> lt(40.0, 60.0);
    std::uniform_real_distribution<double> at(1.0, 3.0);
    for (int r = 0; r < rows; ++r) {
        out << "2025-01-15 08:00:" << (r % 60)
            << "," << tt(rng)
            << "," << tt(rng)
            << "," << ft(rng)
            << "," << lt(rng)
            << "," << at(rng);
        for (int c = 0; c < extra_cols; ++c) out << "," << at(rng);
        out << '\n';
    }
    return out.str();
}

}  // namespace

int main(int argc, char** argv) {
    int rows = (argc > 1) ? std::atoi(argv[1]) : 10000;
    int iters = (argc > 2) ? std::atoi(argv[2]) : 50;
    int extras = (argc > 3) ? std::atoi(argv[3]) : 0;

    std::string csv = generate_csv(rows, extras);
    const double mb = static_cast<double>(csv.size()) / (1024.0 * 1024.0);
    std::printf("input: %d rows × %d cols, %.2f MB\n", rows, 6 + extras, mb);

    // Warm-up
    (void) valengine::parse_csv_from_string(csv);

    auto start = std::chrono::steady_clock::now();
    int64_t total_rows = 0;
    for (int i = 0; i < iters; ++i) {
        auto r = valengine::parse_csv_from_string(csv);
        total_rows += r.total_rows;
    }
    auto end = std::chrono::steady_clock::now();

    double secs = std::chrono::duration<double>(end - start).count();
    double rows_per_sec = static_cast<double>(total_rows) / secs;
    double mb_per_sec = (mb * iters) / secs;
    double per_iter_ms = (secs / iters) * 1000.0;

    std::printf("iters: %d\n", iters);
    std::printf("wall:  %.3f s (%.2f ms/iter)\n", secs, per_iter_ms);
    std::printf("throughput: %.0f rows/sec, %.2f MB/sec\n", rows_per_sec, mb_per_sec);
    return 0;
}

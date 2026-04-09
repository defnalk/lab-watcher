#pragma once
#include "valengine/types.hpp"
#include <vector>
#include <limits>

namespace valengine {

/// Welford's online algorithm for numerically stable variance.
struct WelfordAccumulator {
    int64_t count = 0;
    double mean = 0.0;
    double m2 = 0.0;
    double min_val = std::numeric_limits<double>::infinity();
    double max_val = -std::numeric_limits<double>::infinity();

    void update(double x);
    [[nodiscard]] double variance() const;
    [[nodiscard]] double stddev() const;
};

/// Compute statistics for a single numeric column. NaN values are treated
/// as nulls (counted but excluded from min/max/mean/stddev).
[[nodiscard]] ColumnStats compute_column_stats(const std::string& name,
                                                 const std::vector<double>& values);

}  // namespace valengine

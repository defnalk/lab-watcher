#include "valengine/stats.hpp"
#include <cmath>
#include <limits>

namespace valengine {

void WelfordAccumulator::update(double x) {
    if (std::isnan(x)) return;
    ++count;
    const double delta = x - mean;
    mean += delta / static_cast<double>(count);
    const double delta2 = x - mean;
    m2 += delta * delta2;
    if (x < min_val) min_val = x;
    if (x > max_val) max_val = x;
}

double WelfordAccumulator::variance() const {
    if (count < 2) return 0.0;
    return m2 / static_cast<double>(count - 1);
}

double WelfordAccumulator::stddev() const {
    return std::sqrt(variance());
}

ColumnStats compute_column_stats(const std::string& name,
                                  const std::vector<double>& values) {
    ColumnStats s;
    s.name = name;
    s.type = ColumnType::FLOAT;
    WelfordAccumulator acc;
    for (double v : values) {
        if (std::isnan(v)) {
            ++s.null_count;
        } else {
            acc.update(v);
        }
    }
    s.non_null_count = acc.count;
    if (acc.count > 0) {
        s.min_value = acc.min_val;
        s.max_value = acc.max_val;
        s.mean = acc.mean;
        s.stddev = acc.stddev();
    } else {
        const double nan = std::numeric_limits<double>::quiet_NaN();
        s.min_value = s.max_value = s.mean = s.stddev = nan;
    }
    return s;
}

}  // namespace valengine

#include <gtest/gtest.h>
#include "valengine/stats.hpp"
#include <cmath>

using namespace valengine;

TEST(Welford, KnownSequence) {
    WelfordAccumulator a;
    for (double v : {2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0}) a.update(v);
    EXPECT_EQ(a.count, 8);
    EXPECT_NEAR(a.mean, 5.0, 1e-9);
    EXPECT_NEAR(a.stddev(), 2.138089935299395, 1e-9);  // sample stddev
    EXPECT_DOUBLE_EQ(a.min_val, 2.0);
    EXPECT_DOUBLE_EQ(a.max_val, 9.0);
}

TEST(ColumnStats, NaNTreatedAsNull) {
    auto s = compute_column_stats("x", {1.0, std::nan(""), 3.0});
    EXPECT_EQ(s.non_null_count, 2);
    EXPECT_EQ(s.null_count, 1);
    EXPECT_NEAR(s.mean, 2.0, 1e-9);
}

TEST(ColumnStats, AllNullProducesNaN) {
    auto s = compute_column_stats("x", {std::nan(""), std::nan("")});
    EXPECT_TRUE(std::isnan(s.mean));
    EXPECT_EQ(s.null_count, 2);
}

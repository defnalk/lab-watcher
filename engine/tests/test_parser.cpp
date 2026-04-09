#include <gtest/gtest.h>
#include "valengine/parser.hpp"

using namespace valengine;

TEST(Parser, BasicCommaCsv) {
    std::string csv = "timestamp,TT101,FT103\n"
                      "2025-01-01 00:00:00,85.2,900.3\n"
                      "2025-01-01 00:00:30,85.4,898.7\n";
    auto r = parse_csv_from_string(csv);
    ASSERT_TRUE(r.success);
    EXPECT_EQ(r.headers.size(), 3u);
    EXPECT_EQ(r.headers[1], "TT101");
    EXPECT_EQ(r.total_rows, 2);
    ASSERT_EQ(r.column_stats.size(), 3u);
    EXPECT_EQ(r.column_stats[0].type, ColumnType::STRING);
    EXPECT_EQ(r.column_stats[1].type, ColumnType::FLOAT);
    EXPECT_NEAR(r.column_stats[1].mean, 85.3, 1e-9);
}

TEST(Parser, SemicolonDelimiter) {
    auto r = parse_csv_from_string("a;b\n1;2\n3;4\n");
    ASSERT_TRUE(r.success);
    EXPECT_EQ(r.headers.size(), 2u);
    EXPECT_EQ(r.total_rows, 2);
}

TEST(Parser, BomStripped) {
    std::string s = std::string("\xEF\xBB\xBF") + "a,b\n1,2\n";
    auto r = parse_csv_from_string(s);
    ASSERT_TRUE(r.success);
    EXPECT_EQ(r.headers[0], "a");
}

TEST(Parser, QuotedFieldsWithEscapes) {
    auto r = parse_csv_from_string("name,note\n\"hi\",\"a,\"\"b\"\"\"\n");
    ASSERT_TRUE(r.success);
    EXPECT_EQ(r.total_rows, 1);
    ASSERT_EQ(r.raw_columns.size(), 2u);
    EXPECT_EQ(r.raw_columns[1][0], "a,\"b\"");
}

TEST(Parser, BlankLinesSkipped) {
    auto r = parse_csv_from_string("a,b\n1,2\n\n3,4\n");
    ASSERT_TRUE(r.success);
    EXPECT_EQ(r.total_rows, 2);
    EXPECT_EQ(r.skipped_rows, 1);
}

TEST(Parser, EmptyContent) {
    auto r = parse_csv_from_string("");
    EXPECT_FALSE(r.success);
}

#include <gtest/gtest.h>
#include "valengine/schema.hpp"

using namespace valengine;

TEST(Schema, ParsesBasicToml) {
    std::string toml =
        "name = \"mea\"\n"
        "version = \"1.0\"\n"
        "require_timestamp_column = true\n"
        "timestamp_column_name = \"timestamp\"\n"
        "allow_extra_columns = false\n"
        "\n"
        "[[columns]]\n"
        "name = \"TT101\"\n"
        "type = \"float\"\n"
        "required = true\n"
        "min_value = -10.0\n"
        "max_value = 200.0\n"
        "\n"
        "[[columns]]\n"
        "name = \"FT103\"\n"
        "type = \"float\"\n"
        "min_value = 0.0\n"
        "max_value = 2000.0\n";
    auto sp = load_schema_from_string(toml);
    EXPECT_EQ(sp.name, "mea");
    EXPECT_EQ(sp.version, "1.0");
    EXPECT_FALSE(sp.allow_extra_columns);
    ASSERT_EQ(sp.columns.size(), 2u);
    EXPECT_EQ(sp.columns[0].name, "TT101");
    EXPECT_EQ(sp.columns[0].type, ColumnType::FLOAT);
    ASSERT_TRUE(sp.columns[0].min_value.has_value());
    EXPECT_DOUBLE_EQ(*sp.columns[0].min_value, -10.0);
    EXPECT_DOUBLE_EQ(*sp.columns[0].max_value, 200.0);
    EXPECT_EQ(sp.columns[1].name, "FT103");
}

TEST(Schema, IgnoresCommentsAndBlankLines) {
    auto sp = load_schema_from_string("# top comment\nname = \"x\"  # trailing\n");
    EXPECT_EQ(sp.name, "x");
}

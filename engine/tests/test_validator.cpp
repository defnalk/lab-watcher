#include <gtest/gtest.h>
#include "valengine/parser.hpp"
#include "valengine/schema.hpp"
#include "valengine/validator.hpp"

using namespace valengine;

namespace {
SchemaSpec mea_schema() {
    return load_schema_from_string(
        "name=\"mea\"\nversion=\"1.0\"\n"
        "require_timestamp_column=true\ntimestamp_column_name=\"timestamp\"\n"
        "allow_extra_columns=true\n"
        "[[columns]]\nname=\"TT101\"\ntype=\"float\"\nrequired=true\n"
        "min_value=-10.0\nmax_value=200.0\n"
        "[[columns]]\nname=\"FT103\"\ntype=\"float\"\nrequired=true\n"
        "min_value=0.0\nmax_value=2000.0\n");
}
}

TEST(Validator, ValidPasses) {
    auto p = parse_csv_from_string(
        "timestamp,TT101,FT103\n"
        "2025-01-01 00:00:00,85.2,900.3\n"
        "2025-01-01 00:00:30,86.0,910.0\n");
    auto r = validate(p, mea_schema());
    EXPECT_TRUE(r.passed) << "errors: " << r.errors.size();
    EXPECT_EQ(r.error_count, 0);
}

TEST(Validator, MissingRequiredColumn) {
    auto p = parse_csv_from_string("timestamp,TT101\n2025-01-01 00:00:00,85.0\n");
    auto r = validate(p, mea_schema());
    EXPECT_FALSE(r.passed);
    bool found = false;
    for (const auto& e : r.errors)
        if (e.code == ErrorCode::MISSING_REQUIRED_COLUMN && e.column == "FT103") found = true;
    EXPECT_TRUE(found);
}

TEST(Validator, OutOfRange) {
    auto p = parse_csv_from_string(
        "timestamp,TT101,FT103\n"
        "2025-01-01 00:00:00,250.0,900.0\n");
    auto r = validate(p, mea_schema());
    EXPECT_FALSE(r.passed);
    bool found = false;
    for (const auto& e : r.errors)
        if (e.code == ErrorCode::VALUE_OUT_OF_RANGE && e.column == "TT101") found = true;
    EXPECT_TRUE(found);
}

TEST(Validator, TypeMismatch) {
    auto p = parse_csv_from_string(
        "timestamp,TT101,FT103\n"
        "2025-01-01 00:00:00,N/A,900.0\n");
    auto r = validate(p, mea_schema());
    EXPECT_FALSE(r.passed);
    bool found = false;
    for (const auto& e : r.errors)
        if (e.code == ErrorCode::TYPE_MISMATCH && e.column == "TT101") found = true;
    EXPECT_TRUE(found);
}

TEST(Validator, NullInRequired) {
    auto p = parse_csv_from_string(
        "timestamp,TT101,FT103\n"
        "2025-01-01 00:00:00,,900.0\n");
    auto r = validate(p, mea_schema());
    EXPECT_FALSE(r.passed);
    bool found = false;
    for (const auto& e : r.errors)
        if (e.code == ErrorCode::NULL_IN_REQUIRED && e.column == "TT101") found = true;
    EXPECT_TRUE(found);
}

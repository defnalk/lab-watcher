#pragma once
#include <string>
#include <vector>
#include <cstdint>
#include <optional>
#include <limits>

namespace valengine {

enum class ColumnType { STRING, INTEGER, FLOAT, TIMESTAMP };
enum class Severity { WARN, ERROR_ };
enum class ErrorCode {
    MISSING_REQUIRED_COLUMN,
    UNEXPECTED_COLUMN,
    TYPE_MISMATCH,
    VALUE_OUT_OF_RANGE,
    NULL_IN_REQUIRED,
    DUPLICATE_HEADER,
    EMPTY_FILE,
    PARSE_FAILURE
};

struct ColumnSpec {
    std::string name;
    ColumnType type = ColumnType::FLOAT;
    bool required = true;
    std::optional<double> min_value;
    std::optional<double> max_value;
    bool allow_null = false;
};

struct SchemaSpec {
    std::string name;
    std::string version;
    bool require_timestamp_column = true;
    std::string timestamp_column_name = "timestamp";
    std::vector<ColumnSpec> columns;
    bool allow_extra_columns = true;
};

struct ValidationError {
    int line = 0;
    std::string column;
    ErrorCode code = ErrorCode::PARSE_FAILURE;
    Severity severity = Severity::ERROR_;
    std::string message;
};

struct ColumnStats {
    std::string name;
    ColumnType type = ColumnType::FLOAT;
    int64_t non_null_count = 0;
    int64_t null_count = 0;
    double min_value = std::numeric_limits<double>::quiet_NaN();
    double max_value = std::numeric_limits<double>::quiet_NaN();
    double mean = std::numeric_limits<double>::quiet_NaN();
    double stddev = std::numeric_limits<double>::quiet_NaN();
};

struct ParseResult {
    int64_t total_rows = 0;
    int64_t valid_rows = 0;
    int64_t skipped_rows = 0;
    std::vector<std::string> headers;
    std::vector<ColumnStats> column_stats;
    bool success = false;
    std::string error_message;
    // Raw per-column values kept only during parsing; not serialized.
    std::vector<std::vector<std::string>> raw_columns;
};

struct ValidationReport {
    bool passed = false;
    int64_t error_count = 0;
    int64_t warning_count = 0;
    std::vector<ValidationError> errors;
    ParseResult parse_result;
};

[[nodiscard]] const char* error_code_name(ErrorCode c);
[[nodiscard]] const char* column_type_name(ColumnType t);

}  // namespace valengine

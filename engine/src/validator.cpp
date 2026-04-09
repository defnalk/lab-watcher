#include "valengine/validator.hpp"
#include "valengine/parser.hpp"
#include "valengine/schema.hpp"
#include <unordered_map>
#include <unordered_set>
#include <sstream>
#include <cstdlib>
#include <cmath>

namespace valengine {

namespace {

ValidationError make_err(int line, std::string col, ErrorCode code,
                         Severity sev, std::string msg) {
    ValidationError e;
    e.line = line;
    e.column = std::move(col);
    e.code = code;
    e.severity = sev;
    e.message = std::move(msg);
    return e;
}

bool try_parse_double_strict(const std::string& s, double& out) {
    if (s.empty()) return false;
    const char* begin = s.c_str();
    char* end = nullptr;
    out = std::strtod(begin, &end);
    if (end == begin) return false;
    while (*end == ' ' || *end == '\t' || *end == '\r') ++end;
    return *end == '\0';
}

}  // namespace

ValidationReport validate(const ParseResult& parsed, const SchemaSpec& schema) {
    ValidationReport report;
    report.parse_result = parsed;
    // Drop raw_columns from the embedded copy to keep memory bounded; we
    // still have access via the local `parsed` reference for value checks.
    report.parse_result.raw_columns.clear();

    if (!parsed.success) {
        report.errors.push_back(make_err(0, "", ErrorCode::PARSE_FAILURE,
            Severity::ERROR_, parsed.error_message));
        report.error_count = 1;
        report.passed = false;
        return report;
    }
    if (parsed.headers.empty() || parsed.total_rows == 0) {
        report.errors.push_back(make_err(0, "", ErrorCode::EMPTY_FILE,
            Severity::ERROR_, "file has no data rows"));
    }

    // Duplicate header check.
    std::unordered_map<std::string, int> seen;
    for (const auto& h : parsed.headers) {
        if (++seen[h] == 2) {
            report.errors.push_back(make_err(0, h, ErrorCode::DUPLICATE_HEADER,
                Severity::ERROR_, "duplicate header: " + h));
        }
    }

    // Map header name -> index.
    std::unordered_map<std::string, size_t> idx;
    for (size_t i = 0; i < parsed.headers.size(); ++i) idx[parsed.headers[i]] = i;

    // Required column presence (and timestamp).
    if (schema.require_timestamp_column &&
        idx.find(schema.timestamp_column_name) == idx.end()) {
        report.errors.push_back(make_err(0, schema.timestamp_column_name,
            ErrorCode::MISSING_REQUIRED_COLUMN, Severity::ERROR_,
            "missing required timestamp column: " + schema.timestamp_column_name));
    }
    std::unordered_set<std::string> schema_names;
    for (const auto& col : schema.columns) {
        schema_names.insert(col.name);
        if (col.required && idx.find(col.name) == idx.end()) {
            report.errors.push_back(make_err(0, col.name,
                ErrorCode::MISSING_REQUIRED_COLUMN, Severity::ERROR_,
                "missing required column: " + col.name));
        }
    }
    if (!schema.allow_extra_columns) {
        for (const auto& h : parsed.headers) {
            if (h == schema.timestamp_column_name) continue;
            if (schema_names.find(h) == schema_names.end()) {
                report.errors.push_back(make_err(0, h, ErrorCode::UNEXPECTED_COLUMN,
                    Severity::WARN, "unexpected column: " + h));
            }
        }
    }

    // Per-cell range/null/type checks.
    for (const auto& col : schema.columns) {
        auto it = idx.find(col.name);
        if (it == idx.end()) continue;
        const size_t ci = it->second;
        if (ci >= parsed.raw_columns.size()) continue;
        const auto& values = parsed.raw_columns[ci];
        for (size_t r = 0; r < values.size(); ++r) {
            const std::string& cell = values[r];
            const int line = static_cast<int>(r + 2);  // +1 header, +1 1-indexed
            if (cell.empty()) {
                if (!col.allow_null) {
                    report.errors.push_back(make_err(line, col.name,
                        ErrorCode::NULL_IN_REQUIRED, Severity::ERROR_,
                        "null in non-nullable column"));
                }
                continue;
            }
            if (col.type == ColumnType::FLOAT || col.type == ColumnType::INTEGER) {
                double v;
                if (!try_parse_double_strict(cell, v)) {
                    report.errors.push_back(make_err(line, col.name,
                        ErrorCode::TYPE_MISMATCH, Severity::ERROR_,
                        "expected " + std::string(column_type_name(col.type)) +
                        ", got: " + cell));
                    continue;
                }
                if (col.min_value && v < *col.min_value) {
                    std::ostringstream m;
                    m << "value " << v << " below min " << *col.min_value;
                    report.errors.push_back(make_err(line, col.name,
                        ErrorCode::VALUE_OUT_OF_RANGE, Severity::ERROR_, m.str()));
                }
                if (col.max_value && v > *col.max_value) {
                    std::ostringstream m;
                    m << "value " << v << " above max " << *col.max_value;
                    report.errors.push_back(make_err(line, col.name,
                        ErrorCode::VALUE_OUT_OF_RANGE, Severity::ERROR_, m.str()));
                }
            }
        }
    }

    for (const auto& e : report.errors) {
        if (e.severity == Severity::ERROR_) ++report.error_count;
        else ++report.warning_count;
    }
    report.passed = (report.error_count == 0);
    return report;
}

ValidationReport parse_and_validate(const std::string& file_path,
                                     const std::string& schema_path) {
    ParseResult pr = parse_csv(file_path);
    SchemaSpec sp = load_schema(schema_path);
    return validate(pr, sp);
}

}  // namespace valengine

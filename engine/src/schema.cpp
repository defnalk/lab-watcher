#include "valengine/schema.hpp"
#include <fstream>
#include <sstream>
#include <stdexcept>
#include <cstdlib>

// Minimal TOML subset parser. Supports:
//   - line comments starting with '#'
//   - top-level key = value pairs (string, bool, int, float)
//   - [[columns]] array-of-tables, with subsequent key = value lines
//   - quoted strings: "..." (no escape handling beyond \" and \\)
// This is intentionally limited to what schema files require, in lieu of
// pulling in a full TOML library for the vertical slice.

namespace valengine {

namespace {

std::string trim(const std::string& s) {
    size_t a = 0, b = s.size();
    while (a < b && (s[a] == ' ' || s[a] == '\t' || s[a] == '\r')) ++a;
    while (b > a && (s[b - 1] == ' ' || s[b - 1] == '\t' || s[b - 1] == '\r')) --b;
    return s.substr(a, b - a);
}

std::string strip_comment(const std::string& s) {
    bool in_str = false;
    for (size_t i = 0; i < s.size(); ++i) {
        if (s[i] == '"') in_str = !in_str;
        else if (s[i] == '#' && !in_str) return s.substr(0, i);
    }
    return s;
}

std::string unquote(const std::string& v) {
    if (v.size() >= 2 && v.front() == '"' && v.back() == '"') {
        std::string out;
        for (size_t i = 1; i + 1 < v.size(); ++i) {
            if (v[i] == '\\' && i + 2 < v.size()) {
                char n = v[i + 1];
                if (n == '"' || n == '\\') { out += n; ++i; continue; }
            }
            out += v[i];
        }
        return out;
    }
    return v;
}

bool parse_bool(const std::string& v) {
    return v == "true";
}

double parse_number(const std::string& v) {
    return std::strtod(v.c_str(), nullptr);
}

ColumnType parse_type(const std::string& s) {
    if (s == "float")     return ColumnType::FLOAT;
    if (s == "integer" || s == "int") return ColumnType::INTEGER;
    if (s == "timestamp") return ColumnType::TIMESTAMP;
    return ColumnType::STRING;
}

void apply_top(SchemaSpec& sp, const std::string& key, const std::string& value) {
    if (key == "name")                       sp.name = unquote(value);
    else if (key == "version")               sp.version = unquote(value);
    else if (key == "require_timestamp_column") sp.require_timestamp_column = parse_bool(value);
    else if (key == "timestamp_column_name") sp.timestamp_column_name = unquote(value);
    else if (key == "allow_extra_columns")   sp.allow_extra_columns = parse_bool(value);
}

void apply_col(ColumnSpec& c, const std::string& key, const std::string& value) {
    if (key == "name")            c.name = unquote(value);
    else if (key == "type")       c.type = parse_type(unquote(value));
    else if (key == "required")   c.required = parse_bool(value);
    else if (key == "allow_null") c.allow_null = parse_bool(value);
    else if (key == "min_value")  c.min_value = parse_number(value);
    else if (key == "max_value")  c.max_value = parse_number(value);
}

}  // namespace

SchemaSpec load_schema_from_string(const std::string& content) {
    SchemaSpec sp;
    std::istringstream iss(content);
    std::string raw;
    enum class Section { TOP, COLUMN } sect = Section::TOP;
    while (std::getline(iss, raw)) {
        std::string line = trim(strip_comment(raw));
        if (line.empty()) continue;
        if (line == "[[columns]]") {
            sp.columns.emplace_back();
            sect = Section::COLUMN;
            continue;
        }
        if (!line.empty() && line.front() == '[') {
            sect = Section::TOP;  // unknown section -> ignore
            continue;
        }
        const auto eq = line.find('=');
        if (eq == std::string::npos) continue;
        const std::string key = trim(line.substr(0, eq));
        const std::string value = trim(line.substr(eq + 1));
        if (sect == Section::TOP) apply_top(sp, key, value);
        else if (!sp.columns.empty()) apply_col(sp.columns.back(), key, value);
    }
    if (sp.name.empty()) sp.name = "unnamed";
    if (sp.version.empty()) sp.version = "0.0";
    return sp;
}

SchemaSpec load_schema(const std::string& toml_path) {
    if (toml_path.empty()) throw std::invalid_argument("toml_path is empty");
    std::ifstream f(toml_path);
    if (!f) throw std::runtime_error("cannot open schema file: " + toml_path);
    std::ostringstream ss;
    ss << f.rdbuf();
    return load_schema_from_string(ss.str());
}

}  // namespace valengine

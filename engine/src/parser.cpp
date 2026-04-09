#include "valengine/parser.hpp"
#include "valengine/stats.hpp"
#include <fstream>
#include <sstream>
#include <stdexcept>
#include <unordered_map>
#include <cmath>
#include <cstdlib>
#include <limits>

namespace valengine {

namespace {

constexpr const char* k_utf8_bom = "\xEF\xBB\xBF";

void strip_bom(std::string& s) {
    if (s.size() >= 3 && s.compare(0, 3, k_utf8_bom) == 0) s.erase(0, 3);
}

char detect_delimiter(const std::string& header_line) {
    const char candidates[] = {',', ';', '\t'};
    char best = ',';
    size_t best_count = 0;
    for (char c : candidates) {
        size_t n = 0;
        for (char ch : header_line) if (ch == c) ++n;
        if (n > best_count) { best_count = n; best = c; }
    }
    return best;
}

std::vector<std::string> split_csv_line(const std::string& line, char delim) {
    std::vector<std::string> out;
    std::string field;
    bool in_quotes = false;
    for (size_t i = 0; i < line.size(); ++i) {
        char c = line[i];
        if (in_quotes) {
            if (c == '"') {
                if (i + 1 < line.size() && line[i + 1] == '"') {
                    field += '"';
                    ++i;
                } else {
                    in_quotes = false;
                }
            } else {
                field += c;
            }
        } else {
            if (c == '"') {
                in_quotes = true;
            } else if (c == delim) {
                out.push_back(field);
                field.clear();
            } else {
                field += c;
            }
        }
    }
    out.push_back(field);
    return out;
}

bool is_blank(const std::string& s) {
    for (char c : s) if (c != ' ' && c != '\t' && c != '\r') return false;
    return true;
}

bool try_parse_double(const std::string& s, double& out) {
    if (s.empty()) return false;
    const char* begin = s.c_str();
    char* end = nullptr;
    const double v = std::strtod(begin, &end);
    if (end == begin) return false;
    while (*end == ' ' || *end == '\t' || *end == '\r') ++end;
    if (*end != '\0') return false;
    out = v;
    return true;
}

}  // namespace

ParseResult parse_csv_from_string(const std::string& content) {
    ParseResult result;
    std::string buf = content;
    strip_bom(buf);
    std::istringstream iss(buf);
    std::string line;

    if (!std::getline(iss, line)) {
        result.success = false;
        result.error_message = "empty file";
        return result;
    }
    while (is_blank(line)) {
        if (!std::getline(iss, line)) {
            result.success = false;
            result.error_message = "empty file";
            return result;
        }
    }
    const char delim = detect_delimiter(line);
    result.headers = split_csv_line(line, delim);
    for (auto& h : result.headers) {
        while (!h.empty() && (h.back() == '\r' || h.back() == ' ')) h.pop_back();
    }

    const size_t ncols = result.headers.size();
    std::vector<std::vector<std::string>> raw(ncols);

    while (std::getline(iss, line)) {
        if (is_blank(line)) { ++result.skipped_rows; continue; }
        auto fields = split_csv_line(line, delim);
        if (fields.size() != ncols) { ++result.skipped_rows; continue; }
        ++result.total_rows;
        ++result.valid_rows;
        for (size_t i = 0; i < ncols; ++i) raw[i].push_back(std::move(fields[i]));
    }

    result.column_stats.reserve(ncols);
    for (size_t i = 0; i < ncols; ++i) {
        std::vector<double> nums;
        nums.reserve(raw[i].size());
        int64_t parse_failures = 0;
        for (const auto& s : raw[i]) {
            if (s.empty()) {
                nums.push_back(std::numeric_limits<double>::quiet_NaN());
                continue;
            }
            double v;
            if (try_parse_double(s, v)) nums.push_back(v);
            else { nums.push_back(std::numeric_limits<double>::quiet_NaN()); ++parse_failures; }
        }
        ColumnStats cs = compute_column_stats(result.headers[i], nums);
        // Heuristic: if more than half the non-empty values failed to parse,
        // treat the column as STRING.
        const int64_t non_empty = static_cast<int64_t>(raw[i].size()) - cs.null_count;
        if (non_empty > 0 && parse_failures * 2 > non_empty) {
            cs.type = ColumnType::STRING;
            cs.min_value = cs.max_value = cs.mean = cs.stddev =
                std::numeric_limits<double>::quiet_NaN();
            cs.non_null_count = non_empty;
            cs.null_count = static_cast<int64_t>(raw[i].size()) - non_empty;
        }
        result.column_stats.push_back(std::move(cs));
    }
    result.raw_columns = std::move(raw);
    result.success = true;
    return result;
}

ParseResult parse_csv(const std::string& file_path) {
    if (file_path.empty()) throw std::invalid_argument("file_path is empty");
    std::ifstream f(file_path);
    if (!f) {
        ParseResult r;
        r.success = false;
        r.error_message = "cannot open file: " + file_path;
        return r;
    }
    std::ostringstream ss;
    ss << f.rdbuf();
    return parse_csv_from_string(ss.str());
}

}  // namespace valengine

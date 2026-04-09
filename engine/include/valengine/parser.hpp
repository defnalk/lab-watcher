#pragma once
#include "valengine/types.hpp"
#include <string>

namespace valengine {

/// Parse a CSV file from disk. Auto-detects delimiter (, ; or \t), strips
/// UTF-8 BOM, skips blank lines, handles quoted fields with escaped quotes,
/// and computes per-column statistics for numeric columns.
[[nodiscard]] ParseResult parse_csv(const std::string& file_path);

/// Parse CSV from an in-memory string buffer (for testing without disk I/O).
[[nodiscard]] ParseResult parse_csv_from_string(const std::string& content);

}  // namespace valengine

#pragma once
#include "valengine/types.hpp"
#include <string>

namespace valengine {

/// Load a schema from a TOML file. Throws std::runtime_error on missing or
/// malformed input. Supports a minimal TOML subset sufficient for schema
/// files: top-level key = value pairs, [[columns]] array-of-tables, string,
/// boolean, and float/int scalars, and # comments.
[[nodiscard]] SchemaSpec load_schema(const std::string& toml_path);

/// Load a schema from an in-memory TOML string (for testing).
[[nodiscard]] SchemaSpec load_schema_from_string(const std::string& toml_content);

}  // namespace valengine

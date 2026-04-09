#pragma once
#include "valengine/types.hpp"

namespace valengine {

/// Validate a parsed CSV against a schema. Checks required columns,
/// duplicate headers, type inference, numeric range bounds, nulls in
/// required non-nullable columns, and unexpected columns if disallowed.
[[nodiscard]] ValidationReport validate(const ParseResult& parsed,
                                         const SchemaSpec& schema);

/// Convenience: parse + validate in one call.
[[nodiscard]] ValidationReport parse_and_validate(const std::string& file_path,
                                                    const std::string& schema_path);

}  // namespace valengine

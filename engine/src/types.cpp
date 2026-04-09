#include "valengine/types.hpp"

namespace valengine {

const char* error_code_name(ErrorCode c) {
    switch (c) {
        case ErrorCode::MISSING_REQUIRED_COLUMN: return "MISSING_REQUIRED_COLUMN";
        case ErrorCode::UNEXPECTED_COLUMN:       return "UNEXPECTED_COLUMN";
        case ErrorCode::TYPE_MISMATCH:           return "TYPE_MISMATCH";
        case ErrorCode::VALUE_OUT_OF_RANGE:      return "VALUE_OUT_OF_RANGE";
        case ErrorCode::NULL_IN_REQUIRED:        return "NULL_IN_REQUIRED";
        case ErrorCode::DUPLICATE_HEADER:        return "DUPLICATE_HEADER";
        case ErrorCode::EMPTY_FILE:              return "EMPTY_FILE";
        case ErrorCode::PARSE_FAILURE:           return "PARSE_FAILURE";
    }
    return "UNKNOWN";
}

const char* column_type_name(ColumnType t) {
    switch (t) {
        case ColumnType::STRING:    return "string";
        case ColumnType::INTEGER:   return "integer";
        case ColumnType::FLOAT:     return "float";
        case ColumnType::TIMESTAMP: return "timestamp";
    }
    return "unknown";
}

}  // namespace valengine

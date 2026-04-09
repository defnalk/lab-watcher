#pragma once
#include <string>
#include <sstream>
#include <cmath>

namespace valengine::jsonw {

inline std::string escape(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 2);
    for (char c : s) {
        switch (c) {
            case '"':  out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n";  break;
            case '\r': out += "\\r";  break;
            case '\t': out += "\\t";  break;
            default:
                if (static_cast<unsigned char>(c) < 0x20) {
                    char buf[8];
                    std::snprintf(buf, sizeof(buf), "\\u%04x", c);
                    out += buf;
                } else {
                    out += c;
                }
        }
    }
    return out;
}

inline std::string number(double v) {
    if (std::isnan(v) || std::isinf(v)) return "null";
    std::ostringstream o;
    o.precision(10);
    o << v;
    return o.str();
}

}  // namespace valengine::jsonw

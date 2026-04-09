#include <jni.h>
#include <string>
#include <sstream>
#include <stdexcept>
#include "valengine/parser.hpp"
#include "valengine/schema.hpp"
#include "valengine/validator.hpp"
#include "json_writer.hpp"

using namespace valengine;
using valengine::jsonw::escape;
using valengine::jsonw::number;

namespace {

std::string to_json(const ColumnStats& s) {
    std::ostringstream o;
    o << "{\"name\":\"" << escape(s.name) << "\","
      << "\"type\":\"" << column_type_name(s.type) << "\","
      << "\"non_null_count\":" << s.non_null_count << ","
      << "\"null_count\":" << s.null_count << ","
      << "\"min\":" << number(s.min_value) << ","
      << "\"max\":" << number(s.max_value) << ","
      << "\"mean\":" << number(s.mean) << ","
      << "\"stddev\":" << number(s.stddev) << "}";
    return o.str();
}

std::string to_json(const ParseResult& p) {
    std::ostringstream o;
    o << "{\"success\":" << (p.success ? "true" : "false")
      << ",\"error_message\":\"" << escape(p.error_message) << "\""
      << ",\"total_rows\":" << p.total_rows
      << ",\"valid_rows\":" << p.valid_rows
      << ",\"skipped_rows\":" << p.skipped_rows
      << ",\"headers\":[";
    for (size_t i = 0; i < p.headers.size(); ++i) {
        if (i) o << ",";
        o << "\"" << escape(p.headers[i]) << "\"";
    }
    o << "],\"column_stats\":[";
    for (size_t i = 0; i < p.column_stats.size(); ++i) {
        if (i) o << ",";
        o << to_json(p.column_stats[i]);
    }
    o << "]}";
    return o.str();
}

std::string to_json(const ValidationError& e) {
    std::ostringstream o;
    o << "{\"line\":" << e.line
      << ",\"column\":\"" << escape(e.column) << "\""
      << ",\"code\":\"" << error_code_name(e.code) << "\""
      << ",\"severity\":\"" << (e.severity == Severity::ERROR_ ? "ERROR" : "WARN") << "\""
      << ",\"message\":\"" << escape(e.message) << "\"}";
    return o.str();
}

std::string to_json(const ValidationReport& r) {
    std::ostringstream o;
    o << "{\"passed\":" << (r.passed ? "true" : "false")
      << ",\"error_count\":" << r.error_count
      << ",\"warning_count\":" << r.warning_count
      << ",\"errors\":[";
    for (size_t i = 0; i < r.errors.size(); ++i) {
        if (i) o << ",";
        o << to_json(r.errors[i]);
    }
    o << "],\"parse_result\":" << to_json(r.parse_result) << "}";
    return o.str();
}

std::string jstr_to_std(JNIEnv* env, jstring js) {
    if (!js) return {};
    const char* c = env->GetStringUTFChars(js, nullptr);
    std::string s = c ? c : "";
    if (c) env->ReleaseStringUTFChars(js, c);
    return s;
}

void throw_runtime(JNIEnv* env, const std::string& msg) {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls) env->ThrowNew(cls, msg.c_str());
}

}  // namespace

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_labwatcher_engine_ValEngineJNI_parseCsv(JNIEnv* env, jclass, jstring path) {
    try {
        auto p = parse_csv(jstr_to_std(env, path));
        return env->NewStringUTF(to_json(p).c_str());
    } catch (const std::exception& e) {
        throw_runtime(env, e.what());
        return nullptr;
    }
}

JNIEXPORT jstring JNICALL
Java_com_labwatcher_engine_ValEngineJNI_loadSchema(JNIEnv* env, jclass, jstring path) {
    try {
        auto s = load_schema(jstr_to_std(env, path));
        std::ostringstream o;
        o << "{\"name\":\"" << escape(s.name) << "\","
          << "\"version\":\"" << escape(s.version) << "\","
          << "\"columns\":" << s.columns.size() << "}";
        return env->NewStringUTF(o.str().c_str());
    } catch (const std::exception& e) {
        throw_runtime(env, e.what());
        return nullptr;
    }
}

JNIEXPORT jstring JNICALL
Java_com_labwatcher_engine_ValEngineJNI_parseAndValidate(JNIEnv* env, jclass,
                                                          jstring file, jstring schema) {
    try {
        auto r = parse_and_validate(jstr_to_std(env, file), jstr_to_std(env, schema));
        return env->NewStringUTF(to_json(r).c_str());
    } catch (const std::exception& e) {
        throw_runtime(env, e.what());
        return nullptr;
    }
}

}  // extern "C"

// Property-style fuzz: generate random byte strings biased toward CSV-ish
// shapes and assert parse_csv_from_string never throws and returns a sane
// ParseResult (success or a clean failure with an error message). The goal
// is to catch assertion failures, segfaults, and unhandled exceptions in
// the parser, not to validate semantic correctness.

#include <gtest/gtest.h>
#include "valengine/parser.hpp"
#include <random>
#include <string>

namespace {

std::string random_csvish(std::mt19937& rng, size_t max_len) {
    std::uniform_int_distribution<size_t> len_d(0, max_len);
    std::uniform_int_distribution<int> bias(0, 100);
    const char alphabet[] =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        ".-+e\"\",;\t \n\r";
    const size_t alpha_n = sizeof(alphabet) - 1;
    std::uniform_int_distribution<size_t> ch(0, alpha_n - 1);

    std::string s;
    s.reserve(len_d(rng));
    size_t n = len_d(rng);
    for (size_t i = 0; i < n; ++i) {
        // 5% chance of injecting a high-bit byte to test BOM/UTF-8 handling.
        if (bias(rng) < 5) {
            s.push_back(static_cast<char>(0xC0 | (rng() & 0x1F)));
        } else {
            s.push_back(alphabet[ch(rng)]);
        }
    }
    return s;
}

}  // namespace

TEST(Fuzz, ParserNeverThrows) {
    std::mt19937 rng(0xBADC0FFEE);
    constexpr int kIters = 2000;
    constexpr size_t kMaxLen = 4096;
    for (int i = 0; i < kIters; ++i) {
        std::string input = random_csvish(rng, kMaxLen);
        ASSERT_NO_THROW({
            auto r = valengine::parse_csv_from_string(input);
            // Either success or a clean failure with an error_message.
            if (!r.success) {
                ASSERT_FALSE(r.error_message.empty());
            } else {
                // Headers parsed; row counts non-negative.
                ASSERT_GE(r.total_rows, 0);
                ASSERT_GE(r.skipped_rows, 0);
            }
        }) << "input idx=" << i << " size=" << input.size();
    }
}

TEST(Fuzz, EmptyAndNullByteInputs) {
    EXPECT_NO_THROW(valengine::parse_csv_from_string(""));
    EXPECT_NO_THROW(valengine::parse_csv_from_string(std::string(1, '\0')));
    EXPECT_NO_THROW(valengine::parse_csv_from_string("\xEF\xBB\xBF"));  // BOM only
    EXPECT_NO_THROW(valengine::parse_csv_from_string("a,b,c"));        // header no \n
    EXPECT_NO_THROW(valengine::parse_csv_from_string("\"unterminated"));
}

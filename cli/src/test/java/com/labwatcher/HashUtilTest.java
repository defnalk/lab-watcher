package com.labwatcher;

import com.labwatcher.util.HashUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilTest {
    // sha256("hello\n") — verified with `printf 'hello\n' | shasum -a 256`
    private static final String HELLO_NL_SHA =
        "5891b5b522d5df086d0ff0b110fbd9d21bb4fc7163af34d08286a2e846f6be03";

    @Test void hexEncodes() {
        assertThat(HashUtil.toHex(new byte[]{0x00, 0x0f, (byte) 0xff}))
            .isEqualTo("000fff");
    }

    @Test void sha256OfFile(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("h.txt");
        Files.writeString(f, "hello\n");
        assertThat(HashUtil.sha256(f)).isEqualTo(HELLO_NL_SHA);
    }
}

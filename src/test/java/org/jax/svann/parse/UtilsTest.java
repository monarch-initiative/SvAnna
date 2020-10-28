package org.jax.svann.parse;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

public class UtilsTest {

    private static final Charset ASCII = StandardCharsets.US_ASCII;

    @Test
    public void reverseComplement() {
        byte[] seq = "ACGTUacgtuWSMKwsmkRYryBDHVNbdhvn".getBytes(ASCII);
        byte[] reversed = Utils.reverseComplement(seq);
        String rev = ASCII.decode(ByteBuffer.wrap(reversed)).toString();

        assertThat(rev, is("nbdhvNBDHVryRYmkswMKSWaacgtAACGT"));
    }
}
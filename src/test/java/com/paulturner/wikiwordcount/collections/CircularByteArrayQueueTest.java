package com.paulturner.wikiwordcount.collections;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CircularByteArrayQueueTest {
    private CircularByteArrayQueue queue;

    @Before
    public void before() throws Exception {
        queue = new CircularByteArrayQueue(80);
    }

    @Test
    public void shouldIndicateDoesNotContainWhenEmpty() throws Exception {

        assertThat(queue.contains("hello, is it me you're looking for?".getBytes(StandardCharsets.US_ASCII))).isFalse();
    }

    @Test
    public void shouldIndicateContainsWhenContains() throws Exception {
        IntStream.rangeClosed(48, 57).forEach(
                i -> queue.offer((byte) i)
        );

        assertThat(queue.contains("0123456789".getBytes(StandardCharsets.US_ASCII))).isTrue();
    }

    @Test
    public void shouldIndicateContainsWhenContainsAfterCircularOverflow() throws Exception {
        for (byte b : " disorders]]</text><sha1>tswevljrxdlsg8zl3bs205kb6sec6jc</sha1> </revision></page".getBytes(StandardCharsets.US_ASCII)) {
            queue.offer(b);
        }

        assertThat(queue.contains("</page".getBytes(StandardCharsets.US_ASCII))).isTrue();
    }

}

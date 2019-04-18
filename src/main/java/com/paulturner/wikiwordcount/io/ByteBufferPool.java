package com.paulturner.wikiwordcount.io;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class ByteBufferPool {

    private static final Logger logger = LoggerFactory.getLogger(ByteBufferPool.class);

    private final Deque<ByteBuffer> bufferDeque = new ConcurrentLinkedDeque<>();
    private final AtomicInteger allocated = new AtomicInteger();

    private final Semaphore semaphore;

    private final int capacity;
    private final boolean direct;

    public ByteBufferPool(int inCapacity, boolean direct) {
        this.capacity = inCapacity;
        this.direct = direct;
        semaphore = new Semaphore(inCapacity);
    }

    public ByteBuffer acquire() {


        try {
            semaphore.acquire();
        } catch (InterruptedException ie) {
            logger.warn("Interrupted while waiting to get a byteBuffer from pool", ie);
        }
        ByteBuffer byteBuffer = bufferDeque.poll();
        if (byteBuffer == null) {
            byteBuffer = direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
            allocated.incrementAndGet();

            logger.debug("Created new ByteBuffer in pool: [byteBuffer:{}]", byteBuffer);

        }


        logger.debug("Acquired ByteBuffer from pool [byteBuffer identityHashCode={}]", System.identityHashCode(byteBuffer));

        return byteBuffer;

    }

    public void release(ByteBuffer byteBuffer) {

        byteBuffer.clear();
        bufferDeque.offer(byteBuffer);
        semaphore.release();
        logger.debug("Returned ByteBuffer to pool [byteBuffer identityHashCode={}]", System.identityHashCode(byteBuffer));
    }

    public int getAllocated() {
        return allocated.get();
    }

    public int getCapacity() {
        return capacity;
    }

}

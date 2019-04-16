package com.paulturner.wikiwordcount.io;


import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class ByteBufferPool {

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

    public ByteBuffer acquire() throws Exception {

        semaphore.acquire();
        ByteBuffer byteBuffer = bufferDeque.poll();
        if (byteBuffer == null) {
            byteBuffer = direct ? ByteBuffer.allocateDirect(100) : ByteBuffer.allocate(100);
            allocated.incrementAndGet();
        }
        return byteBuffer;

    }

    public void release(ByteBuffer byteBuffer) {
        byteBuffer.clear();
        bufferDeque.offer(byteBuffer);
        semaphore.release();
    }

    public int getAllocated() {
        return allocated.get();
    }

    public int getCapacity() {
        return capacity;
    }

}

package org.cobbzilla.wizardtest.mq;

import lombok.Getter;
import org.cobbzilla.util.mq.MqClient;
import org.cobbzilla.util.mq.MqConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

public class DummyMqConsumer implements MqConsumer {

    private static final long DEFAULT_TIMEOUT = 5000;

    @Getter private final List<Object> messages = new ArrayList<>();
    @Getter private final AtomicInteger messageCount = new AtomicInteger(0);

    public DummyMqConsumer(MqClient mqClient, String queueName) throws Exception {
        mqClient.flushAllQueues(); // start with empty queues
        flush();
        mqClient.registerConsumer(this, queueName, queueName + "_error");
    }

    @Override
    public void onMessage(Object message) throws Exception {
        synchronized (messages) {
            messages.add(message);
            messageCount.incrementAndGet();
        }
    }

    public void flush() {
        synchronized (messages) {
            messages.clear();
            messageCount.set(0);
        }
    }

    public boolean waitForMessage () throws InterruptedException { return waitForMessage(DEFAULT_TIMEOUT); }
    public boolean waitForMessage (long timeout) throws InterruptedException { return waitForMessages(1, timeout); }

    public boolean waitForMessages (int count) throws InterruptedException { return waitForMessages(count, DEFAULT_TIMEOUT); }

    public boolean waitForMessages (int count, long timeout) throws InterruptedException {
        long start = now();
        while (messageCount.get() < count && now() < start + timeout) {
            Thread.sleep(100);
        }
        return messageCount.get() >= count;
    }

}

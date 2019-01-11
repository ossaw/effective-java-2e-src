// Simple framework for timing concurrent execution
package org.effectivejava.examples.chapter10.item69;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * 正常情况下引入ready和done两个CountDownLatch就可以计算时间, 之所以引入start可以更加精确的
 * 计算程序执行时间, ready和done两个初始化值为concurrency并发数, 它们在countDown为零之后会
 * 依次唤醒线程, 这个线程唤醒过程存在延迟, 而引入一个初始值为1的start变量则可以消除这一点
 */
public class ConcurrentTimer {
    private ConcurrentTimer() {}

    private static long time(ExecutorService executor, int concurrency,
                             final Runnable action) throws InterruptedException {
        final CountDownLatch ready = new CountDownLatch(concurrency);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(concurrency);

        for (int i = 0; i < concurrency; i++)
            executor.execute(() -> {
                ready.countDown(); // Tell timer we're ready
                try {
                    start.await(); // Wait till peers are ready
                    action.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown(); // Tell timer we're done
                }
            });

        ready.await(); // Wait for all workers to be ready
        long startNanos = System.nanoTime();
        start.countDown(); // And they're off!
        done.await(); // Wait for all workers to finish
        return System.nanoTime() - startNanos;
    }

    public static void main(String[] args) throws InterruptedException {
        final ExecutorService exec = Executors.newCachedThreadPool();
        final long time = time(exec, 10, () -> {
            try {
                MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        System.out.println("cost time: " + time);
        exec.shutdown();
    }
}

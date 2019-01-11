// Simple framework for timing concurrent execution
package org.effectivejava.examples.chapter10.item69;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ConcurrentTimer {
    private ConcurrentTimer() {
    }

    /**
     * 1. 正常情况下引入ready和done两个CountDownLatch就可以计算时间, 之所以引入start可以更加精
     * 确的计算程序执行时间, ready和done两个初始化值为concurrency并发数, 它们在countDown为零之
     * 后会依次唤醒线程, 这个线程唤醒过程存在延迟, 而引入一个初始值为1的start变量则可以消除这一点
     * <p>
     * 2. 运用此方法需要注意time传递的参数是一个Runnable, 也就是说多线程共享一个Runnable, 在run
     * 方法内尽量不要涉及实例变量, 否则会发生数据竞争, 如果避免不了, 那么请使用同步或者修改time方法
     * 为多个Runnable实例
     * <p>
     * 3. 其他注意事项请参见effective java第69条目
     */
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

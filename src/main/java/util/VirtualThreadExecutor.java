package util;

import file.Config;

import java.util.List;
import java.util.concurrent.*;

public class VirtualThreadExecutor extends AbstractExecutorService {

    private volatile boolean shutdown;
    private final ExecutorService delegate = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore permits;

    public VirtualThreadExecutor(int maxConcurrency) {
        this.permits = new Semaphore(maxConcurrency);
    }

    public static VirtualThreadExecutor create() {
        return new VirtualThreadExecutor(Config.getInstance().getConcurrency());
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(() -> {
            boolean acquired = false;
            try {
                permits.acquire();
                acquired = true;
                command.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (acquired) {
                    permits.release();
                }
            }
        });
    }

    @Override
    public void shutdown() {
        shutdown = true;
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
}

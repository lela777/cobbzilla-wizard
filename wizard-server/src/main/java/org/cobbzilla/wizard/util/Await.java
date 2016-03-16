package org.cobbzilla.wizard.util;

import org.springframework.util.CompositeIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.system.Sleep.sleep;

public class Await {

    public static final long DEFAULT_AWAIT_GET_SLEEP = 10;
    public static final long DEFAULT_AWAIT_RETRY_SLEEP = 100;

    public static <E> E await(Future<E> future, long timeout) { return await(future, timeout, DEFAULT_AWAIT_RETRY_SLEEP); }
    public static <E> E await(Future<E> future, long timeout, long retrySleep) { return await(future, timeout, retrySleep, DEFAULT_AWAIT_GET_SLEEP); }

    public static <E> E await(Future<E> future, long timeout, long retrySleep, long getSleep) {
        long start = now();
        while (now() - start < timeout) {
            try {
                return future.get(getSleep, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                die("await: interrupted: "+e);
            } catch (ExecutionException e) {
                die("await: execution error: "+e);
            } catch (TimeoutException e) {
                // noop
            }
        }
        return die("await: timed out");
    }

    public static <E> E awaitFirst(Collection<Future<E>> futures, long timeout) { return awaitFirst(futures, timeout, DEFAULT_AWAIT_RETRY_SLEEP); }
    public static <E> E awaitFirst(Collection<Future<E>> futures, long timeout, long retrySleep) { return awaitFirst(futures, timeout, retrySleep, DEFAULT_AWAIT_GET_SLEEP); }

    public static <E> E awaitFirst(Collection<Future<E>> futures, long timeout, long retrySleep, long getSleep) {
        long start = now();
        while (now() - start < timeout) {
            for (Iterator<Future<E>> iter = futures.iterator(); iter.hasNext(); ) {
                Future<E> future = iter.next();
                try {
                    final E value = future.get(getSleep, TimeUnit.MILLISECONDS);
                    if (value != null) return value;
                    iter.remove();

                } catch (InterruptedException e) {
                    die("await: interrupted: " + e);
                } catch (ExecutionException e) {
                    die("await: execution error: " + e);
                } catch (TimeoutException e) {
                    // noop
                }
                sleep(retrySleep);
            }
        }
        if (now() - start < timeout) return die("await: timed out");
        return null; // all futures had a null result
    }

    public static <E> List<E> awaitAndCollect(Collection<Future<List<E>>> futures, int maxResults, long timeout) {
        return awaitAndCollect(futures, maxResults, timeout, DEFAULT_AWAIT_RETRY_SLEEP);
    }

    public static <E> List<E> awaitAndCollect(Collection<Future<List<E>>> futures, int maxResults, long timeout, long retrySleep) {
        return awaitAndCollect(futures, maxResults, timeout, retrySleep, DEFAULT_AWAIT_GET_SLEEP);
    }

    public static <E> List<E> awaitAndCollect(Collection<Future<List<E>>> futures, int maxResults, long timeout, long retrySleep, long getSleep) {
        final List<E> results = new ArrayList<>();
        long start = now();
        int size = futures.size();
        while (now() - start < timeout) {
            for (Iterator<Future<List<E>>> iter = futures.iterator(); iter.hasNext(); ) {
                Future future = iter.next();
                try {
                    results.addAll((List<E>) future.get(getSleep, TimeUnit.MILLISECONDS));
                    iter.remove();
                    if (--size <= 0 || results.size() >= maxResults) return results;

                } catch (InterruptedException e) {
                    die("await: interrupted: " + e);
                } catch (ExecutionException e) {
                    die("await: execution error: " + e);
                } catch (TimeoutException e) {
                    // noop
                }
                sleep(retrySleep);
            }
        }
        if (now() - start < timeout) die("await: timed out");
        return results;
    }

    public static <E> CompositeIterator<E> awaitAndCollect(Collection<Future<Iterator<E>>> futures, long timeout) {
        return awaitAndCollect(futures, timeout, DEFAULT_AWAIT_RETRY_SLEEP);
    }

    public static <E> CompositeIterator<E> awaitAndCollect(Collection<Future<Iterator<E>>> futures, long timeout, long retrySleep) {
        return awaitAndCollect(futures, timeout, retrySleep, DEFAULT_AWAIT_GET_SLEEP);
    }

    public static <E> CompositeIterator<E> awaitAndCollect(Collection<Future<Iterator<E>>> futures, long timeout, long retrySleep, long getSleep) {
        final CompositeIterator<E> results = new CompositeIterator<>();
        long start = now();
        int size = futures.size();
        while (now() - start < timeout) {
            for (Iterator<Future<Iterator<E>>> iter = futures.iterator(); iter.hasNext(); ) {
                Future future = iter.next();
                try {
                    results.add((Iterator<E>) future.get(getSleep, TimeUnit.MILLISECONDS));
                    iter.remove();
                    if (--size <= 0) return results;

                } catch (InterruptedException e) {
                    die("await: interrupted: " + e);
                } catch (ExecutionException e) {
                    die("await: execution error: " + e);
                } catch (TimeoutException e) {
                    // noop
                }
                sleep(retrySleep);
            }
        }
        if (now() - start < timeout) die("await: timed out");
        return results;
    }

    public static void awaitAll(Collection<Future> futures, long timeout) { awaitAll(futures, timeout, DEFAULT_AWAIT_RETRY_SLEEP); }
    public static void awaitAll(Collection<Future> futures, long timeout, long retrySleep) { awaitAll(futures, timeout, retrySleep, DEFAULT_AWAIT_GET_SLEEP); }

    public static void awaitAll(Collection<Future> futures, long timeout, long retrySleep, long getSleep) {
        long start = now();
        int size = futures.size();
        while (now() - start < timeout) {
            for (Iterator<Future> iter = futures.iterator(); iter.hasNext(); ) {
                final Future future = iter.next();
                try {
                    future.get(getSleep, TimeUnit.MILLISECONDS);
                    iter.remove();
                    if (--size <= 0) return;

                } catch (InterruptedException e) {
                    die("await: interrupted: " + e);
                } catch (ExecutionException e) {
                    die("await: execution error: " + e);
                } catch (TimeoutException e) {
                    // noop
                }

                sleep(retrySleep);
            }
        }
        if (futures.isEmpty()) return;
        if (now() - start < timeout) die("await: timed out");
    }

}

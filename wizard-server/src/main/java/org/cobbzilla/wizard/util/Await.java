package org.cobbzilla.wizard.util;

import java.util.*;
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

    public static <E> E awaitFirst(Collection<Future<E>> futures, long timeout) throws TimeoutException {
        return awaitFirst(futures, timeout, DEFAULT_AWAIT_RETRY_SLEEP);
    }
    public static <E> E awaitFirst(Collection<Future<E>> futures, long timeout, long retrySleep) throws TimeoutException {
        return awaitFirst(futures, timeout, retrySleep, DEFAULT_AWAIT_GET_SLEEP);
    }

    public static <E> E awaitFirst(Collection<Future<E>> futures, long timeout, long retrySleep, long getSleep) throws TimeoutException {
        long start = now();
        while (!futures.isEmpty() && now() - start < timeout) {
            for (Iterator<Future<E>> iter = futures.iterator(); iter.hasNext(); ) {
                Future<E> future = iter.next();
                try {
                    final E value = future.get(getSleep, TimeUnit.MILLISECONDS);
                    if (value != null) return value;
                    iter.remove();
                    if (futures.isEmpty()) break;

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
        if (now() - start > timeout) throw new TimeoutException("await: timed out");
        return null; // all futures had a null result
    }

    public static <E> List<E> awaitAndCollect(Collection<Future<List<E>>> futures, int maxResults, long timeout) throws TimeoutException {
        return awaitAndCollect(futures, maxResults, timeout, DEFAULT_AWAIT_RETRY_SLEEP);
    }

    public static <E> List<E> awaitAndCollect(Collection<Future<List<E>>> futures, int maxResults, long timeout, long retrySleep) throws TimeoutException {
        return awaitAndCollect(futures, maxResults, timeout, retrySleep, DEFAULT_AWAIT_GET_SLEEP);
    }

    public static <E> List<E> awaitAndCollect(Collection<Future<List<E>>> futures, int maxResults, long timeout, long retrySleep, long getSleep) throws TimeoutException {
        return awaitAndCollect(futures, maxResults, timeout, retrySleep, getSleep, new ArrayList<E>());
    }

    public static <E> List<E> awaitAndCollect(List<Future<List<E>>> futures, int maxQueryResults, long timeout, List results) throws TimeoutException {
        return awaitAndCollect(futures, maxQueryResults, timeout, DEFAULT_AWAIT_RETRY_SLEEP, DEFAULT_AWAIT_GET_SLEEP, results);
    }

    public static <E> List<E> awaitAndCollect(Collection<Future<List<E>>> futures, int maxResults, long timeout, long retrySleep, long getSleep, List<E> results) throws TimeoutException {
        long start = now();
        int size = futures.size();
        while (!futures.isEmpty() && now() - start < timeout) {
            for (Iterator<Future<List<E>>> iter = futures.iterator(); iter.hasNext(); ) {
                Future future = iter.next();
                try {
                    results.addAll((List<E>) future.get(getSleep, TimeUnit.MILLISECONDS));
                    iter.remove();
                    if (--size <= 0 || results.size() >= maxResults) return results;
                    break;

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
        if (now() - start > timeout) throw new TimeoutException("await: timed out");
        return results;
    }

    public static <E> Set<E> awaitAndCollectSet(Collection<Future<List<E>>> futures, int maxResults, long timeout) throws TimeoutException {
        return awaitAndCollectSet(futures, maxResults, timeout, DEFAULT_AWAIT_RETRY_SLEEP);
    }

    public static <E> Set<E> awaitAndCollectSet(Collection<Future<List<E>>> futures, int maxResults, long timeout, long retrySleep) throws TimeoutException {
        return awaitAndCollectSet(futures, maxResults, timeout, retrySleep, DEFAULT_AWAIT_GET_SLEEP);
    }

    public static <E> Set<E> awaitAndCollectSet(Collection<Future<List<E>>> futures, int maxResults, long timeout, long retrySleep, long getSleep) throws TimeoutException {
        return awaitAndCollectSet(futures, maxResults, timeout, retrySleep, getSleep, new HashSet<E>());
    }

    public static <E> Set<E> awaitAndCollectSet(List<Future<List<E>>> futures, int maxQueryResults, long timeout, Set<E> results) throws TimeoutException {
        return awaitAndCollectSet(futures, maxQueryResults, timeout, DEFAULT_AWAIT_RETRY_SLEEP, DEFAULT_AWAIT_GET_SLEEP, results);
    }

    public static <E> Set<E> awaitAndCollectSet(Collection<Future<List<E>>> futures, int maxResults, long timeout, long retrySleep, long getSleep, Set<E> results) throws TimeoutException {
        long start = now();
        int size = futures.size();
        while (!futures.isEmpty() && now() - start < timeout) {
            for (Iterator<Future<List<E>>> iter = futures.iterator(); iter.hasNext(); ) {
                Future future = iter.next();
                try {
                    results.addAll((Collection<E>) future.get(getSleep, TimeUnit.MILLISECONDS));
                    iter.remove();
                    if (--size <= 0 || results.size() >= maxResults) return results;
                    break;

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
        if (now() - start > timeout) throw new TimeoutException("await: timed out");
        return results;
    }
}

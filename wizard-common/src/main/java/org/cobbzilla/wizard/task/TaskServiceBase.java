package org.cobbzilla.wizard.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskServiceBase<R extends TaskResult> {

    protected final ExecutorService executor = Executors.newFixedThreadPool(5);
    protected final Map<String, ITask<R>> taskMap = new ConcurrentHashMap<>();

    public TaskId execute(ITask<R> task) {
        task.init();
        executor.submit(task);
        taskMap.put(task.getTaskId().getUuid(), task);
        return task.getTaskId();
    }

    public R getResult(String uuid) {
        ITask<R> task = taskMap.get(uuid);
        return task == null ? null : task.getResult();
    }
}

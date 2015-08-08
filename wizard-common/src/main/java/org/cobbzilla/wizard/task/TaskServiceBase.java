package org.cobbzilla.wizard.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskServiceBase<T extends TaskResult> {

    protected final ExecutorService executor = Executors.newFixedThreadPool(5);
    protected final Map<String, TaskBase> taskMap = new ConcurrentHashMap<>();

    public TaskId execute(TaskBase task) {
        task.init();
        executor.submit(task);
        taskMap.put(task.getTaskId().getUuid(), task);
        return task.getTaskId();
    }

    public T getResult(String uuid) {
        TaskBase<T> task = taskMap.get(uuid);
        return task == null ? null : task.getResult();
    }
}

package com.licel.stringer;

import org.apache.tools.ant.Task;

class AntTaskWorkerListener implements WorkerListener {
    private final AntTask antTask;
    private final Task task;

    AntTaskWorkerListener(AntTask antTask, Task task) {
        this.antTask = antTask;
        this.task = task;
    }

    @Override
    public void info(String message) {
        this.task.log(message);
    }

    @Override
    public void verbose(String message) {
        this.task.log(message);
    }
}

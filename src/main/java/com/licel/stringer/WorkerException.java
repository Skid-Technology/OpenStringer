package com.licel.stringer;

public class WorkerException extends RuntimeException {
    private final Worker worker;

    public WorkerException(Worker worker, String message) {
        super(message);
        this.worker = worker;
    }

    public Worker getWorker() {
        return this.worker;
    }
}

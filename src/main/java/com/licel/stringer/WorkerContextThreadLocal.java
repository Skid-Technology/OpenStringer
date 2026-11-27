package com.licel.stringer;

final class WorkerContextThreadLocal extends ThreadLocal<WorkerContext> {
    @Override
    protected WorkerContext initialValue() {
        return new WorkerContext();
    }
}

package com.licel.stringer;

class ConsoleWorkerListener implements WorkerListener {
    private final Config config;

    ConsoleWorkerListener(Config config) {
        this.config = config;
    }

    @Override
    public void info(String message) {
        System.out.println(message);
    }

    @Override
    public void verbose(String message) {
        System.out.println(message);
    }
}

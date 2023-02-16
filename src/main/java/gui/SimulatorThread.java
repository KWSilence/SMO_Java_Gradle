package gui;

import smo_system.simulator.Simulator;

class SimulatorThread {
    private final Simulator simulator;
    private Thread thread = null;

    public SimulatorThread(Simulator simulator, Thread thread) {
        this.simulator = simulator;
        this.thread = thread;
    }

    public SimulatorThread(Simulator simulator, boolean useFullSimulation) {
        this.simulator = simulator;
        if (useFullSimulation) {
            this.thread = new Thread() {
                @Override
                public void run() {
                    do {
                        simulator.simulationStep();
                    } while (!isInterrupted() && simulator.canContinue());
                }
            };
        }
    }

    public void start() {
        if (thread != null) thread.start();
    }

    public void join() throws InterruptedException {
        if (thread != null) thread.join();
    }

    public void interrupt() {
        if (thread != null) thread.interrupt();
    }

    public boolean isInterrupted() {
        if (thread != null) return thread.isInterrupted();
        return true;
    }

    public boolean isAlive() {
        if (thread != null) return thread.isAlive();
        return false;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public Simulator getSimulator() {
        return simulator;
    }
}

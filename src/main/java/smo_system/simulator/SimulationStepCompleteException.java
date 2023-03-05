package smo_system.simulator;

class SimulationStepCompleteException extends Exception {
    private final boolean state;

    public SimulationStepCompleteException(boolean state) {
        super("Simulation step completed " + (state ? "[continue]" : "[end]"));
        this.state = state;
    }

    public boolean getState() {
        return state;
    }
}

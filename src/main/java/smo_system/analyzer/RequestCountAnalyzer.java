package smo_system.analyzer;

import configs.SimulationConfig;
import smo_system.simulator.Simulator;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestCountAnalyzer {
    private final boolean debug;
    private int n0;
    private Simulator lastSimulator = null;
    private static final Logger LOGGER = Logger.getLogger(RequestCountAnalyzer.class.getName());

    public RequestCountAnalyzer(int n0, boolean debug) {
        this.n0 = n0;
        this.debug = debug;
    }

    public Simulator getLastSimulator() {
        return lastSimulator;
    }

    public void analyze(SimulationConfig config) {
        final double Ta = 1.643;
        final double d = 0.1;
        double lastP = -10;
        int lastN = 0;
        while (true) {
            if (lastP > 0) {
                n0 = (int) Math.round(Ta * Ta * (1 - lastP) / (lastP * d * d));
            }

            Simulator sim = new Simulator(config.createSources(), config.createBuffer(), config.createProcessors(), n0);
            sim.fullSimulation();

            double p1 = (double) sim.getProductionManager().getFullRejectCount() / n0;
            if (debug) {
                String message = (lastN == 0 ? "\n" : "N0=" + lastN + " p0=" + lastP +
                        " ") + "N1=" + n0 + " p1=" + p1 + "  [abs=" +
                        Math.abs(lastP - p1) + ", dp0=" + (0.1 * lastP) + "]";
                LOGGER.log(Level.INFO, message);
            }

            if (lastP != -1 && Math.abs(lastP - p1) < 0.1 * lastP) {
                break;
            }
            lastSimulator = sim;
            if (p1 == 0 || p1 == 1) {
                break;
            }
            lastP = p1;
            lastN = n0;
        }
    }
}

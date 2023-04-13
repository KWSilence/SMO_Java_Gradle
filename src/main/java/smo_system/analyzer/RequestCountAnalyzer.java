package smo_system.analyzer;

import configs.SimulationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smo_system.simulator.Simulator;

public class RequestCountAnalyzer {
    private final int n0;
    private Simulator lastSimulator = null;
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestCountAnalyzer.class);

    public RequestCountAnalyzer(int n0) {
        this.n0 = n0;
    }

    public Simulator getLastSimulator() {
        return lastSimulator;
    }

    public void analyze(SimulationConfig config) {
        final double Ta = 1.643;
        final double d = 0.1;
        double lastP = -10;
        int lastN = 0;
        int n1 = n0;
        while (true) {
            if (lastP > 0) {
                n1 = (int) Math.round(Ta * Ta * (1 - lastP) / (lastP * d * d));
            }

            Simulator sim = new Simulator(config.createSources(), config.createBuffer(), config.createProcessors(), n1);
            sim.fullSimulation();

            double p1 = (double) sim.getProductionManager().getFullRejectCount() / n1;
            if (LOGGER.isDebugEnabled()) {
                String message = (lastN == 0 ? "" : "N0=" + lastN + " p0=" + lastP +
                        " ") + "N1=" + n1 + " p1=" + p1 + "  [abs=" +
                        Math.abs(lastP - p1) + ", dp0=" + (0.1 * lastP) + "]";
                LOGGER.info(message);
            }

            if (lastP != -1 && Math.abs(lastP - p1) < 0.1 * lastP) {
                break;
            }
            lastSimulator = sim;
            if (p1 == 0 || p1 == 1) {
                break;
            }
            lastP = p1;
            lastN = n1;
        }
        LOGGER.info("Optimal count set {} -> {} with prob {}", n0, lastN, lastP);
    }
}

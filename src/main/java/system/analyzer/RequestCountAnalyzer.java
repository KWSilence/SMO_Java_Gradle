package system.analyzer;

import configs.SimulationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import system.simulator.Simulator;

public class RequestCountAnalyzer extends Thread {
    private final int n0;
    private final SimulationConfig config;
    private Simulator lastSimulator = null;
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestCountAnalyzer.class);

    public RequestCountAnalyzer(int n0, SimulationConfig config) {
        this.n0 = n0;
        this.config = config;
    }

    public Simulator getLastSimulator() {
        return lastSimulator;
    }

    @Override
    public void run() {
        analyze();
    }

    public void analyze() {
        final double Ta = 1.643;
        final double d = 0.1;
        double lastP = -10;
        int lastN = 0;
        int n1 = n0;
        while (true) {
            if (isInterrupted()) return;
            if (lastP > 0) {
                n1 = (int) Math.round(Ta * Ta * (1 - lastP) / (lastP * d * d));
            }

            Simulator simulator = new Simulator(config.createSources(), config.createBuffer(), config.createProcessors(), n1);
            do {
                simulator.simulationStep();
            } while (!isInterrupted() && simulator.canContinue());
            if (isInterrupted()) return;

            double p1 = (double) simulator.getProductionManager().getFullRejectCount() / n1;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(debugMessage(lastN, lastP, n1, p1));
            }

            if (checkBreakConditions(simulator, lastP, p1)) break;
            lastP = p1;
            lastN = n1;
        }
        LOGGER.info("Optimal count set {} -> {} with prob {}", n0, lastN, lastP);
    }

    private boolean checkBreakConditions(Simulator simulator, double lastP, double p1) {
        if (lastP != -1 && Math.abs(lastP - p1) < 0.1 * lastP) return true;
        lastSimulator = simulator;
        return p1 == 0 || p1 == 1;
    }

    private String debugMessage(int lastN, double lastP, int n1, double p1) {
        return (lastN == 0 ? "" : "N0=" + lastN + " p0=" + lastP +
                " ") + "N1=" + n1 + " p1=" + p1 + "  [abs=" +
                Math.abs(lastP - p1) + ", dp0=" + (0.1 * lastP) + "]";
    }
}

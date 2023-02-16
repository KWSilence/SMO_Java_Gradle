package smo_system.analyzer;

import configs.SimulationConfig;
import smo_system.simulator.Simulator;

public class RequestCountAnalyzer {
    private final boolean debug;
    private int N0;
    private Simulator lastSimulator = null;

    public RequestCountAnalyzer(int N0, boolean debug) {
        this.N0 = N0;
        this.debug = debug;
    }

    public Simulator getLastSimulator() {
        return lastSimulator;
    }

    public void analyze() {
        SimulationConfig defaultConfig = debug ? new SimulationConfig("src/main/resources/config.json")
                : new SimulationConfig("config.json");
        final double Ta = 1.643;
        final double d = 0.1;
        double lastP = -10;
        int lastN = 0;
        while (true) {
            if (lastP > 0) {
                N0 = (int) Math.round(Ta * Ta * (1 - lastP) / (lastP * d * d));
            }

            SimulationConfig conf = new SimulationConfig(defaultConfig.getConfig());
            Simulator sim = new Simulator(conf.getSources(), conf.getBuffer(), conf.getProcessors(), N0);
            sim.fullSimulation();

            double p1 = (double) sim.getProductionManager().getFullRejectCount() / (double) N0;

            if (debug) {
                System.out.println(
                        (lastN == 0 ? "\n" : "N0=" + lastN + " p0=" + lastP + " ") + "N1=" + N0 + " p1=" + p1 + "  [abs=" +
                                Math.abs(lastP - p1) + ", dp0=" + (0.1 * lastP) + "]");
            }

            if (p1 == 0 || p1 == 1) {
                lastSimulator = sim;
                break;
            }

            if (lastP != -1 && Math.abs(lastP - p1) < 0.1 * lastP) {
                break;
            }
            lastSimulator = sim;
            lastP = p1;
            lastN = N0;
        }
    }
}

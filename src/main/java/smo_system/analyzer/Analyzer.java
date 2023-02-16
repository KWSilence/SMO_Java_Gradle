package smo_system.analyzer;

import smo_system.component.Processor;
import smo_system.component.Request;
import smo_system.component.Source;
import smo_system.manager.ProductionManager;
import smo_system.manager.SelectionManager;
import smo_system.simulator.Simulator;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class Analyzer {
    private final Simulator simulator;
    private final ArrayList<AnalyzerResults> sourcesResult;
    private final ArrayList<AnalyzerResults> processorsResult;

    private final NumberFormat formatter = new DecimalFormat("#0.000");

    public Analyzer(Simulator simulator) {
        this.simulator = simulator;
        this.sourcesResult = new ArrayList<>();
        this.processorsResult = new ArrayList<>();
    }

    public void analyze(boolean simulated) {
        if (!simulated) {
            simulator.fullSimulation();
        }
        analyzeSources();
        analyzeProcessors();
    }

    public ArrayList<ArrayList<String>> getSourceResults() {
        ArrayList<ArrayList<String>> ar = new ArrayList<>();
        for (AnalyzerResults r : sourcesResult) {
            ArrayList<String> s = new ArrayList<>();
            s.add(String.valueOf(r.number));
            s.add(String.valueOf(r.requestCount));
            s.add(formatter.format(r.rejectProbability));
            s.add(formatter.format(r.lifeTime));
            s.add(formatter.format(r.bufferTime));
            s.add(formatter.format(r.processTime));
            s.add(formatter.format(r.bufferTimeDispersion));
            s.add(formatter.format(r.processTimeDispersion));
            ar.add(s);
        }
        return ar;
    }

    public ArrayList<ArrayList<String>> getProcessorResults() {
        ArrayList<ArrayList<String>> ar = new ArrayList<>();
        for (AnalyzerResults r : processorsResult) {
            ArrayList<String> s = new ArrayList<>();
            s.add(String.valueOf(r.number));
            s.add(formatter.format(r.usageRate));
            ar.add(s);
        }
        return ar;
    }

    private void analyzeSources() {
        ProductionManager pm = simulator.getProductionManager();
        ArrayList<Source> sources = pm.getSources();
        ArrayList<ArrayList<Request>> rejected = pm.getRejectedRequests();
        SelectionManager sm = simulator.getSelectionManager();
        ArrayList<ArrayList<Request>> success = sm.getSuccessRequests();
        for (Source s : sources) {
            AnalyzerResults r = new AnalyzerResults(true);
            r.number = s.getNumber();
            r.requestCount = s.getRequestCount();
            r.rejectProbability = ((double) rejected.get(r.number).size()) / ((double) r.requestCount);
            r.lifeTime = success.get(r.number).stream().mapToDouble(Request::getLifeTime).sum() / r.requestCount;
            r.bufferTime = success.get(r.number).stream().mapToDouble(Request::getTimeInBuffer).sum() / r.requestCount;
            r.processTime = success.get(r.number).stream().mapToDouble(Request::getTimeInProcessor).sum() / r.requestCount;
            r.bufferTimeDispersion = success.get(r.number).stream().mapToDouble(a -> Math.pow((a.getTimeInBuffer() - r.bufferTime), 2) / (r.requestCount - 1)).sum();
            r.processTimeDispersion = success.get(r.number).stream().mapToDouble(a -> Math.pow((a.getTimeInProcessor() - r.processTime), 2) / (r.requestCount - 1)).sum();
            sourcesResult.add(r);
        }
    }

    private void analyzeProcessors() {
        SelectionManager sm = simulator.getSelectionManager();
        ArrayList<Processor> processors = sm.getProcessors();
        double endTime = simulator.getEndTime();

        for (Processor p : processors) {
            AnalyzerResults r = new AnalyzerResults(false);
            r.number = p.getNumber();
            r.usageRate = p.getWorkTime() / endTime;
            processorsResult.add(r);
        }
    }
}

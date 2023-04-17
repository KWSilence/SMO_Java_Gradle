package system.analyzer;

import system.component.Processor;
import system.component.Request;
import system.component.Source;
import system.manager.ProductionManager;
import system.manager.SelectionManager;
import system.simulator.Simulator;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

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

    public List<List<String>> getSourceResults() {
        List<List<String>> sourceResultsTable = new ArrayList<>();
        for (AnalyzerResults result : sourcesResult) {
            List<String> sourceResultsRow = new ArrayList<>();
            sourceResultsRow.add(String.valueOf(result.number));
            sourceResultsRow.add(String.valueOf(result.requestCount));
            sourceResultsRow.add(formatter.format(result.rejectProbability));
            sourceResultsRow.add(formatter.format(result.lifeTime));
            sourceResultsRow.add(formatter.format(result.bufferTime));
            sourceResultsRow.add(formatter.format(result.processTime));
            sourceResultsRow.add(formatter.format(result.bufferTimeDispersion));
            sourceResultsRow.add(formatter.format(result.processTimeDispersion));
            sourceResultsTable.add(sourceResultsRow);
        }
        return sourceResultsTable;
    }

    public List<List<String>> getProcessorResults() {
        List<List<String>> processorResultsTable = new ArrayList<>();
        for (AnalyzerResults result : processorsResult) {
            List<String> processorResultsRow = new ArrayList<>();
            processorResultsRow.add(String.valueOf(result.number));
            processorResultsRow.add(formatter.format(result.usageRate));
            processorResultsTable.add(processorResultsRow);
        }
        return processorResultsTable;
    }

    private void analyzeSources() {
        ProductionManager pm = simulator.getProductionManager();
        List<Source> sources = pm.getSources();
        List<List<Request>> rejected = pm.getRejectedRequests();
        SelectionManager sm = simulator.getSelectionManager();
        List<List<Request>> success = sm.getSuccessRequests();
        for (Source s : sources) {
            AnalyzerResults r = new AnalyzerResults();
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
        List<Processor> processors = sm.getProcessors();
        double endTime = simulator.getEndTime();

        for (Processor p : processors) {
            AnalyzerResults r = new AnalyzerResults();
            r.number = p.getNumber();
            r.usageRate = p.getWorkTime() / endTime;
            processorsResult.add(r);
        }
    }
}

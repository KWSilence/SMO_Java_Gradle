package gui.tab;

import configs.SimulationConfig;
import gui.MainGUI;
import gui.SimulatorThread;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import system.component.Processor;
import system.component.Request;
import system.manager.ProductionManager;
import system.manager.SelectionManager;
import system.simulator.Simulator;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyzeTab implements TabCreator {
    private enum SelectorType {
        SOURCE, PROCESSOR, BUFFER
    }

    private enum SeriesType {
        REJECT_PROBABILITY, LIFE_TIME, PROCESSORS_USING_RATE
    }

    @FunctionalInterface
    private interface OnAnalyzeComplete {
        void analyzeComplete();
    }

    @FunctionalInterface
    private interface OnSeriesUpdate {
        void seriesUpdate(SeriesType type, double x, double y);
    }

    private final JPanel root;
    private final boolean debug;
    private final ArrayList<SimulatorThread> simToAnalyze = new ArrayList<>();
    private Thread analyzeThread = null;

    public AnalyzeTab(LayoutManager layoutManager, boolean debug) {
        this.root = new JPanel(layoutManager);
        this.debug = debug;

        //[COM]{ELEMENT} Tab Analyze: reject probability chart
        JFreeChart rejectProbabilityChart = createChart("RejectProbability", "Count", "Probability", null);
        root.add(new ChartPanel(rejectProbabilityChart));
        //[COM]{ELEMENT} Tab Analyze: lifetime chart
        JFreeChart lifeTimeChart = createChart("LifeTime", "Count", "Time", null);
        root.add(new ChartPanel(lifeTimeChart));
        //[COM]{ELEMENT} Tab Analyze: processors using rate chart
        JFreeChart processorsUsingRateChart = createChart("ProcessorsUsingRate", "Count", "Rate", null);
        root.add(new ChartPanel(processorsUsingRateChart), "wrap");
        //[COM]{ELEMENT} Tab Analyze: selection of variable element combobox
        JComboBox<String> selectorCombobox = new JComboBox<>(new String[]{"Source", "Processor", "Buffer"});
        root.add(selectorCombobox);
        //[COM]{ELEMENT} Tab Analyze: "from" text field
        root.add(new JLabel("From"), "split 8");
        JTextField fromTextField = new JTextField("10");
        root.add(fromTextField);
        //[COM]{ELEMENT} Tab Analyze: "to" text field
        root.add(new JLabel("To"));
        JTextField toTextField = new JTextField("100");
        root.add(toTextField);
        //[COM]{ELEMENT} Tab Analyze: "lambda" text field
        root.add(new JLabel("Lambda"));
        JTextField lambdaTextField = new JTextField("1.0");
        root.add(lambdaTextField);
        //[COM]{ELEMENT} Tab Analyze: visualization step for charts text field -- threads count
        root.add(new JLabel("VStep"));
        JTextField visualStepTextField = new JTextField("5");
        root.add(visualStepTextField);
        //[COM]{ACTION} Tab Analyze: combobox change buffer->!lambda
        selectorCombobox.addActionListener(e -> lambdaTextField.setEnabled(selectorCombobox.getSelectedIndex() != 2));
        //[COM]{ELEMENT} Tab Analyze: stop button
        JButton stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        root.add(stopButton, "split 2");
        //[COM]{ELEMENT} Tab Analyze: start button
        JButton startButton = new JButton("Launch");
        root.add(startButton);
        //[COM]{ACTION} Tab Analyze: stop button
        stopButton.addActionListener(e -> {
            stopAnalyze();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        });

        Map<SeriesType, JFreeChart> charts = new HashMap<>(3);
        charts.put(SeriesType.REJECT_PROBABILITY, rejectProbabilityChart);
        charts.put(SeriesType.LIFE_TIME, lifeTimeChart);
        charts.put(SeriesType.PROCESSORS_USING_RATE, processorsUsingRateChart);

        Map<SeriesType, XYSeries> series = new HashMap<>(3);
        series.put(SeriesType.REJECT_PROBABILITY, new XYSeries("None"));
        series.put(SeriesType.LIFE_TIME, new XYSeries("None"));
        series.put(SeriesType.PROCESSORS_USING_RATE, new XYSeries("None"));

        for (SeriesType type : charts.keySet()) {
            charts.get(type).getXYPlot().setDataset(new XYSeriesCollection(series.get(type)));
        }

        //[COM]{ACTION} Tab Analyze: start button
        startButton.addActionListener(e -> {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            SelectorType selector = SelectorType.values()[selectorCombobox.getSelectedIndex()];
            int minCount = Integer.parseInt(fromTextField.getText());
            int maxCount = Integer.parseInt(toTextField.getText());
            double lambda = Double.parseDouble(lambdaTextField.getText());
            int visualisationStep = Integer.parseInt(visualStepTextField.getText());

            String name = getSeriesName(selector, minCount, maxCount, lambda);
            for (SeriesType type : series.keySet()) {
                XYSeries xySeries = series.get(type);
                xySeries.clear();
                xySeries.setKey(name);
            }

            analyze(selector, minCount, maxCount, lambda, visualisationStep,
                    (type, index, value) -> {
                        XYSeries xySeries = series.get(type);
                        if (xySeries != null) {
                            xySeries.add(index, value);
                        }
                    },
                    () -> {
                        stopAnalyze();
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                    }
            );
        });
    }

    private JFreeChart createChart(String title, String xl, String yl, XYDataset ds) {
        return ChartFactory.createXYLineChart(title, xl, yl, ds, PlotOrientation.VERTICAL, true, true, true);
    }

    private void analyze(
            SelectorType selector,
            int minCount,
            int maxCount,
            double lambda,
            int visualisationStep,
            OnSeriesUpdate onSeriesUpdate,
            OnAnalyzeComplete onAnalyzeComplete
    ) {
        analyzeThread = new Thread() {
            private void checkInterruption() throws InterruptedException {
                if (isInterrupted()) {
                    throw new InterruptedException();
                }
            }

            @Override
            public void run() {
                ArrayList<SimulatorThread> buffer = new ArrayList<>();
                SimulationConfig.ConfigJSON config = SimulationConfig.readJSON(MainGUI.getDefaultConfigPath(debug));
                try {
                    for (int i = minCount; i <= maxCount; i++) {
                        checkInterruption();
                        List<Double> sources = getSources(selector, i, config, lambda);
                        List<Double> processors = getProcessors(selector, i, config, lambda);
                        int bufferCapacity = getBufferCapacity(selector, i, config);
                        SimulationConfig.ConfigJSON configJSON = new SimulationConfig.ConfigJSON(
                                config.getRequestsCount(), bufferCapacity, sources, processors
                        );
                        Simulator tmpSimulator = new Simulator(new SimulationConfig(configJSON));
                        SimulatorThread tmpSimulatorThread = new SimulatorThread(tmpSimulator, true);
                        buffer.add(tmpSimulatorThread);
                        tmpSimulatorThread.start();
                        if (i >= maxCount || buffer.size() >= visualisationStep) {
                            checkInterruption();
                            simToAnalyze.addAll(buffer);
                            buffer.clear();
                            int ind = 1;
                            for (SimulatorThread simulatorThread : simToAnalyze) {
                                simulatorThread.join();
                                checkInterruption();
                                Simulator simulator = simulatorThread.getSimulator();
                                int index = i - simToAnalyze.size() + ind;
                                addSeries(index, onSeriesUpdate, simulator, config.getRequestsCount());
                                ind++;
                            }
                            checkInterruption();
                            simToAnalyze.clear();
                        }
                    }
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                    Thread.currentThread().interrupt();
                } finally {
                    onAnalyzeComplete.analyzeComplete();
                }
            }
        };
        analyzeThread.start();
    }

    private void stopAnalyze() {
        if (analyzeThread != null) {
            analyzeThread.interrupt();
            analyzeThread = null;
        }
        for (SimulatorThread simulator : simToAnalyze) {
            simulator.interrupt();
        }
        simToAnalyze.clear();
    }

    private String getSeriesName(SelectorType selector, int minCount, int maxCount, double lambda) {
        StringBuilder stringBuilder = new StringBuilder();
        switch (selector) {
            case SOURCE -> {
                stringBuilder.append("Source[");
                stringBuilder.append(minCount);
                stringBuilder.append(":");
                stringBuilder.append(maxCount);
                stringBuilder.append("], lambda=");
                stringBuilder.append(lambda);
            }
            case PROCESSOR -> {
                stringBuilder.append("Processor[");
                stringBuilder.append(minCount);
                stringBuilder.append(":");
                stringBuilder.append(maxCount);
                stringBuilder.append("], lambda=");
                stringBuilder.append(lambda);
            }
            case BUFFER -> {
                stringBuilder.append("BufferCapacity[");
                stringBuilder.append(minCount);
                stringBuilder.append(":");
                stringBuilder.append(maxCount);
                stringBuilder.append("]");
            }
            default -> stringBuilder.append("NONE");
        }
        return stringBuilder.toString();
    }

    private void addSeries(int index, OnSeriesUpdate onSeriesUpdate, Simulator simulator, int requestCount) {
        SelectionManager selectionManager = simulator.getSelectionManager();
        ProductionManager productionManager = simulator.getProductionManager();
        double rejectProbability = (double) productionManager.getFullRejectCount() / requestCount;
        onSeriesUpdate.seriesUpdate(SeriesType.REJECT_PROBABILITY, index, rejectProbability);
        double totalRequestsLifeTime = 0;
        for (List<Request> requests : selectionManager.getSuccessRequests()) {
            totalRequestsLifeTime += requests.stream().mapToDouble(Request::getLifeTime).sum();
        }
        double avgLifeTime = totalRequestsLifeTime / selectionManager.getFullSuccessCount();
        onSeriesUpdate.seriesUpdate(SeriesType.LIFE_TIME, index, avgLifeTime);
        double totalAvgProcessorUsingRate = 0;
        for (Processor processor : selectionManager.getProcessors()) {
            totalAvgProcessorUsingRate += processor.getWorkTime() / simulator.getEndTime();
        }
        double avgProcessorsUsingRate = totalAvgProcessorUsingRate / selectionManager.getProcessors().size();
        onSeriesUpdate.seriesUpdate(SeriesType.PROCESSORS_USING_RATE, index, avgProcessorsUsingRate);
    }

    private List<Double> getSources(SelectorType selector, int iter, SimulationConfig.ConfigJSON config, double val) {
        if (selector == SelectorType.SOURCE) {
            ArrayList<Double> sources = new ArrayList<>();
            for (int j = 0; j < iter; j++) {
                sources.add(val);
            }
            return sources;
        }
        return config.getSources();
    }

    private List<Double> getProcessors(SelectorType selector, int iter, SimulationConfig.ConfigJSON config, double val) {
        if (selector == SelectorType.PROCESSOR) {
            List<Double> processors = new ArrayList<>();
            for (int j = 0; j < iter; j++) {
                processors.add(val);
            }
            return processors;
        }
        return config.getProcessors();
    }

    private int getBufferCapacity(SelectorType selector, int iter, SimulationConfig.ConfigJSON config) {
        if (selector == SelectorType.BUFFER) {
            return iter;
        }
        return config.getBufferCapacity();
    }

    @Override
    public JPanel getRoot() {
        return root;
    }
}

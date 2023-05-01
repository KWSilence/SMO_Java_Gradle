package gui.tab;

import configs.SimulationConfig;
import gui.ComponentHelper.AutoHelper;
import gui.MainGUI;
import gui.SimulatorThread;
import gui.TableHelper;
import system.analyzer.Analyzer;
import system.analyzer.RequestCountAnalyzer;
import system.simulator.Simulator;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AutoTab implements TabCreator {
    public enum ResultType {
        SOURCES, PROCESSORS
    }

    @FunctionalInterface
    private interface OnAnalyzeComplete {
        void analyzeCompleted(Analyzer analyzer);
    }

    @FunctionalInterface
    private interface OnProgressChanged {
        void progressChanged(Integer progress);
    }

    private Thread lineMover = null;
    private SimulatorThread autoSimulatorThread = null;
    private RequestCountAnalyzer requestCountAnalyzer = null;
    private final boolean debug;
    private final JPanel root;
    private final JTable sourcesResultsTable;
    private final JTable processorsResultsTable;

    public AutoTab(LayoutManager layoutManager, boolean debug) {
        this.root = new JPanel(layoutManager);
        this.debug = debug;
        //[COM]{ELEMENT} Tab Auto: sources results table
        String[] sourcesResultsHeaders = new String[]{
                "Source", "RequestsGen", "RejectProb", "StayTime", "WaitingTime", "ProcTime", "DisWaitingTime",
                "DisProcTime"
        };
        sourcesResultsTable = TableHelper.createTable(sourcesResultsHeaders, null);
        root.add(new JScrollPane(sourcesResultsTable), "wrap, span, grow");
        //[COM]{ELEMENT} Tab Auto: processors results table
        String[] processorsResultsHeaders = new String[]{"Processor", "UsageRate"};
        processorsResultsTable = TableHelper.createTable(processorsResultsHeaders, null);
        root.add(new JScrollPane(processorsResultsTable), "wrap, span");
        //[COM]{ELEMENT} Tab Auto: progressbar
        JProgressBar progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        root.add(progressBar);
        //[COM]{ELEMENT} Tab Auto: stop button
        JButton stopButton = new JButton("Stop");
        stopButton.setName(AutoHelper.stop);
        stopButton.setEnabled(false);
        root.add(stopButton, "split 4");
        //[COM]{ELEMENT} Tab Auto: start button
        JButton startButton = new JButton("Start Auto");
        startButton.setName(AutoHelper.start);
        root.add(startButton);
        //[COM]{ELEMENT} Tab Auto: use N0 checkbox
        JCheckBox useN0CheckBox = new JCheckBox("Use N0");
        useN0CheckBox.setName(AutoHelper.useN0);
        root.add(useN0CheckBox);
        //[COM]{ELEMENT} Tab Auto: "N0" text field
        JTextField n0TextField = new JTextField("10000");
        n0TextField.setName(AutoHelper.n0);
        n0TextField.setEnabled(false);
        root.add(n0TextField);
        //[COM]{ACTION} Tab Auto: use N0 checkbox
        useN0CheckBox.addActionListener(e -> n0TextField.setEnabled(useN0CheckBox.isSelected()));
        //[COM]{ACTION} Tab Auto: start button
        startButton.addActionListener(e -> {
            SimulationConfig simulationConfig = MainGUI.useDefaultConfigFile(debug);

            TableHelper.initTable(sourcesResultsTable, simulationConfig.getConfig().getSources().size());
            TableHelper.initTable(processorsResultsTable, simulationConfig.getConfig().getProcessors().size());

            final Integer N0;
            if (useN0CheckBox.isSelected()) {
                N0 = Integer.parseInt(n0TextField.getText());
                progressBar.setMaximum(N0);
            } else {
                N0 = null;
                Simulator simulator = new Simulator(simulationConfig);
                autoSimulatorThread = new SimulatorThread(simulator, true);
                progressBar.setMaximum(simulationConfig.getConfig().getRequestsCount());
            }

            progressBar.setValue(0);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            lineMover = createLineMoverThread(
                    N0,
                    progress -> progressBar.setValue(progress == null ? progressBar.getMaximum() : progress),
                    analyzer -> {
                        setResults(analyzer, ResultType.SOURCES);
                        setResults(analyzer, ResultType.PROCESSORS);
                        stopAuto();
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                    }
            );
            lineMover.start();
        });
        //[COM]{ACTION} Tab Auto: stop button
        stopButton.addActionListener(e -> {
            stopAuto();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        });
    }

    private Thread createLineMoverThread(Integer n0, OnProgressChanged onProgressChanged, OnAnalyzeComplete onAnalyzeComplete) {
        return new Thread(() -> {
            Analyzer analyzer = null;
            try {
                if (n0 != null) {
                    requestCountAnalyzer = new RequestCountAnalyzer(n0, MainGUI.useDefaultConfigFile(debug));
                    requestCountAnalyzer.start();
                    requestCountAnalyzer.join();
                    onProgressChanged.progressChanged(null);
                    autoSimulatorThread = new SimulatorThread(requestCountAnalyzer.getLastSimulator(), null);
                } else {
                    autoSimulatorThread.start();
                    while (autoSimulatorThread.isAlive()) {
                        if (autoSimulatorThread.isInterrupted()) throw new InterruptedException();
                        onProgressChanged.progressChanged(autoSimulatorThread.getSimulator().getProgress());
                    }
                    onProgressChanged.progressChanged(autoSimulatorThread.getSimulator().getProgress());
                }
                if (!Thread.currentThread().isInterrupted()) {
                    analyzer = new Analyzer(autoSimulatorThread.getSimulator());
                    analyzer.analyze(true);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                onAnalyzeComplete.analyzeCompleted(analyzer);
            }
        });
    }

    public void setResults(Analyzer analyzer, ResultType resultType) {
        if (analyzer == null || resultType == null) return;
        List<List<String>> results = null;
        JTable table = null;
        switch (resultType) {
            case SOURCES -> {
                results = analyzer.getSourceResults();
                table = sourcesResultsTable;
            }
            case PROCESSORS -> {
                results = analyzer.getProcessorResults();
                table = processorsResultsTable;
            }
        }
        if (table == null) return;
        TableHelper.clearTable(table);
        if (results == null || results.isEmpty()) return;
        TableHelper.fillTable(table, results);
    }

    private void stopAuto() {
        if (lineMover != null) {
            lineMover.interrupt();
            lineMover = null;
        }
        if (requestCountAnalyzer != null) {
            requestCountAnalyzer.interrupt();
            requestCountAnalyzer = null;
        }
        if (autoSimulatorThread != null) {
            autoSimulatorThread.interrupt();
            autoSimulatorThread = null;
        }
    }

    @Override
    public JPanel getRoot() {
        return root;
    }
}

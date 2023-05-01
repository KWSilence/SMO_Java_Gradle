package gui.tab;

import configs.SimulationConfig;
import gui.ComponentHelper.StepHelper;
import gui.MainGUI;
import gui.SimulatorThread;
import gui.TableHelper;
import system.analyzer.Analyzer;
import system.component.Buffer;
import system.component.Processor;
import system.component.Request;
import system.simulator.Simulator;
import system.simulator.SimulatorEvent;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class StepTab implements TabCreator {
    public interface OnAnalyzeStart {
        void analyzeStart(Analyzer analyzer);
    }

    private final JPanel root;
    private SimulatorThread stepSimulationThread = null;
    private Runnable stepTask = null;
    private boolean skipState = false;
    private final NumberFormat formatter = new DecimalFormat("#0.000");
    private final JButton[] buttons;

    public StepTab(LayoutManager layoutManager, boolean debug, OnAnalyzeStart onAnalyzeStart) {
        this.root = new JPanel(layoutManager);

        //[COM]{ELEMENT} Tab Step: source table
        JTable sourcesTable = TableHelper.createTable(new String[]{"Source", "Request", "GenerateTime"}, null);
        root.add(new JScrollPane(sourcesTable));
        //[COM]{ELEMENT} Tab Step: buffer table
        JTable bufferTable = TableHelper.createTable(new String[]{"Buffer", "Request", "TakeTime"}, null);
        root.add(new JScrollPane(bufferTable), "wrap");
        //[COM]{ELEMENT} Tab Step: processor table
        JTable processorsTable = TableHelper.createTable(new String[]{"Processor", "Request", "TakeTime", "ReleaseTime"}, null);
        root.add(new JScrollPane(processorsTable));
        //[COM]{ELEMENT} Tab Step: log area
        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        root.add(logScrollPane, "wrap");
        //[COM]{ELEMENT} Tab Step: progressbar
        JProgressBar progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        root.add(progressBar);
        //[COM]{ELEMENT} Tab Step: stop button
        JButton stopButton = new JButton("Stop");
        stopButton.setName(StepHelper.stop);
        stopButton.setEnabled(false);
        root.add(stopButton, "split 4");
        //[COM]{ELEMENT} Tab Step: start button
        JButton startButton = new JButton("Start Steps");
        startButton.setName(StepHelper.start);
        root.add(startButton);
        //[COM]{ELEMENT} Tab Step: skip button
        JButton skipButton = new JButton("Skip");
        skipButton.setName(StepHelper.skip);
        skipButton.setEnabled(false);
        root.add(skipButton);
        buttons = new JButton[]{startButton, stopButton, skipButton};
        //[COM]{ELEMENT} Tab Step: auto scroll checkbox
        JCheckBox autoScrollCheckBox = new JCheckBox("textAutoScroll");
        autoScrollCheckBox.setName(StepHelper.autoScroll);
        root.add(autoScrollCheckBox);
        //[COM]{ACTION} Tab Step: auto scroll event using checkbox
        logScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> e.getAdjustable().setValue(
                autoScrollCheckBox.isSelected() ? e.getAdjustable().getMaximum() : e.getAdjustable().getValue()));
        //[COM]{ACTION} Tab Step: start button
        startButton.addActionListener(e -> {
            if (stepSimulationThread == null) {
                SimulationConfig simulationConfig = MainGUI.useDefaultConfigFile(debug);

                TableHelper.initTable(sourcesTable, simulationConfig.getConfig().getSources().size());
                TableHelper.initTable(bufferTable, simulationConfig.getConfig().getBufferCapacity());
                TableHelper.initTable(processorsTable, simulationConfig.getConfig().getProcessors().size());

                logArea.setText("");

                progressBar.setMaximum(simulationConfig.getConfig().getRequestsCount());
                progressBar.setValue(0);

                stepSimulationThread = new SimulatorThread(new Simulator(simulationConfig), null);

                startButton.setText("Next Step");
                stopButton.setEnabled(true);
                skipButton.setEnabled(true);

                stepTask = () -> {
                    SimulatorThread simulatorThread = stepSimulationThread;
                    Simulator finalSimulator = simulatorThread.getSimulator();
                    do {
                        stepSimulationThread.getSimulator().simulationStep();
                        SimulatorEvent event = finalSimulator.getLastEvent();
                        switch (event.getType()) {
                            case GENERATE -> processGenerate(event, sourcesTable, logArea);
                            case PACKAGE -> processPackage(event, bufferTable, logArea);
                            case TAKE -> processTake(event, sourcesTable, bufferTable, processorsTable, logArea);
                            case BUFFER -> processBuffer(event, sourcesTable, bufferTable, logArea);
                            case REJECT -> processReject(event, sourcesTable, logArea);
                            case RELEASE -> processRelease(event, processorsTable, logArea);
                            case WORK_END -> processWorkEnd(event, startButton, skipButton, logArea);
                            case ANALYZE -> processAnalyze(finalSimulator, buttons, logArea, onAnalyzeStart);
                        }
                        progressBar.setValue(finalSimulator.getProgress());
                    } while (skipState && finalSimulator.canContinue() && !simulatorThread.isInterrupted());
                };
            } else {
                startSimulation();
            }
        });
        //[COM]{ACTION} Tab Step: stop button
        stopButton.addActionListener(e -> {
            stepSimulationThread.interrupt();
            stepSimulationThread = null;
            stepTask = null;
            skipState = false;
            startButton.setText("Start Steps");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            skipButton.setEnabled(false);
        });
        //[COM]{ACTION} Tab Step: skip button
        skipButton.addActionListener(e -> {
            skipButton.setEnabled(false);
            skipState = true;
            startButton.getActionListeners()[0].actionPerformed(e);
            startButton.setEnabled(false);
        });
    }

    private void processGenerate(SimulatorEvent event, JTable sourcesTable, JTextArea logTextArea) {
        Request request = event.getRequest();
        if (!skipState) {
            sourcesTable.setValueAt(request.getSourceNumber() + "." + request.getNumber(), request.getSourceNumber(), 1);
            sourcesTable.setValueAt(formatter.format(request.getTime()), request.getSourceNumber(), 2);
        }
        logTextArea.append(event.getLog());
    }

    private void processPackage(SimulatorEvent event, JTable bufferTable, JTextArea logTextArea) {
        if (!skipState) {
            Buffer buffer = event.getBuffer();
            for (Request request : buffer.getRequestsPackage()) {
                int index = buffer.getList().indexOf(request);
                bufferTable.setValueAt((bufferTable.getValueAt(index, 1) + "P"), index, 1);
            }
        }
        logTextArea.append(event.getLog());
    }

    private void processTake(SimulatorEvent event, JTable sourcesTable, JTable bufferTable, JTable processorsTable, JTextArea logTextArea) {
        Request request = event.getRequest();
        Processor processor = event.getProcessor();
        Buffer buffer = event.getBuffer();
        if (!skipState) {
            if (buffer != null) {
                TableHelper.clearRowWithMove(bufferTable, event.getBuffer().getTakeIndex());
            } else {
                sourcesTable.setValueAt(null, request.getSourceNumber(), 1);
                sourcesTable.setValueAt(null, request.getSourceNumber(), 2);
            }
            processorsTable.setValueAt(request.getSourceNumber() + "." + request.getNumber(), processor.getNumber(), 1);
            processorsTable.setValueAt(formatter.format(request.getTime() + request.getTimeInBuffer()), processor.getNumber(), 2);
            processorsTable.setValueAt(null, processor.getNumber(), 3);
        }
        logTextArea.append(event.getLog());
    }

    private void processBuffer(SimulatorEvent event, JTable sourcesTable, JTable bufferTable, JTextArea logArea) {
        Request request = event.getRequest();
        Buffer buffer = event.getBuffer();
        int row = buffer.getSize() - 1;
        if (!skipState) {
            sourcesTable.setValueAt(null, request.getSourceNumber(), 1);
            sourcesTable.setValueAt(null, request.getSourceNumber(), 2);
            bufferTable.setValueAt(request.getSourceNumber() + "." + request.getNumber(), row, 1);
            bufferTable.setValueAt(formatter.format(request.getTime()), row, 2);
        }
        logArea.append(event.getLog());
    }

    private void processReject(SimulatorEvent event, JTable sourcesTable, JTextArea logArea) {
        Request request = event.getRequest();
        if (!skipState) {
            sourcesTable.setValueAt(null, request.getSourceNumber(), 1);
            sourcesTable.setValueAt(null, request.getSourceNumber(), 2);
        }
        logArea.append(event.getLog());
    }

    private void processRelease(SimulatorEvent event, JTable processorsTable, JTextArea logArea) {
        Processor processor = event.getProcessor();
        if (!skipState) {
            processorsTable.setValueAt(formatter.format(processor.getProcessTime()), processor.getNumber(), 3);
        }
        logArea.append(event.getLog());
    }

    private void processWorkEnd(SimulatorEvent event, JButton startButton, JButton skipButton, JTextArea logArea) {
        logArea.append(event.getLog());
        startButton.setEnabled(true);
        skipButton.setEnabled(false);
        if (skipState) {
            skipState = false;
        }
    }

    private void processAnalyze(Simulator finalSimulator, JButton[] buttons, JTextArea logArea, OnAnalyzeStart onAnalyzeStart) {
        Analyzer analyzer = new Analyzer(finalSimulator);
        analyzer.analyze(true);
        onAnalyzeStart.analyzeStart(analyzer);

        stepSimulationThread = null;
        buttons[0].setText("Start Steps");
        buttons[0].setEnabled(true);
        buttons[1].setEnabled(false);
        buttons[2].setEnabled(false);
        skipState = false;
        logArea.append("Simulation was analyzed.");
    }

    private void startSimulation() {
        if (skipState) {
            Thread thread = new Thread(stepTask);
            stepSimulationThread.setThread(thread);
            thread.start();
        } else {
            stepTask.run();
        }
    }

    @Override
    public JPanel getRoot() {
        return root;
    }
}

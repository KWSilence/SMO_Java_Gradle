package gui;

import configs.SimulationConfig;
import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import smo_system.analyzer.Analyzer;
import smo_system.analyzer.RequestCountAnalyzer;
import smo_system.component.Buffer;
import smo_system.component.Processor;
import smo_system.component.Request;
import smo_system.simulator.Simulator;
import smo_system.simulator.SimulatorEvent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainGUI {
    private final NumberFormat formatter = new DecimalFormat("#0.000");
    private Thread lineMover = null;
    private Runnable task = null;
    private ArrayList<SimulatorThread> simToAnalyze = new ArrayList<>();
    private final HashMap<String, SimulatorThread> simulatorThreads = new HashMap<>();
    private boolean skipState = false;

    MainGUI(boolean debug) {
        SimulationConfig.initDefaultConfigFile(debug);

        simulatorThreads.put("steps", null);
        simulatorThreads.put("auto", null);

        JFrame frame = new JFrame("test");
        frame.setLayout(new MigLayout("", "[fill, grow]", "[fill, grow]"));

        JTabbedPane tabbedPane = new JTabbedPane();

        //[COM]{TAB} Tab Auto
        JPanel first = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow][fill, grow][]"));
        //[COM]{ELEMENT} Tab Auto: sources results table
        JTable tf1 = createTable(
                new String[]{"Source", "RequestsGen", "RejectProb", "StayTime", "WaitingTime", "ProcTime", "DisWaitingTime",
                        "DisProcTime"}, null);
        first.add(new JScrollPane(tf1), "wrap, span, grow");
        //[COM]{ELEMENT} Tab Auto: processors results table
        JTable tf2 = createTable(new String[]{"Processor", "UsageRate"}, null);
        first.add(new JScrollPane(tf2), "wrap, span");
        //[COM]{ELEMENT} Tab Auto: progressbar
        JProgressBar pbf1 = new JProgressBar();
        pbf1.setMinimum(0);
        first.add(pbf1);
        //[COM]{ELEMENT} Tab Auto: stop button
        JButton bf1 = new JButton("Stop");
        bf1.setEnabled(false);
        first.add(bf1, "split 4");
        //[COM]{ELEMENT} Tab Auto: start button
        JButton bf2 = new JButton("Start Auto");
        first.add(bf2);
        //[COM]{ELEMENT} Tab Auto: use N0 checkbox
        JCheckBox cbf1 = new JCheckBox("Use N0");
        first.add(cbf1);
        //[COM]{ELEMENT} Tab Auto: "N0" text field
        JTextField tff1 = new JTextField("100");
        tff1.setEnabled(false);
        first.add(tff1);
        //[COM]{ACTION} Tab Auto: use N0 checkbox
        cbf1.addActionListener(e -> tff1.setEnabled(cbf1.isSelected()));
        //[COM]{ACTION} Tab Auto: start button
        bf2.addActionListener(e -> {
            SimulationConfig simulationConfig = SimulationConfig.useDefaultConfigFile(debug);

            clearTable(tf1);
            initTableRows(tf1, simulationConfig.getSources().size());
            clearTable(tf2);
            initTableRows(tf2, simulationConfig.getProcessors().size());

            final int N0;
            if (cbf1.isSelected()) {
                N0 = Integer.parseInt(tff1.getText());
                pbf1.setMaximum(N0);
            } else {
                N0 = 0;
                Simulator simulator = new Simulator(simulationConfig);
                simulatorThreads.put("auto", new SimulatorThread(simulator, true));
                pbf1.setMaximum(simulationConfig.getProductionManager().getMaxRequestCount());
            }

            pbf1.setValue(0);
            bf2.setEnabled(false);
            bf1.setEnabled(true);

            lineMover = new Thread(() -> {
                if (cbf1.isSelected()) {
                    RequestCountAnalyzer analyzer = new RequestCountAnalyzer(N0, debug);
                    analyzer.analyze();
                    pbf1.setValue(pbf1.getMaximum());
                    simulatorThreads.put("auto", new SimulatorThread(analyzer.getLastSimulator(), null));
                } else {
                    SimulatorThread simulatorThread = simulatorThreads.get("auto");
                    simulatorThread.start();
                    while (true) {
                        if (simulatorThread.isInterrupted()) return;
                        if (!simulatorThread.isAlive()) break;
                        pbf1.setValue(simulatorThread.getSimulator().getProgress());
                    }
                }

                Analyzer analyzer = new Analyzer(simulatorThreads.get("auto").getSimulator());
                analyzer.analyze(true);
                ArrayList<ArrayList<String>> sr = analyzer.getSourceResults();
                ArrayList<ArrayList<String>> pr = analyzer.getProcessorResults();
                clearTable(tf1);
                initTableRows(tf1, sr.size());
                clearTable(tf2);
                initTableRows(tf2, pr.size());

                for (int i = 0; i < sr.size(); i++) {
                    ArrayList<String> el = sr.get(i);
                    for (int j = 1; j < el.size(); j++) {
                        tf1.setValueAt(el.get(j), i, j);
                    }
                }

                for (int i = 0; i < pr.size(); i++) {
                    ArrayList<String> el = pr.get(i);
                    for (int j = 1; j < el.size(); j++) {
                        tf2.setValueAt(el.get(j), i, j);
                    }
                }

                simulatorThreads.put("auto", null);
                bf2.setEnabled(true);
                bf1.setEnabled(false);
            });
            lineMover.start();
        });
        //[COM]{ACTION} Tab Auto: stop button
        bf1.addActionListener(e -> {
            lineMover.interrupt();
            SimulatorThread simulatorThread = simulatorThreads.get("auto");
            if (simulatorThread != null) {
                simulatorThread.interrupt();
                simulatorThreads.put("auto", null);
            }
            bf2.setEnabled(true);
            bf1.setEnabled(false);
        });
        tabbedPane.addTab("auto", first);

        //[COM]{TAB} Tab Step
        JPanel second = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow][fill, grow][]"));
        //[COM]{ELEMENT} Tab Step: source table
        JTable ts1 = createTable(new String[]{"Source", "Request", "GenerateTime"}, null);
        second.add(new JScrollPane(ts1));
        //[COM]{ELEMENT} Tab Step: buffer table
        JTable ts2 = createTable(new String[]{"Buffer", "Request", "TakeTime"}, null);
        second.add(new JScrollPane(ts2), "wrap");
        //[COM]{ELEMENT} Tab Step: processor table
        JTable ts3 = createTable(new String[]{"Processor", "Request", "TakeTime", "ReleaseTime"}, null);
        second.add(new JScrollPane(ts3));
        //[COM]{ELEMENT} Tab Step: log area
        JTextArea tps1 = new JTextArea();
        tps1.setEditable(false);
        JScrollPane sps1 = new JScrollPane(tps1);
        second.add(sps1, "wrap");
        //[COM]{ELEMENT} Tab Step: progressbar
        JProgressBar pbs1 = new JProgressBar();
        pbs1.setMinimum(0);
        second.add(pbs1);
        //[COM]{ELEMENT} Tab Step: stop button
        JButton bs1 = new JButton("Stop");
        bs1.setEnabled(false);
        second.add(bs1, "split 4");
        //[COM]{ELEMENT} Tab Step: start button
        JButton bs2 = new JButton("Start Steps");
        second.add(bs2);
        //[COM]{ELEMENT} Tab Step: skip button
        JButton bs3 = new JButton("Skip");
        bs3.setEnabled(false);
        second.add(bs3);
        //[COM]{ELEMENT} Tab Step: auto scroll checkbox
        JCheckBox cbs1 = new JCheckBox("textAutoScroll");
        second.add(cbs1);
        //[COM]{ACTION} Tab Step: auto scroll event using checkbox
        sps1.getVerticalScrollBar().addAdjustmentListener(e -> e.getAdjustable().setValue(
                cbs1.isSelected() ? e.getAdjustable().getMaximum() : e.getAdjustable().getValue()));
        //[COM]{ACTION} Tab Step: start button
        bs2.addActionListener(e -> {
            SimulatorThread simulatorThread = simulatorThreads.get("steps");
            if (simulatorThread == null) {
                SimulationConfig simulationConfig = SimulationConfig.useDefaultConfigFile(debug);

                clearTable(ts1);
                initTableRows(ts1, simulationConfig.getSources().size());
                clearTable(ts2);
                initTableRows(ts2, simulationConfig.getBuffer().getCapacity());
                clearTable(ts3);
                initTableRows(ts3, simulationConfig.getProcessors().size());

                tps1.setText("");

                pbs1.setMaximum(simulationConfig.getProductionManager().getMaxRequestCount());
                pbs1.setValue(0);

                simulatorThreads.put("steps", new SimulatorThread(new Simulator(simulationConfig), null));
                simulatorThread = simulatorThreads.get("steps");

                bs2.setText("Next Step");
                bs1.setEnabled(true);
                bs3.setEnabled(true);

                Simulator finalSimulator = simulatorThread.getSimulator();
                task = () -> {
                    try {
                        boolean canContinue;
                        do {
                            canContinue = finalSimulator.simulationStep();
                            SimulatorEvent event = finalSimulator.getLastEvent();
                            switch (event.getType()) {
                                case GENERATE -> {
                                    Request request = event.getRequest();
                                    if (!skipState) {
                                        ts1.setValueAt(request.getSourceNumber() + "." + request.getNumber(), request.getSourceNumber(), 1);
                                        ts1.setValueAt(formatter.format(request.getTime()), request.getSourceNumber(), 2);
                                    }
                                    tps1.append(event.getLog());
                                }

                                case PACKAGE -> {
                                    if (!skipState) {
                                        Buffer buffer = event.getBuffer();
                                        for (Request request : buffer.getRequestsPackage()) {
                                            int index = buffer.getList().indexOf(request);
                                            ts2.setValueAt((ts2.getValueAt(index, 1) + "P"), index, 1);
                                        }
                                    }
                                    tps1.append(event.getLog());
                                }

                                case TAKE -> {
                                    Request request = event.getRequest();
                                    Processor processor = event.getProcessor();
                                    Buffer buffer = event.getBuffer();
                                    if (!skipState) {
                                        if (buffer != null) {
                                            clearRowWithMove(ts2, event.getBuffer().getTakeIndex());
                                        } else {
                                            ts1.setValueAt(null, request.getSourceNumber(), 1);
                                            ts1.setValueAt(null, request.getSourceNumber(), 2);
                                        }
                                        ts3.setValueAt(request.getSourceNumber() + "." + request.getNumber(), processor.getNumber(), 1);
                                        ts3.setValueAt(formatter.format(request.getTime() + request.getTimeInBuffer()),
                                                processor.getNumber(), 2);
                                        ts3.setValueAt(null, processor.getNumber(), 3);
                                    }
                                    tps1.append(event.getLog());
                                }

                                case BUFFER -> {
                                    Request request = event.getRequest();
                                    Buffer buffer = event.getBuffer();
                                    int row = buffer.getSize() - 1;
                                    if (!skipState) {
                                        ts1.setValueAt(null, request.getSourceNumber(), 1);
                                        ts1.setValueAt(null, request.getSourceNumber(), 2);
                                        ts2.setValueAt(request.getSourceNumber() + "." + request.getNumber(), row, 1);
                                        ts2.setValueAt(formatter.format(request.getTime()), row, 2);
                                    }
                                    tps1.append(event.getLog());
                                }

                                case REJECT -> {
                                    Request request = event.getRequest();
                                    if (!skipState) {
                                        ts1.setValueAt(null, request.getSourceNumber(), 1);
                                        ts1.setValueAt(null, request.getSourceNumber(), 2);
                                    }
                                    tps1.append(event.getLog());
                                }

                                case RELEASE -> {
                                    Processor processor = event.getProcessor();
                                    if (!skipState) {
                                        ts3.setValueAt(formatter.format(processor.getProcessTime()), processor.getNumber(), 3);
                                    }
                                    tps1.append(event.getLog());
                                }

                                case WORK_END -> {
                                    tps1.append(event.getLog());
                                    bs2.setEnabled(true);
                                    bs3.setEnabled(false);
                                    if (skipState) {
                                        skipState = false;
                                    }
                                }

                                case ANALYZE -> {
                                    Analyzer analyzer = new Analyzer(finalSimulator);
                                    analyzer.analyze(true);
                                    tps1.append("Simulation was analyzed.");
                                    ArrayList<ArrayList<String>> sr = analyzer.getSourceResults();
                                    ArrayList<ArrayList<String>> pr = analyzer.getProcessorResults();
                                    clearTable(tf1);
                                    initTableRows(tf1, sr.size());
                                    clearTable(tf2);
                                    initTableRows(tf2, pr.size());

                                    for (int i = 0; i < sr.size(); i++) {
                                        ArrayList<String> el = sr.get(i);
                                        for (int j = 1; j < el.size(); j++) {
                                            tf1.setValueAt(el.get(j), i, j);
                                        }
                                    }

                                    for (int i = 0; i < pr.size(); i++) {
                                        ArrayList<String> el = pr.get(i);
                                        for (int j = 1; j < el.size(); j++) {
                                            tf2.setValueAt(el.get(j), i, j);
                                        }
                                    }

                                    tabbedPane.setSelectedIndex(0);

                                    simulatorThreads.put("steps", null);
                                    bs2.setText("Start Steps");
                                    bs2.setEnabled(true);
                                    bs1.setEnabled(false);
                                    bs3.setEnabled(false);
                                    skipState = false;
                                }
                            }
                            pbs1.setValue(finalSimulator.getProgress());
                        } while (skipState && canContinue);
                    } catch (Exception ignored) {
                    }
                };
            } else {
                if (skipState) {
                    Thread thread = new Thread(task);
                    simulatorThread.setThread(thread);
                    thread.start();
                } else {
                    task.run();
                }
            }
        });
        //[COM]{ACTION} Tab Step: stop button
        bs1.addActionListener(e -> {
            simulatorThreads.get("steps").interrupt();
            simulatorThreads.put("steps", null);
            bs2.setText("Start Steps");
            bs2.setEnabled(true);
            bs1.setEnabled(false);
            bs3.setEnabled(false);
            skipState = false;
        });
        //[COM]{ACTION} Tab Step: skip button
        bs3.addActionListener(e -> {
            bs3.setEnabled(false);
            skipState = true;
            bs2.getActionListeners()[0].actionPerformed(e);
            bs2.setEnabled(false);
        });
        tabbedPane.addTab("step", second);

        //[COM]{TAB} Tab Analyze
        JPanel third = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow][]"));
        //[COM]{ELEMENT} Tab Analyze: reject probability chart
        JFreeChart chart1 = createChart("RejectProbability", "Count", "Probability", null);
        third.add(new ChartPanel(chart1));
        //[COM]{ELEMENT} Tab Analyze: lifetime chart
        JFreeChart chart2 = createChart("LifeTime", "Count", "Time", null);
        third.add(new ChartPanel(chart2));
        //[COM]{ELEMENT} Tab Analyze: processors using rate chart
        JFreeChart chart3 = createChart("ProcessorsUsingRate", "Count", "Rate", null);
        third.add(new ChartPanel(chart3), "wrap");
        //[COM]{ELEMENT} Tab Analyze: selection of variable element combobox
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"Source", "Processor", "Buffer"});
        third.add(comboBox);
        //[COM]{ELEMENT} Tab Analyze: "from" text field
        third.add(new JLabel("From"), "split 8");
        JTextField tft1 = new JTextField("10");
        third.add(tft1);
        //[COM]{ELEMENT} Tab Analyze: "to" text field
        third.add(new JLabel("To"));
        JTextField tft2 = new JTextField("100");
        third.add(tft2);
        //[COM]{ELEMENT} Tab Analyze: "lambda" text field
        third.add(new JLabel("Lambda"));
        JTextField tft3 = new JTextField("1.0");
        third.add(tft3);
        //[COM]{ELEMENT} Tab Analyze: visualization step for charts text field -- threads count
        third.add(new JLabel("VStep"));
        JTextField tft4 = new JTextField("5");
        third.add(tft4);
        //[COM]{ACTION} Tab Analyze: combobox change buffer->!lambda
        comboBox.addActionListener(e -> tft3.setEnabled(comboBox.getSelectedIndex() != 2));
        //[COM]{ELEMENT} Tab Analyze: stop button
        JButton bt1 = new JButton("Stop");
        bt1.setEnabled(false);
        third.add(bt1, "split 2");
        //[COM]{ELEMENT} Tab Analyze: start button
        JButton bt2 = new JButton("Launch");
        third.add(bt2);
        //[COM]{ACTION} Tab Analyze: stop button
        bt1.addActionListener(e -> {
            bt2.setEnabled(true);
            bt1.setEnabled(false);
            for (SimulatorThread simulator : simToAnalyze) {
                simulator.interrupt();
            }
            simToAnalyze.clear();
            simToAnalyze = null;
        });
        //[COM]{ACTION} Tab Analyze: start button
        bt2.addActionListener(e -> {
            simToAnalyze = new ArrayList<>();
            bt2.setEnabled(false);
            bt1.setEnabled(true);
            int var = comboBox.getSelectedIndex();
            int from = Integer.parseInt(tft1.getText());
            int to = Integer.parseInt(tft2.getText());
            double val = Double.parseDouble(tft3.getText());
            int step = Integer.parseInt(tft4.getText());
            analyze(var, new JFreeChart[]{chart1, chart2, chart3}, from, to, val, step, debug, new JButton[]{bt1, bt2});
        });
        tabbedPane.addTab("analyze", third);

        //[COM]{TAB} Tab Settings
        JPanel fourth = new JPanel(new MigLayout("", "[grow,fill]", "[grow,fill][]"));
        SimulationConfig.ConfigJSON config = SimulationConfig.readJSON(SimulationConfig.getDefaultConfigPath(debug));
        //[COM]{ELEMENT} Tab Settings: sources tab
        JTable th1 = createTable(new String[]{"SourceNumber", "Lambda"}, Collections.singletonList(1));
        initTableRows(th1, config.getSources().size());
        setTableLambdas(th1, config.getSources());
        fourth.add(new JScrollPane(th1));
        //[COM]{ELEMENT} Tab Settings: processors tab
        JTable th2 = createTable(new String[]{"ProcessorNumber", "Lambda"}, Collections.singletonList(1));
        initTableRows(th2, config.getProcessors().size());
        setTableLambdas(th2, config.getProcessors());
        fourth.add(new JScrollPane(th2), "wrap");
        //[COM]{ELEMENT} Tab Settings: sources count text field
        JTextField tfh1 = new JTextField(String.valueOf(config.getSources().size()));
        fourth.add(tfh1, "split 2");
        //[COM]{ELEMENT} Tab Settings: set sources count button
        JButton bh1 = new JButton("Set sources count");
        //[COM]{ACTION} Tab Settings: set sources count button
        bh1.addActionListener(e -> addOrDeleteRows(th1, Integer.parseInt(tfh1.getText())));
        fourth.add(bh1);
        //[COM]{ELEMENT} Tab Settings: processors count text field
        JTextField tfh2 = new JTextField(String.valueOf(config.getProcessors().size()));
        fourth.add(tfh2, "split 2");
        //[COM]{ELEMENT} Tab Settings: set processors count button
        JButton bh2 = new JButton("Set processors count");
        //[COM]{ACTION} Tab Settings: set processors count button
        bh2.addActionListener(e -> addOrDeleteRows(th2, Integer.parseInt(tfh2.getText())));
        fourth.add(bh2, "wrap");
        //[COM]{ELEMENT} Tab Settings: source lambdas text field
        JTextField tfh11 = new JTextField("1.0");
        fourth.add(tfh11, "split 2");
        //[COM]{ELEMENT} Tab Settings: set source lambdas button
        JButton bh11 = new JButton("Set sources lambdas");
        //[COM]{ACTION} Tab Settings: set source Lambdas button
        bh11.addActionListener(e -> {
            String val = tfh11.getText();
            for (int i = 0; i < th1.getRowCount(); i++) {
                th1.setValueAt(val, i, 1);
            }
        });
        fourth.add(bh11);
        //[COM]{ELEMENT} Tab Settings: processor lambdas text field
        JTextField tfh21 = new JTextField("1.0");
        fourth.add(tfh21, "split 2");
        //[COM]{ELEMENT} Tab Settings: set processor lambdas button
        JButton bh21 = new JButton("Set processors lambdas");
        //[COM]{ACTION} Tab Settings: set processor lambdas button
        bh21.addActionListener(e -> {
            String val = tfh21.getText();
            for (int i = 0; i < th2.getRowCount(); i++) {
                th2.setValueAt(val, i, 1);
            }
        });
        fourth.add(bh21, "wrap");
        //[COM]{ELEMENT} Tab Settings: buffer capacity text field
        fourth.add(new JLabel("Buffer Capacity"), "split 4");
        JTextField tfh3 = new JTextField(String.valueOf(config.getBufferCapacity()));
        fourth.add(tfh3);
        //[COM]{ELEMENT} Tab Settings: request count text field
        fourth.add(new JLabel("Requests Count"));
        JTextField tfh4 = new JTextField(String.valueOf(config.getRequestsCount()));
        fourth.add(tfh4);
        //[COM]{ELEMENT} Tab Settings: refresh button
        JButton bh3 = new JButton("Refresh");
        //[COM]{ACTION} Tab Settings: refresh button
        bh3.addActionListener(e -> {
            SimulationConfig.ConfigJSON configRefresh = SimulationConfig.readJSON(SimulationConfig.getDefaultConfigPath(debug));
            clearTable(th1);
            initTableRows(th1, configRefresh.getSources().size());
            setTableLambdas(th1, configRefresh.getSources());
            clearTable(th2);
            initTableRows(th2, configRefresh.getProcessors().size());
            setTableLambdas(th2, configRefresh.getProcessors());
            tfh1.setText(String.valueOf(configRefresh.getSources().size()));
            tfh2.setText(String.valueOf(configRefresh.getProcessors().size()));
            tfh3.setText(String.valueOf(configRefresh.getBufferCapacity()));
            tfh4.setText(String.valueOf(configRefresh.getRequestsCount()));
        });
        fourth.add(bh3, "split 2");
        //[COM]{ELEMENT} Tab Settings: save button
        JButton bh4 = new JButton("Save");
        //[COM]{ACTION} Tab Settings: save button
        bh4.addActionListener(e -> {
            ArrayList<Double> sources = getTableLambdas(th1);
            ArrayList<Double> processors = getTableLambdas(th2);
            int bufferCapacity = Integer.parseInt(tfh3.getText());
            int requestsCount = Integer.parseInt(tfh4.getText());
            SimulationConfig.ConfigJSON configSave = new SimulationConfig.ConfigJSON(
                    sources, processors, bufferCapacity, requestsCount
            );
            File configFile = new File(SimulationConfig.getDefaultConfigPath(debug));
            SimulationConfig.saveConfigFile(configFile, configSave);
        });
        fourth.add(bh4);
        tabbedPane.addTab("settings", fourth);

        frame.add(tabbedPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setSize(1280, 720);
        frame.setVisible(true);
    }

    private void clearTable(JTable table) {
        String[] s = new String[table.getColumnCount()];
        for (int i = 0; i < table.getColumnCount(); i++) {
            s[i] = table.getColumnName(i);
        }
        table.setModel(new DefaultTableModel(s, 0));
    }

    private void initTableRows(JTable table, int rowCount) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < rowCount; i++) {
            model.insertRow(i, new Object[]{i});
        }
        table.setModel(model);
    }

    private JTable createTable(Object[] headers, List<Integer> editableColumns) {
        JTable table = new JTable(new DefaultTableModel(headers, 0)) {
            public boolean isCellEditable(int row, int column) {
                if (editableColumns != null) {
                    return editableColumns.contains(column);
                }
                return false;
            }
        };
        table.setCellSelectionEnabled(false);
        table.setFocusable(false);
        return table;
    }

    private JFreeChart createChart(String title, String xl, String yl, XYDataset ds) {
        return ChartFactory.createXYLineChart(title, xl, yl, ds, PlotOrientation.VERTICAL, true, true, true);
    }

    private void clearRowWithMove(JTable table, int rowIndex) {
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.setValueAt("", rowIndex, i);
        }
        for (int i = rowIndex; i < table.getRowCount() - 1; i++) {
            Object s = null;
            for (int j = 1; j < table.getColumnCount(); j++) {
                s = table.getValueAt(i + 1, j);
                table.setValueAt(s, i, j);
            }
            if (s == null) {
                break;
            }
        }
        for (int j = 1; j < table.getColumnCount(); j++) {
            table.setValueAt(null, table.getRowCount() - 1, j);
        }
    }

    private void addOrDeleteRows(JTable table, int size) {
        if (size > table.getRowCount()) {
            for (int i = table.getRowCount(); i < size; i++) {
                ((DefaultTableModel) table.getModel()).insertRow(i, new Object[]{String.valueOf(i), 1.0});
            }
        } else {
            for (int i = table.getRowCount() - 1; i >= size; i--) {
                ((DefaultTableModel) table.getModel()).removeRow(i);
            }
        }
    }

    private void setTableLambdas(JTable table, ArrayList<Double> lambdas) {
        for (int i = 0; i < lambdas.size(); i++) {
            table.setValueAt(lambdas.get(i), i, 1);
        }
    }

    private ArrayList<Double> getTableLambdas(JTable table) {
        ArrayList<Double> lambdas = new ArrayList<>();
        for (int i = 0; i < table.getRowCount(); i++) {
            lambdas.add(Double.valueOf(String.valueOf(table.getValueAt(i, 1))));
        }
        return lambdas;
    }

    private void analyze(int var, JFreeChart[] charts, int from, int to, double val, int count, boolean debug,
                         JButton[] b) {
        for (JFreeChart chart : charts) {
            chart.getXYPlot().setDataset(null);
        }

        Thread thread = new Thread(() -> {
            ArrayList<SimulatorThread> buffer = new ArrayList<>();
            SimulationConfig.ConfigJSON config = SimulationConfig.readJSON(debug ? "src/main/resources/config.json" : "config.json");
            String name = "";
            switch (var) {
                case 0 -> name = "Source[" + from + ":" + to + "], lambda=" + val;
                case 1 -> name = "Processor[" + from + ":" + to + "], lambda=" + val;
                case 2 -> name = "BufferCapacity[" + from + ":" + to + "]";
            }
            XYSeries series0 = new XYSeries(name);
            XYSeries series1 = new XYSeries(name);
            XYSeries series2 = new XYSeries(name);

            for (int i = from; i <= to; i++) {
                if (simToAnalyze == null) {
                    return;
                }
                ArrayList<Double> source;
                ArrayList<Double> processor;
                int bufferCapacity;
                switch (var) {
                    case 0 -> {
                        source = new ArrayList<>();
                        for (int j = 0; j < i; j++) {
                            source.add(val);
                        }
                        processor = new ArrayList<>(config.getProcessors());
                        bufferCapacity = config.getBufferCapacity();
                    }
                    case 1 -> {
                        source = new ArrayList<>(config.getSources());
                        processor = new ArrayList<>();
                        for (int j = 0; j < i; j++) {
                            processor.add(val);
                        }
                        bufferCapacity = config.getBufferCapacity();
                    }
                    default -> {
                        source = new ArrayList<>(config.getSources());
                        processor = new ArrayList<>(config.getProcessors());
                        bufferCapacity = i;
                    }
                }
                SimulationConfig.ConfigJSON configJSON = new SimulationConfig.ConfigJSON(
                        source, processor, bufferCapacity, config.getRequestsCount()
                );
                Simulator tmpSimulator = new Simulator(new SimulationConfig(configJSON));
                buffer.add(new SimulatorThread(tmpSimulator, true));
                if (i >= to || buffer.size() >= count) {
                    if (simToAnalyze == null) return;
                    simToAnalyze.addAll(buffer);
                    for (SimulatorThread simulator : buffer) {
                        simulator.start();
                    }
                    int ind = 1;
                    for (SimulatorThread simulatorThread : buffer) {
                        try {
                            simulatorThread.join();
                        } catch (Exception e) {
                            return;
                        }
                        Simulator simulator = simulatorThread.getSimulator();
                        if (simToAnalyze == null) return;
                        int index = i - buffer.size() + ind;
                        series0.add(index, ((double) simulator.getProductionManager().getFullRejectCount() /
                                (double) config.getRequestsCount()));
                        double time = 0;
                        for (ArrayList<Request> requests : simulator.getSelectionManager().getSuccessRequests()) {
                            time += requests.stream().mapToDouble(Request::getLifeTime).sum();
                        }
                        series1.add(index, time / simulator.getSelectionManager().getFullSuccessCount());
                        time = 0;
                        for (Processor p : simulator.getSelectionManager().getProcessors()) {
                            time += p.getWorkTime() / simulator.getEndTime();
                        }
                        series2.add(index, time / simulator.getSelectionManager().getProcessors().size());
                        ind++;
                    }
                    charts[0].getXYPlot().setDataset(new XYSeriesCollection(series0));
                    charts[1].getXYPlot().setDataset(new XYSeriesCollection(series1));
                    charts[2].getXYPlot().setDataset(new XYSeriesCollection(series2));
                    buffer.clear();
                    simToAnalyze.clear();
                }
            }
            b[1].setEnabled(true);
            b[0].setEnabled(false);
            simToAnalyze.clear();
            simToAnalyze = null;
        });
        thread.start();
    }

    public static void main(String[] args) {
        new MainGUI(true);
    }
}

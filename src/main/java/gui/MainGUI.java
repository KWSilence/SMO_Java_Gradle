package gui;

import com.google.gson.Gson;
import configs.SimulationConfig;
import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import smo_system.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class MainGUI
{
  private final NumberFormat formatter = new DecimalFormat("#0.000");
  private Thread lineMover = null;
  private Runnable task = null;
  private ArrayList<Simulator> simToAnalyze = new ArrayList<>();
  private final HashMap<String, Simulator> simulators = new HashMap<>();
  private boolean skipState = false;
  private final int threadPass = 5;

  MainGUI(boolean debug)
  {
    File configFile = new File(debug ? "src/main/resources/config.json" : "config.json");
    if (!configFile.exists())
    {
      try
      {
        PrintWriter writer = new PrintWriter(configFile, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        writer.print(gson.toJson(new SimulationConfig.ConfigJSON(new ArrayList<>(Arrays.asList(1.0, 1.0, 1.0)),
                                                                 new ArrayList<>(Arrays.asList(1.0, 1.0)), 3, 1000)));
        writer.close();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }

    simulators.put("steps", null);
    simulators.put("auto", null);

    JFrame frame = new JFrame("test");
    frame.setLayout(new MigLayout("", "[fill, grow]", "[fill, grow]"));

    JTabbedPane tabbedPane = new JTabbedPane();

    JPanel first = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow][fill, grow][]"));
    JTable tf1 = createTable(
      new String[]{"Source", "RequestsGen", "RejectProb", "StayTime", "WaitingTime", "ProcTime", "DisWaitingTime",
                   "DisProcTime"}, null);
    first.add(new JScrollPane(tf1), "wrap, span, grow");
    JTable tf2 = createTable(new String[]{"Processor", "UsageRate"}, null);
    first.add(new JScrollPane(tf2), "wrap, span");
    JProgressBar pbf1 = new JProgressBar();
    pbf1.setMinimum(0);
    first.add(pbf1);
    JButton bf1 = new JButton("Stop");
    bf1.setEnabled(false);
    first.add(bf1, "split 4");
    JButton bf2 = new JButton("Start Auto");
    first.add(bf2);
    JCheckBox cbf1 = new JCheckBox("Use N0");
    first.add(cbf1);
    JTextField tff1 = new JTextField("100");
    tff1.setEnabled(false);
    first.add(tff1);
    cbf1.addActionListener(e -> tff1.setEnabled(cbf1.isSelected()));
    bf2.addActionListener(e -> {
      SimulationConfig simulationConfig = debug ? new SimulationConfig("src/main/resources/config.json")
                                                : new SimulationConfig("config.json");

      clearTable(tf1);
      initTableRows(tf1, simulationConfig.getSources().size());
      clearTable(tf2);
      initTableRows(tf2, simulationConfig.getProcessors().size());

      final int N0;

      if (cbf1.isSelected())
      {
        N0 = Integer.parseInt(tff1.getText());
        pbf1.setMaximum(N0);
      }
      else
      {
        N0 = 0;
        simulators.put("auto", new Simulator(simulationConfig));
        Simulator simulator = simulators.get("auto");
        simulator.setUseSteps(false);
        pbf1.setMaximum(simulationConfig.getProductionManager().getMaxRequestCount());
      }

      pbf1.setValue(0);

      bf2.setEnabled(false);
      bf1.setEnabled(true);

      lineMover = new Thread(() -> {
        if (cbf1.isSelected())
        {
          Analyzer.RequestCountAnalyzer analyzer = new Analyzer.RequestCountAnalyzer(N0, debug);
          try
          {
            analyzer.start();
            analyzer.join();
          }
          catch (Exception exception)
          {
            analyzer.interrupt();
            return;
          }
          pbf1.setValue(pbf1.getMaximum());
          simulators.put("auto", analyzer.getLastSimulator());
        }
        else
        {
          Simulator simulator = simulators.get("auto");
          simulator.start();
          while (true)
          {
            if (simulator.isInterrupted())
            {
              return;
            }
            if (!simulator.isAlive())
            {
              break;
            }
            pbf1.setValue(simulator.getProgress());
          }
        }

        Analyzer analyzer = new Analyzer(simulators.get("auto"));
        analyzer.analyze(true);
        ArrayList<ArrayList<String>> sr = analyzer.getSourceResults();
        ArrayList<ArrayList<String>> pr = analyzer.getProcessorResults();
        clearTable(tf1);
        initTableRows(tf1, sr.size());
        clearTable(tf2);
        initTableRows(tf2, pr.size());

        for (int i = 0; i < sr.size(); i++)
        {
          ArrayList<String> el = sr.get(i);
          for (int j = 1; j < el.size(); j++)
          {
            tf1.setValueAt(el.get(j), i, j);
          }
        }

        for (int i = 0; i < pr.size(); i++)
        {
          ArrayList<String> el = pr.get(i);
          for (int j = 1; j < el.size(); j++)
          {
            tf2.setValueAt(el.get(j), i, j);
          }
        }

        simulators.put("auto", null);
        bf2.setEnabled(true);
        bf1.setEnabled(false);
      });

      lineMover.start();
    });
    bf1.addActionListener(e -> {
      lineMover.interrupt();
      Simulator simulator = simulators.get("auto");
      if (simulator != null)
      {
        simulator.interrupt();
        simulators.put("auto", null);
      }
      bf2.setEnabled(true);
      bf1.setEnabled(false);
    });
    tabbedPane.addTab("auto", first);


    JPanel second = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow][fill, grow][]"));
    JTable ts1 = createTable(new String[]{"Source", "Request", "GenerateTime"}, null);
    second.add(new JScrollPane(ts1));
    JTable ts2 = createTable(new String[]{"Buffer", "Request", "TakeTime"}, null);
    second.add(new JScrollPane(ts2), "wrap");
    JTable ts3 = createTable(new String[]{"Processor", "Request", "TakeTime", "ReleaseTime"}, null);
    second.add(new JScrollPane(ts3));
    JTextArea tps1 = new JTextArea();
    tps1.setEditable(false);
    JScrollPane sps1 = new JScrollPane(tps1);
    second.add(sps1, "wrap");
    JProgressBar pbs1 = new JProgressBar();
    pbs1.setMinimum(0);
    second.add(pbs1);
    JButton bs1 = new JButton("Stop");
    bs1.setEnabled(false);
    second.add(bs1, "split 4");
    JButton bs2 = new JButton("Start Steps");
    second.add(bs2);
    JButton bs3 = new JButton("Skip");
    second.add(bs3);
    JCheckBox cbs1 = new JCheckBox("textAutoScroll");
    second.add(cbs1);
    sps1.getVerticalScrollBar().addAdjustmentListener(e -> e.getAdjustable().setValue(
      cbs1.isSelected() ? e.getAdjustable().getMaximum() : e.getAdjustable().getValue()));
    bs3.setEnabled(false);
    bs2.addActionListener(e -> {
      Simulator simulator = simulators.get("steps");
      if (simulator == null)
      {
        SimulationConfig simulationConfig = debug ? new SimulationConfig("src/main/resources/config.json")
                                                  : new SimulationConfig("config.json");

        clearTable(ts1);
        initTableRows(ts1, simulationConfig.getSources().size());
        clearTable(ts2);
        initTableRows(ts2, simulationConfig.getBuffer().getCapacity());
        clearTable(ts3);
        initTableRows(ts3, simulationConfig.getProcessors().size());

        tps1.setText("");

        pbs1.setMaximum(simulationConfig.getProductionManager().getMaxRequestCount());
        pbs1.setValue(0);

        simulators.put("steps", new Simulator(simulationConfig));
        simulator = simulators.get("steps");
        simulator.setUseSteps(true);
        simulator.start();

        bs2.setText("Next Step");
        bs1.setEnabled(true);
        bs3.setEnabled(true);

        Simulator finalSimulator = simulator;
        task = () -> {
          try
          {
            do
            {
              if (finalSimulator.isInterrupted())
              {
                return;
              }
              synchronized (finalSimulator)
              {
                finalSimulator.notify();
                finalSimulator.wait();
              }
              SimulatorEvent event = finalSimulator.getLastEvent();
              switch (event.getType())
              {
                case GENERATE -> {
                  Request request = event.getRequest();
                  if (!skipState)
                  {
                    ts1.setValueAt(request.getSourceNumber() + "." + request.getNumber(), request.getSourceNumber(), 1);
                    ts1.setValueAt(formatter.format(request.getTime()), request.getSourceNumber(), 2);
                  }
                  tps1.append(event.getLog());
                }

                case TAKE -> {
                  Request request = event.getRequest();
                  Processor processor = event.getProcessor();
                  Buffer buffer = event.getBuffer();
                  if (!skipState)
                  {
                    if (buffer != null)
                    {
                      clearRowWithMove(ts2, event.getBuffer().getTakeIndex());
                    }
                    else
                    {
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
                  if (!skipState)
                  {
                    ts1.setValueAt(null, request.getSourceNumber(), 1);
                    ts1.setValueAt(null, request.getSourceNumber(), 2);
                    ts2.setValueAt(request.getSourceNumber() + "." + request.getNumber(), row, 1);
                    ts2.setValueAt(formatter.format(request.getTime()), row, 2);
                  }
                  tps1.append(event.getLog());
                }

                case REJECT -> {
                  Request request = event.getRequest();
                  if (!skipState)
                  {
                    ts1.setValueAt(null, request.getSourceNumber(), 1);
                    ts1.setValueAt(null, request.getSourceNumber(), 2);
                  }
                  tps1.append(event.getLog());
                }

                case RELEASE -> {
                  Processor processor = event.getProcessor();
                  if (!skipState)
                  {
                    ts3.setValueAt(formatter.format(processor.getProcessTime()), processor.getNumber(), 3);
                  }
                  tps1.append(event.getLog());
                }

                case WORK_END -> {
                  tps1.append(event.getLog());
                  bs2.setText("Show Result Table");
                  bs2.setEnabled(true);
                  if (skipState)
                  {
                    bs3.setEnabled(false);
                    skipState = false;
                    return;
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

                  for (int i = 0; i < sr.size(); i++)
                  {
                    ArrayList<String> el = sr.get(i);
                    for (int j = 1; j < el.size(); j++)
                    {
                      tf1.setValueAt(el.get(j), i, j);
                    }
                  }

                  for (int i = 0; i < pr.size(); i++)
                  {
                    ArrayList<String> el = pr.get(i);
                    for (int j = 1; j < el.size(); j++)
                    {
                      tf2.setValueAt(el.get(j), i, j);
                    }
                  }

                  tabbedPane.setSelectedIndex(0);
                  bs2.setEnabled(false);
                }
              }
              pbs1.setValue(finalSimulator.getProgress());
            } while (skipState);
          }
          catch (Exception ex)
          {
            ex.printStackTrace();
          }
        };
      }
      else
      {
        if (skipState)
        {
          new Thread(task).start();
        }
        else
        {
          task.run();
        }
      }
    });
    bs1.addActionListener(e -> {
      simulators.get("steps").interrupt();
      simulators.put("steps", null);
      bs2.setText("Start Steps");
      bs2.setEnabled(true);
      bs1.setEnabled(false);
      bs3.setEnabled(false);
      skipState = false;
    });
    bs3.addActionListener(e -> {
      bs3.setEnabled(false);
      skipState = true;
      bs2.getActionListeners()[0].actionPerformed(e);
      bs2.setEnabled(false);
    });
    tabbedPane.addTab("step", second);

    //TODO 4 step
    JPanel third = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow][]"));
    //[COM] Charts
    JFreeChart chart1 = createChart("RejectProbability", "Count", "Probability", null);
    third.add(new ChartPanel(chart1));
    JFreeChart chart2 = createChart("LifeTime", "Count", "Time", null);
    third.add(new ChartPanel(chart2));
    JFreeChart chart3 = createChart("ProcessorsUsingRate", "Count", "Rate", null);
    third.add(new ChartPanel(chart3), "wrap");
    //[COM] input places
    JComboBox<String> comboBox = new JComboBox<>(new String[]{"Source", "Processor", "Buffer"});
    third.add(comboBox);
    third.add(new JLabel("From"), "split 6");
    JTextField tft1 = new JTextField("10");
    third.add(tft1);
    third.add(new JLabel("To"));
    JTextField tft2 = new JTextField("100");
    third.add(tft2);
    third.add(new JLabel("Value"));
    JTextField tft3 = new JTextField("1.0");
    third.add(tft3);
    comboBox.addActionListener(e -> tft3.setEnabled(comboBox.getSelectedIndex() != 2));
    JButton bt1 = new JButton("Stop");
    bt1.setEnabled(false);
    third.add(bt1, "split 2");
    JButton bt2 = new JButton("Launch");
    third.add(bt2);
    //TODO progress
    //[COM] Stop button
    bt1.addActionListener(e -> {
      bt2.setEnabled(true);
      bt1.setEnabled(false);
      for (int i = simToAnalyze.size() - threadPass; i < simToAnalyze.size(); i++)
      {
        simToAnalyze.get(i).interrupt();
      }
      simToAnalyze.clear();
      simToAnalyze = null;
    });
    //[COM] Launch button
    bt2.addActionListener(e -> {
      simToAnalyze = new ArrayList<>();
      bt2.setEnabled(false);
      bt1.setEnabled(true);
      analyze(comboBox.getSelectedIndex(), new JFreeChart[]{chart1, chart2, chart3}, Integer.parseInt(tft1.getText()), Integer.parseInt(tft2.getText()), Double.parseDouble(tft3.getText()), threadPass, debug);
    });
    tabbedPane.addTab("analyze", third);


    JPanel fourth = new JPanel(new MigLayout("", "[grow,fill]", "[grow,fill][]"));
    SimulationConfig.ConfigJSON config = SimulationConfig
      .readJSON(debug ? "src/main/resources/config.json" : "config.json");
    //[COM] JSON tables
    JTable th1 = createTable(new String[]{"SourceNumber", "Lambda"}, Collections.singletonList(1));
    initTableRows(th1, config.getSources().size());
    setTableLambdas(th1, config.getSources());
    JTable th2 = createTable(new String[]{"ProcessorNumber", "Lambda"}, Collections.singletonList(1));
    initTableRows(th2, config.getProcessors().size());
    setTableLambdas(th2, config.getProcessors());
    fourth.add(new JScrollPane(th1));
    fourth.add(new JScrollPane(th2), "wrap");
    //[COM] set elements count
    JTextField tfh1 = new JTextField(String.valueOf(config.getSources().size()));
    fourth.add(tfh1, "split 2");
    JButton bh1 = new JButton("Set sources count");
    bh1.addActionListener(e -> addOrDeleteRows(th1, Integer.parseInt(tfh1.getText())));
    fourth.add(bh1);
    JTextField tfh2 = new JTextField(String.valueOf(config.getProcessors().size()));
    fourth.add(tfh2, "split 2");
    JButton bh2 = new JButton("Set processors count");
    bh2.addActionListener(e -> addOrDeleteRows(th2, Integer.parseInt(tfh2.getText())));
    fourth.add(bh2, "wrap");
    //[COM] set lambdas
    JTextField tfh11 = new JTextField("1.0");
    fourth.add(tfh11, "split 2");
    JButton bh11 = new JButton("Set sources lambdas");
    bh11.addActionListener(e -> {
      String val = tfh11.getText();
      for (int i = 0; i < th1.getRowCount(); i++)
      {
        th1.setValueAt(val, i, 1);
      }
    });
    fourth.add(bh11);
    JTextField tfh21 = new JTextField("1.0");
    fourth.add(tfh21, "split 2");
    JButton bh21 = new JButton("Set processors lambdas");
    bh21.addActionListener(e -> {
      String val = tfh21.getText();
      for (int i = 0; i < th2.getRowCount(); i++)
      {
        th1.setValueAt(val, i, 1);
      }
    });
    fourth.add(bh21, "wrap");
    //[COM] set BufferCapacity RequestCount
    fourth.add(new JLabel("Buffer Capacity"), "split 2");
    JTextField tfh3 = new JTextField(String.valueOf(config.getBufferCapacity()));
    fourth.add(tfh3);
    fourth.add(new JLabel("Requests Count"), "split 2");
    JTextField tfh4 = new JTextField(String.valueOf(config.getRequestsCount()));
    fourth.add(tfh4, "wrap");
    //[COM] Refresh Save JSON
    JButton bh3 = new JButton("Refresh");
    bh3.addActionListener(e -> {
      SimulationConfig.ConfigJSON configRefresh = SimulationConfig
        .readJSON(debug ? "src/main/resources/config.json" : "config.json");
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
    fourth.add(bh3);
    JButton bh4 = new JButton("Save");
    bh4.addActionListener(e -> {
      ArrayList<Double> sources = getTableLambdas(th1);
      ArrayList<Double> processors = getTableLambdas(th2);
      int bufferCapacity = Integer.parseInt(tfh3.getText());
      int requestsCount = Integer.parseInt(tfh4.getText());
      SimulationConfig.ConfigJSON configSave = new SimulationConfig.ConfigJSON(sources, processors, bufferCapacity,
                                                                               requestsCount);
      String fileName = debug ? "src/main/resources/config.json" : "config.json";
      try
      {
        PrintWriter writer = new PrintWriter(fileName, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        writer.print(gson.toJson(configSave));
        writer.close();
      }
      catch (Exception exception)
      {
        exception.printStackTrace();
      }
    });
    fourth.add(bh4);
    tabbedPane.addTab("settings", fourth);

    frame.add(tabbedPane);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    frame.setSize(1280, 720);
    frame.setVisible(true);
  }

  private void clearTable(JTable table)
  {
    String[] s = new String[table.getColumnCount()];
    for (int i = 0; i < table.getColumnCount(); i++)
    {
      s[i] = table.getColumnName(i);
    }
    table.setModel(new DefaultTableModel(s, 0));
  }

  private void initTableRows(JTable table, int rowCount)
  {
    DefaultTableModel model = (DefaultTableModel) table.getModel();
    for (int i = 0; i < rowCount; i++)
    {
      model.insertRow(i, new Object[]{i});
    }
    table.setModel(model);
  }

  private JTable createTable(Object[] headers, List<Integer> editableColumns)
  {
    JTable table = new JTable(new DefaultTableModel(headers, 0))
    {
      public boolean isCellEditable(int row, int column)
      {
        if (editableColumns != null)
        {
          return editableColumns.contains(column);
        }
        return false;
      }
    };
    table.setCellSelectionEnabled(false);
    table.setFocusable(false);
    return table;
  }

  private JFreeChart createChart(String title, String xl, String yl, XYDataset ds)
  {
    return ChartFactory.createXYLineChart(title, xl, yl, ds, PlotOrientation.VERTICAL, true, true, true);
  }

  private void clearRowWithMove(JTable table, int rowIndex)
  {
    for (int i = 1; i < table.getColumnCount(); i++)
    {
      table.setValueAt("", rowIndex, i);
    }
    for (int i = rowIndex; i < table.getRowCount() - 1; i++)
    {
      Object s = null;
      for (int j = 1; j < table.getColumnCount(); j++)
      {
        s = table.getValueAt(i + 1, j);
        table.setValueAt(s, i, j);
      }
      if (s == null)
      {
        break;
      }
    }
    for (int j = 1; j < table.getColumnCount(); j++)
    {
      table.setValueAt(null, table.getRowCount() - 1, j);
    }
  }

  private void addOrDeleteRows(JTable table, int size)
  {
    if (size > table.getRowCount())
    {
      for (int i = table.getRowCount(); i < size; i++)
      {
        ((DefaultTableModel) table.getModel()).insertRow(i, new Object[]{String.valueOf(i), 1.0});
      }
    }
    else
    {
      for (int i = table.getRowCount() - 1; i >= size; i--)
      {
        ((DefaultTableModel) table.getModel()).removeRow(i);
      }
    }
  }

  private void setTableLambdas(JTable table, ArrayList<Double> lambdas)
  {
    for (int i = 0; i < lambdas.size(); i++)
    {
      table.setValueAt(lambdas.get(i), i, 1);
    }
  }

  private ArrayList<Double> getTableLambdas(JTable table)
  {
    ArrayList<Double> lamdas = new ArrayList<>();
    for (int i = 0; i < table.getRowCount(); i++)
    {
      lamdas.add(Double.valueOf(String.valueOf(table.getValueAt(i, 1))));
    }
    return lamdas;
  }

  private void analyze(int var, JFreeChart[] charts, int from, int to, double val, int count, boolean debug)
  {
    for (JFreeChart chart : charts)
    {
      chart.getXYPlot().setDataset(null);
    }

    Thread thread = new Thread(() -> {
      ArrayList<Simulator> buffer = new ArrayList<>();
      SimulationConfig.ConfigJSON config = SimulationConfig
        .readJSON(debug ? "src/main/resources/config.json" : "config.json");
      String name = "";
      switch (var)
      {
        case 0 -> name = "Source";
        case 1 -> name = "Processor";
        case 2 -> name = "BufferCapacity";
      }
      XYSeries series0 = new XYSeries(name);
      XYSeries series1 = new XYSeries(name);
      XYSeries series2 = new XYSeries(name);

      for (int i = from; i <= to; i++)
      {
        if (simToAnalyze == null)
        {
          return;
        }
        ArrayList<Double> source;
        ArrayList<Double> processor;
        int bufferCapacity;
        switch (var)
        {
          case 0 -> {
            source = new ArrayList<>();
            for (int j = 0; j < i; j++)
            {
              source.add(val);
            }
            processor = new ArrayList<>(config.getProcessors());
            bufferCapacity = config.getBufferCapacity();
          }
          case 1 -> {
            source = new ArrayList<>(config.getSources());
            processor = new ArrayList<>();
            for (int j = 0; j < i; j++)
            {
              processor.add(val);
            }
            bufferCapacity = config.getBufferCapacity();
          }
          case 2 -> {
            source = new ArrayList<>(config.getSources());
            processor = new ArrayList<>(config.getProcessors());
            bufferCapacity = i;
          }
          default -> {
            source = new ArrayList<>(config.getSources());
            processor = new ArrayList<>(config.getProcessors());
            bufferCapacity = config.getBufferCapacity();
          }
        }
        buffer.add(new Simulator(new SimulationConfig(
          new SimulationConfig.ConfigJSON(source, processor, bufferCapacity, config.getRequestsCount()))));
        if (i >= to || buffer.size() >= count)
        {
          if (simToAnalyze == null)
          {
            return;
          }
          simToAnalyze.addAll(buffer);
          for (Simulator simulator : buffer)
          {
            simulator.start();
          }
          int ind = 0;
          for (Simulator simulator : buffer)
          {
            try
            {
              simulator.join();
            }
            catch (Exception e)
            {
              e.printStackTrace();
              return;
            }
            if (simToAnalyze == null)
            {
              return;
            }
            series0.add(i - count + ind, ((double) simulator.getProductionManager().getFullRejectCount() /
                                  (double) config.getRequestsCount()));
            double time = 0;
            for (ArrayList<Request> requests : simulator.getSelectionManager().getSuccessRequests())
            {
              time += requests.stream().mapToDouble(Request::getLifeTime).sum();
            }
            series1.add(i - count + ind, time / simulator.getSelectionManager().getFullSuccessCount());
            time = 0;
            for (Processor p : simulator.getSelectionManager().getProcessors())
            {
              time += p.getWorkTime() / simulator.getEndTime();
            }
            series2.add(i - count + ind, time / simulator.getSelectionManager().getProcessors().size());
            ind++;
          }
          charts[0].getXYPlot().setDataset(new XYSeriesCollection(series0));
          charts[1].getXYPlot().setDataset(new XYSeriesCollection(series1));
          charts[2].getXYPlot().setDataset(new XYSeriesCollection(series2));
          buffer.clear();
        }
      }
    });
    thread.start();
  }

  public static void main(String[] args)
  {
    new MainGUI(true);
  }
}

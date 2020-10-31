package gui;

import com.google.gson.Gson;
import configs.SimulationConfig;
import net.miginfocom.swing.MigLayout;
import smo_system.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainGUI
{
  private final NumberFormat formatter = new DecimalFormat("#0.000");
  private Thread lineMover = null;
  private final HashMap<String, Simulator> simulators = new HashMap<>();
  private boolean skipState = false;

  MainGUI(boolean debug)
  {
    simulators.put("steps", null);
    simulators.put("auto", null);

    JFrame frame = new JFrame("test");
    frame.setLayout(new MigLayout("", "[fill, grow]", "[fill, grow]"));

    JTabbedPane tabbedPane = new JTabbedPane();

    JPanel first = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow]"));
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


    JPanel second = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow]"));
    JTable ts1 = createTable(new String[]{"Source", "Request", "GenerateTime"}, null);
    second.add(new JScrollPane(ts1));
    JTable ts2 = createTable(new String[]{"Buffer", "Request", "TakeTime"}, null);
    second.add(new JScrollPane(ts2), "wrap");
    JTable ts3 = createTable(new String[]{"Processor", "Request", "TakeTime", "ReleaseTime"}, null);
    second.add(new JScrollPane(ts3));
    JTextArea tps1 = new JTextArea();
    tps1.setEditable(false);
    second.add(new JScrollPane(tps1), "wrap");
    JProgressBar pbs1 = new JProgressBar();
    pbs1.setMinimum(0);
    second.add(pbs1);
    JButton bs1 = new JButton("Stop");
    bs1.setEnabled(false);
    second.add(bs1, "split 3");
    JButton bs2 = new JButton("Start Steps");
    second.add(bs2);
    JButton bs3 = new JButton("Skip");
    second.add(bs3);
    bs3.setEnabled(false);
    bs2.addActionListener(e -> {
      try
      {
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
        }
        else
        {
          synchronized (simulator)
          {
            simulator.notify();
            simulator.wait();
          }
          SimulatorEvent event = simulator.getLastEvent();
          switch (event.getType())
          {
            case GENERATE -> {
              Request request = event.getRequest();
              ts1.setValueAt(request.getSourceNumber() + "." + request.getNumber(), request.getSourceNumber(), 1);
              ts1.setValueAt(formatter.format(request.getTime()), request.getSourceNumber(), 2);
              tps1.append(event.getLog());
            }

            case TAKE -> {
              Request request = event.getRequest();
              Processor processor = event.getProcessor();
              Buffer buffer = event.getBuffer();
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
              ts3.setValueAt(formatter.format(request.getTime() + request.getTimeInBuffer()), processor.getNumber(), 2);
              ts3.setValueAt(null, processor.getNumber(), 3);
              tps1.append(event.getLog());
            }

            case BUFFER -> {
              Request request = event.getRequest();
              Buffer buffer = event.getBuffer();
              int row = buffer.getSize() - 1;
              ts1.setValueAt(null, request.getSourceNumber(), 1);
              ts1.setValueAt(null, request.getSourceNumber(), 2);
              ts2.setValueAt(request.getSourceNumber() + "." + request.getNumber(), row, 1);
              ts2.setValueAt(formatter.format(request.getTime()), row, 2);
              tps1.append(event.getLog());
            }

            case REJECT -> {
              Request request = event.getRequest();
              tps1.append(event.getLog());
              ts1.setValueAt(null, request.getSourceNumber(), 1);
              ts1.setValueAt(null, request.getSourceNumber(), 2);
            }

            case RELEASE -> {
              Processor processor = event.getProcessor();
              ts3.setValueAt(formatter.format(processor.getProcessTime()), processor.getNumber(), 3);
              tps1.append(event.getLog());
            }

            case WORK_END -> {
              tps1.append(event.getLog());
              bs2.setText("Show Result Table");
              if (skipState)
              {
                bs3.setEnabled(false);
                skipState = false;
                return;
              }
            }

            case ANALYZE -> {
              Analyzer analyzer = new Analyzer(simulator);
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
          pbs1.setValue(simulator.getProgress());

          if (skipState)
          {
            bs2.getActionListeners()[0].actionPerformed(e);
          }
        }
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    });
    bs1.addActionListener(e -> {
      simulators.get("steps").interrupt();
      simulators.put("steps", null);
      bs2.setText("Start Steps");
      bs2.setEnabled(true);
      bs1.setEnabled(false);
      bs3.setEnabled(false);
    });
    bs3.addActionListener(e -> {
      bs3.setEnabled(false);
      skipState = true;
      bs2.getActionListeners()[0].actionPerformed(e);
    });
    tabbedPane.addTab("step", second);

      //TODO 4 step
//    JPanel third = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow]"));
//    XYSeries series = new XYSeries("sin(a)");
//    for(float i = 0; i <= Math.PI; i+=0.01){
//      series.add(i, Math.sin(i));
//    }
//    XYDataset xyDataset = new XYSeriesCollection(series);
//    JFreeChart chart = ChartFactory
//      .createXYLineChart("y = sin(x)", "x", "y",
//                         xyDataset,
//                         PlotOrientation.VERTICAL,
//                         true, true, true);
//    third.add(new ChartPanel(chart), "wrap");
//    third.add(new JButton("Ok"));
//    tabbedPane.addTab("test", third);


    JPanel fourth = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow]"));
    SimulationConfig.ConfigJSON config = SimulationConfig
      .readJSON(debug ? "src/main/resources/config.json" : "config.json");
    JTable tfh1 = createTable(new String[]{"SourceNumber", "Lambda"}, Collections.singletonList(1));
    initTableRows(tfh1, config.getSources().size());
    setTableLambdas(tfh1, config.getSources());
    JTable tfh2 = createTable(new String[]{"ProcessorNumber", "Lambda"}, Collections.singletonList(1));
    initTableRows(tfh2, config.getProcessors().size());
    setTableLambdas(tfh2, config.getProcessors());
    fourth.add(new JScrollPane(tfh1));
    fourth.add(new JScrollPane(tfh2), "wrap");
    JTextField tffh1 = new JTextField(String.valueOf(config.getSources().size()));
    fourth.add(tffh1, "split 2");
    JButton bfh1 = new JButton("Set sources count");
    bfh1.addActionListener(e -> addOrDeleteRows(tfh1, Integer.parseInt(tffh1.getText())));
    fourth.add(bfh1);
    JTextField tffh2 = new JTextField(String.valueOf(config.getProcessors().size()));
    fourth.add(tffh2, "split 2");
    JButton bfh2 = new JButton("Set processors count");
    bfh2.addActionListener(e -> addOrDeleteRows(tfh2, Integer.parseInt(tffh2.getText())));
    fourth.add(bfh2, "wrap");
    JLabel lfh1 = new JLabel("Buffer Capacity");
    fourth.add(lfh1, "split 2");
    JTextField tffh3 = new JTextField(String.valueOf(config.getBufferCapacity()));
    fourth.add(tffh3);
    JLabel lfh2 = new JLabel("Requests Count");
    fourth.add(lfh2, "split 2");
    JTextField tffh4 = new JTextField(String.valueOf(config.getRequestsCount()));
    fourth.add(tffh4, "wrap");
    JButton bfh3 = new JButton("Refresh");
    bfh3.addActionListener(e -> {
      SimulationConfig.ConfigJSON configRefresh = SimulationConfig
        .readJSON(debug ? "src/main/resources/config.json" : "config.json");
      clearTable(tfh1);
      initTableRows(tfh1, configRefresh.getSources().size());
      setTableLambdas(tfh1, configRefresh.getSources());
      clearTable(tfh2);
      initTableRows(tfh2, configRefresh.getProcessors().size());
      setTableLambdas(tfh2, configRefresh.getProcessors());
      tffh1.setText(String.valueOf(configRefresh.getSources().size()));
      tffh2.setText(String.valueOf(configRefresh.getProcessors().size()));
      tffh3.setText(String.valueOf(configRefresh.getBufferCapacity()));
      tffh4.setText(String.valueOf(configRefresh.getRequestsCount()));
    });
    fourth.add(bfh3);
    JButton bfh4 = new JButton("Save");
    bfh4.addActionListener(e -> {
      ArrayList<Double> sources = getTableLambdas(tfh1);
      ArrayList<Double> processors = getTableLambdas(tfh2);
      int bufferCapacity = Integer.parseInt(tffh3.getText());
      int requestsCount = Integer.parseInt(tffh4.getText());
      SimulationConfig.ConfigJSON configSave = new SimulationConfig.ConfigJSON(sources, processors, bufferCapacity, requestsCount);
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
    fourth.add(bfh4);
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
        ((DefaultTableModel)table.getModel()).removeRow(i);
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

  public static void main(String[] args)
  {
    new MainGUI(true);
  }
}

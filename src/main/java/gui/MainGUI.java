package gui;

import configs.SimulationConfig;
import net.miginfocom.swing.MigLayout;
import smo_system.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;

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
    JTable tf1 = createNonEditTable(
      new String[]{"Source", "RequestsGen", "RejectProb", "StayTime", "WaitingTime", "ProcTime", "DisWaitingTime",
                   "DisProcTime"});
    first.add(new JScrollPane(tf1), "wrap, span, grow");
    JTable tf2 = createNonEditTable(new String[]{"Processor", "UsageRate"});
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
    tabbedPane.addTab("auto", first);

    JPanel second = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow]"));
    JTable ts1 = createNonEditTable(new String[]{"Source", "Request", "GenerateTime"});
    second.add(new JScrollPane(ts1));
    JTable ts2 = createNonEditTable(new String[]{"Buffer", "Request", "TakeTime"});
    second.add(new JScrollPane(ts2), "wrap");
    JTable ts3 = createNonEditTable(new String[]{"Processor", "Request", "TakeTime", "ReleaseTime"});
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
    tabbedPane.addTab("step", second);

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

  private JTable createNonEditTable(Object[] headers)
  {
    JTable table = new JTable(new DefaultTableModel(headers, 0))
    {
      public boolean isCellEditable(int row, int column)
      {
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

  public static void main(String[] args)
  {
    new MainGUI(true);
  }
}

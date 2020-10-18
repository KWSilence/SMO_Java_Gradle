package gui;

import configs.SimulationConfig;
import net.miginfocom.swing.MigLayout;
import smo_system.Buffer;
import smo_system.Processor;
import smo_system.Request;
import smo_system.Simulator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;

public class MainGUI
{
  private final NumberFormat formatter = new DecimalFormat("#0.000");

  MainGUI()
  {
    final HashMap<String, Simulator> simulators = new HashMap<>();

    simulators.put("steps", null);
    simulators.put("auto", null);

    JFrame frame = new JFrame("test");
    frame.setLayout(new MigLayout("", "[fill, grow]", "[fill, grow]"));

    JTabbedPane tabbedPane = new JTabbedPane();

    JPanel first = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow]"));
    JTable tf1 = createNonEditTable(
      new String[]{"Source", "RequestsGen", "RejectProb", "StayTime", "WaitingTime", "ProcTime", "DisWaitingTime",
                   "DisProcTime"}, 0);
    first.add(new JScrollPane(tf1), "wrap, span, grow");
    JTable tf2 = createNonEditTable(new String[]{"Processor", "WorkTime"}, 0);
    first.add(new JScrollPane(tf2), "wrap, span");
    JProgressBar pbf1 = new JProgressBar();
    pbf1.setMinimum(0);
    first.add(pbf1);
    JButton bf1 = new JButton("Stop");
    bf1.setEnabled(false);
    first.add(bf1, "split 2");
    JButton bf2 = new JButton("Start Auto");
    first.add(bf2);
    tabbedPane.addTab("auto", first);

    JPanel second = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow]"));
    JTable ts1 = createNonEditTable(new String[]{"Source", "Request", "GenerateTime"}, 0);
    second.add(new JScrollPane(ts1));
    JTable ts2 = createNonEditTable(new String[]{"Buffer", "Request", "TakeTime"}, 0);
    second.add(new JScrollPane(ts2), "wrap");
    JTable ts3 = createNonEditTable(new String[]{"Processor", "Request", "TakeTime", "ReleaseTime"}, 0);
    second.add(new JScrollPane(ts3));
    JTextArea tps1 = new JTextArea();
    tps1.setEditable(false);
    second.add(new JScrollPane(tps1), "wrap");
    JProgressBar pbs1 = new JProgressBar();
    pbs1.setMinimum(0);
    second.add(pbs1);
    JButton bs1 = new JButton("Stop");
    bs1.setEnabled(false);
    second.add(bs1, "split 2");
    JButton bs2 = new JButton("Start Steps");
    second.add(bs2);
    tabbedPane.addTab("step", second);


    bs2.addActionListener(e -> {
      try
      {
        Simulator simulator = simulators.get("steps");
        if (simulator == null)
        {
//          SimulationConfig simulationConfig = new SimulationConfig("config.json"); //Release
          SimulationConfig simulationConfig = new SimulationConfig("src/main/resources/config.json");

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
            }

            case ANALYZE -> {
              //TODO (move to auto page|create new tmp page), analyze
            }
          }
          pbs1.setValue(simulator.getProgress());
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
      bs1.setEnabled(false);
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

  private JTable createNonEditTable(Object[] headers, int rowCount)
  {
    JTable table = new JTable(new DefaultTableModel(headers, rowCount))
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
    MainGUI mainGUI = new MainGUI();
  }
}

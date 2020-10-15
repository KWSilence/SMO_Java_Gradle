package gui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.HashMap;

public class MainGUI
{
//  private final Analyzer analyzer;

  private final JFrame frame;

  private final JTabbedPane tabbedPane;

  //  private final HashMap<String, JPanel> panels;
  private final HashMap<String, JTable> tables;
  private final HashMap<String, JProgressBar> progressBars;
  //  private final HashMap<String, JButton> buttons;
  private final HashMap<String, JTextPane> textPanes;

  MainGUI()
  {
//    panels = new HashMap<>();
    tables = new HashMap<>();
    progressBars = new HashMap<>();
//    buttons = new HashMap<>();
    textPanes = new HashMap<>();

    frame = new JFrame("test");
    frame.setLayout(new MigLayout("", "[fill, grow]", "[fill, grow]"));

    tabbedPane = new JTabbedPane();

    JPanel first = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow]"));
    JTable tf1 = createNonEditTable(
      new String[]{"Source", "RequestsGen", "RejectProb", "StayTime", "WaitingTime", "ProcTime", "DispWaitingTime",
                   "DispProcTime"}, 10);
    first.add(new JScrollPane(tf1), "wrap, span, grow");
    JTable tf2 = createNonEditTable(new String[]{"Processor", "WorkTime"}, 10);
    first.add(new JScrollPane(tf2), "wrap, span");
    JProgressBar pbf1 = new JProgressBar();
    first.add(pbf1);
    JButton bf1 = new JButton("Stop");
    first.add(bf1, "split 2");
    JButton bf2 = new JButton("Start Auto");
    first.add(bf2);
    tabbedPane.addTab("auto", first);

    JPanel second = new JPanel(new MigLayout("", "[fill, grow]", "[fill, grow]"));
    JTable ts1 = createNonEditTable(new String[]{"Source", "Request", "Time"}, 10);
    second.add(new JScrollPane(ts1));
    JTable ts2 = createNonEditTable(new String[]{"Buffer", "Request"}, 10);
    second.add(new JScrollPane(ts2), "wrap");
    JTable ts3 = createNonEditTable(new String[]{"Processor", "Request", "Time"}, 10);
    second.add(new JScrollPane(ts3));
    JTextPane tps1 = new JTextPane();
    tps1.setEditable(false);
    second.add(new JScrollPane(tps1), "wrap");
    JProgressBar pbs1 = new JProgressBar();
    second.add(pbs1);
    JButton bs1 = new JButton("Stop");
    second.add(bs1, "split 2");
    JButton bs2 = new JButton("Start Steps");
    second.add(bs2);
    tabbedPane.addTab("step", second);

//    panels.put("first", first);
//    panels.put("second", second);
    tables.put("tf1", tf1);
    tables.put("tf2", tf2);
    tables.put("ts1", ts1);
    tables.put("ts2", ts2);
    tables.put("ts3", ts3);
    progressBars.put("pf1", pbf1);
    progressBars.put("ps1", pbs1);
    textPanes.put("ts1", tps1);
//    buttons.put("bf1", bf1);
//    buttons.put("bf2", bf2);
//    buttons.put("bs1", bs1);
//    buttons.put("bs2", bs2);

    frame.add(tabbedPane);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    frame.setSize(1280, 720);
    frame.setVisible(true);
  }

  public HashMap<String, JTable> getTables()
  {
    return tables;
  }

  public HashMap<String, JProgressBar> getProgressBars()
  {
    return progressBars;
  }

  public HashMap<String, JTextPane> getTextPanes()
  {
    return textPanes;
  }

  private JTable createNonEditTable(Object[] headers, int rowCount)
  {
    return new JTable(new DefaultTableModel(headers, rowCount))
    {
      public boolean isCellEditable(int row, int column)
      {
        return false;
      }
    };
  }

  public static void main(String[] args)
  {
    MainGUI mainGUI = new MainGUI();
  }
}

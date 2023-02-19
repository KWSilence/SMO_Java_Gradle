package gui;

import configs.SimulationConfig;
import gui.tab.AnalyzeTab;
import gui.tab.AutoTab;
import gui.tab.SettingsTab;
import gui.tab.StepTab;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class MainGUI {
    MainGUI(boolean debug) {
        SimulationConfig.initDefaultConfigFile(debug);

        JFrame frame = new JFrame("test");
        frame.setLayout(new MigLayout("", "[fill, grow]", "[fill, grow]"));

        JTabbedPane tabbedPane = new JTabbedPane();

        AutoTab autoTab = new AutoTab(
                new MigLayout("", "[fill, grow]", "[fill, grow][fill, grow][]"),
                debug
        );
        tabbedPane.addTab("auto", autoTab.getRoot());

        StepTab stepTab = new StepTab(
                new MigLayout("", "[fill, grow]", "[fill, grow][fill, grow][]"),
                debug,
                analyzer -> {
                    autoTab.setResults(analyzer, AutoTab.ResultType.SOURCES);
                    autoTab.setResults(analyzer, AutoTab.ResultType.PROCESSORS);
                    tabbedPane.setSelectedIndex(0);
                }
        );
        tabbedPane.addTab("step", stepTab.getRoot());

        AnalyzeTab analyzeTab = new AnalyzeTab(
                new MigLayout("", "[fill, grow]", "[fill, grow][]"),
                debug
        );
        tabbedPane.addTab("analyze", analyzeTab.getRoot());

        SettingsTab settingsTab = new SettingsTab(
                new MigLayout("", "[grow,fill]", "[grow,fill][]"),
                debug
        );
        tabbedPane.addTab("settings", settingsTab.getRoot());

        frame.add(tabbedPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setSize(1280, 720);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new MainGUI(true);
    }
}

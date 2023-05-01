package gui;

import configs.SimulationConfig;
import gui.tab.AnalyzeTab;
import gui.tab.AutoTab;
import gui.tab.SettingsTab;
import gui.tab.StepTab;
import net.miginfocom.swing.MigLayout;
import gui.ComponentHelper.MainHelper;

import javax.swing.*;
import java.io.File;

public class MainGUI {
    public static String getDefaultConfigPath(boolean debug) {
        return debug ? "src/main/resources/config.json" : "config.json";
    }

    public static SimulationConfig useDefaultConfigFile(boolean debug) {
        return new SimulationConfig(SimulationConfig.readJSON(getDefaultConfigPath(debug)));
    }

    private final JFrame root;

    public MainGUI(boolean debug) {
        File defaultConfigFile = new File(getDefaultConfigPath(debug));
        if (!defaultConfigFile.exists()) {
            SimulationConfig.saveConfigFile(defaultConfigFile, SimulationConfig.ConfigJSON.getDefaultConfig());
        }

        root = new JFrame(MainHelper.title);
        root.setName(MainHelper.root);
        root.setLayout(new MigLayout("", "[fill, grow]", "[fill, grow]"));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setName(MainHelper.tabbedPane);

        AutoTab autoTab = new AutoTab(
                new MigLayout("", "[fill, grow]", "[fill, grow][fill, grow][]"),
                debug
        );
        tabbedPane.addTab(MainHelper.autoTabTitle, autoTab.getRoot());

        StepTab stepTab = new StepTab(
                new MigLayout("", "[fill, grow]", "[fill, grow][fill, grow][]"),
                debug,
                analyzer -> {
                    autoTab.setResults(analyzer, AutoTab.ResultType.SOURCES);
                    autoTab.setResults(analyzer, AutoTab.ResultType.PROCESSORS);
                    tabbedPane.setSelectedIndex(0);
                }
        );
        tabbedPane.addTab(MainHelper.stepTabTitle, stepTab.getRoot());

        AnalyzeTab analyzeTab = new AnalyzeTab(
                new MigLayout("", "[fill, grow]", "[fill, grow][]"),
                debug
        );
        tabbedPane.addTab(MainHelper.analyzeTabTitle, analyzeTab.getRoot());

        SettingsTab settingsTab = new SettingsTab(
                new MigLayout("", "[grow,fill]", "[grow,fill][]"),
                debug
        );
        tabbedPane.addTab(MainHelper.settingsTabTitle, settingsTab.getRoot());

        root.add(tabbedPane);
        root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void showWindow() {
        root.setSize(1280, 720);
        root.setLocationRelativeTo(null);
        root.setVisible(true);
    }

    public JFrame getRoot() {
        return root;
    }

    public static void main(String[] args) {
        boolean debug = args.length > 0 && args[0].equals("--debug");
        new MainGUI(debug).showWindow();
    }
}

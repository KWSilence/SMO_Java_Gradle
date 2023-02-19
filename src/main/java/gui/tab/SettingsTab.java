package gui.tab;

import com.google.gson.Gson;
import configs.SimulationConfig;
import gui.TableHelper;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

public class SettingsTab implements TabCreator {
    private final JPanel root;
    private final boolean debug;

    public SettingsTab(LayoutManager layoutManager, boolean debug) {
        this.root = new JPanel(layoutManager);
        this.debug = debug;
        SimulationConfig.ConfigJSON config = SimulationConfig.readJSON(SimulationConfig.getDefaultConfigPath(debug));
        //[COM]{ELEMENT} Tab Settings: sources tab
        JTable sourcesTable = TableHelper.createTable(new String[]{"SourceNumber", "Lambda"}, Collections.singletonList(1));
        TableHelper.initTableRows(sourcesTable, config.getSources().size());
        setTableLambdas(sourcesTable, config.getSources());
        root.add(new JScrollPane(sourcesTable));
        //[COM]{ELEMENT} Tab Settings: processors tab
        JTable processorsTable = TableHelper.createTable(new String[]{"ProcessorNumber", "Lambda"}, Collections.singletonList(1));
        TableHelper.initTableRows(processorsTable, config.getProcessors().size());
        setTableLambdas(processorsTable, config.getProcessors());
        root.add(new JScrollPane(processorsTable), "wrap");
        //[COM]{ELEMENT} Tab Settings: sources count text field
        JTextField sourcesCountTextField = new JTextField(String.valueOf(config.getSources().size()));
        root.add(sourcesCountTextField, "split 2");
        //[COM]{ELEMENT} Tab Settings: set sources count button
        JButton setSourcesCountButton = new JButton("Set sources count");
        //[COM]{ACTION} Tab Settings: set sources count button
        setSourcesCountButton.addActionListener(e ->
                TableHelper.addOrDeleteRows(sourcesTable, Integer.parseInt(sourcesCountTextField.getText()))
        );
        root.add(setSourcesCountButton);
        //[COM]{ELEMENT} Tab Settings: processors count text field
        JTextField processorsCountTextField = new JTextField(String.valueOf(config.getProcessors().size()));
        root.add(processorsCountTextField, "split 2");
        //[COM]{ELEMENT} Tab Settings: set processors count button
        JButton setProcessorsCountButton = new JButton("Set processors count");
        //[COM]{ACTION} Tab Settings: set processors count button
        setProcessorsCountButton.addActionListener(e ->
                TableHelper.addOrDeleteRows(processorsTable, Integer.parseInt(processorsCountTextField.getText()))
        );
        root.add(setProcessorsCountButton, "wrap");
        //[COM]{ELEMENT} Tab Settings: source lambdas text field
        JTextField sourcesLambdaTextField = new JTextField("1.0");
        root.add(sourcesLambdaTextField, "split 2");
        //[COM]{ELEMENT} Tab Settings: set source lambdas button
        JButton setSourcesLambdaButton = new JButton("Set sources lambdas");
        //[COM]{ACTION} Tab Settings: set source Lambdas button
        setSourcesLambdaButton.addActionListener(e -> {
            String val = sourcesLambdaTextField.getText();
            for (int i = 0; i < sourcesTable.getRowCount(); i++) {
                sourcesTable.setValueAt(val, i, 1);
            }
        });
        root.add(setSourcesLambdaButton);
        //[COM]{ELEMENT} Tab Settings: processor lambdas text field
        JTextField processorsLambdaTextField = new JTextField("1.0");
        root.add(processorsLambdaTextField, "split 2");
        //[COM]{ELEMENT} Tab Settings: set processor lambdas button
        JButton setProcessorsLambdaButton = new JButton("Set processors lambdas");
        //[COM]{ACTION} Tab Settings: set processor lambdas button
        setProcessorsLambdaButton.addActionListener(e -> {
            String val = processorsLambdaTextField.getText();
            for (int i = 0; i < processorsTable.getRowCount(); i++) {
                processorsTable.setValueAt(val, i, 1);
            }
        });
        root.add(setProcessorsLambdaButton, "wrap");
        //[COM]{ELEMENT} Tab Settings: buffer capacity text field
        root.add(new JLabel("Buffer Capacity"), "split 4");
        JTextField bufferCapacityTextField = new JTextField(String.valueOf(config.getBufferCapacity()));
        root.add(bufferCapacityTextField);
        //[COM]{ELEMENT} Tab Settings: request count text field
        root.add(new JLabel("Requests Count"));
        JTextField requestCountTextField = new JTextField(String.valueOf(config.getRequestsCount()));
        root.add(requestCountTextField);
        //[COM]{ELEMENT} Tab Settings: refresh button
        JButton refreshButton = new JButton("Refresh");
        //[COM]{ACTION} Tab Settings: refresh button
        refreshButton.addActionListener(e -> {
            SimulationConfig.ConfigJSON configRefresh = SimulationConfig.readJSON(SimulationConfig.getDefaultConfigPath(debug));
            TableHelper.initTable(sourcesTable, configRefresh.getSources().size());
            setTableLambdas(sourcesTable, configRefresh.getSources());
            TableHelper.initTable(processorsTable, configRefresh.getProcessors().size());
            setTableLambdas(processorsTable, configRefresh.getProcessors());
            sourcesCountTextField.setText(String.valueOf(configRefresh.getSources().size()));
            processorsCountTextField.setText(String.valueOf(configRefresh.getProcessors().size()));
            bufferCapacityTextField.setText(String.valueOf(configRefresh.getBufferCapacity()));
            requestCountTextField.setText(String.valueOf(configRefresh.getRequestsCount()));
        });
        root.add(refreshButton, "split 2");
        //[COM]{ELEMENT} Tab Settings: save button
        JButton saveButton = new JButton("Save");
        //[COM]{ACTION} Tab Settings: save button
        saveButton.addActionListener(e -> {
            ArrayList<Double> sources = getTableLambdas(sourcesTable);
            ArrayList<Double> processors = getTableLambdas(processorsTable);
            int bufferCapacity = Integer.parseInt(bufferCapacityTextField.getText());
            int requestsCount = Integer.parseInt(requestCountTextField.getText());
            SimulationConfig.ConfigJSON configSave = new SimulationConfig.ConfigJSON(
                    sources, processors, bufferCapacity, requestsCount
            );
            String fileName = SimulationConfig.getDefaultConfigPath(debug);
            try {
                PrintWriter writer = new PrintWriter(fileName, StandardCharsets.UTF_8);
                Gson gson = new Gson();
                writer.print(gson.toJson(configSave));
                writer.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
        root.add(saveButton);
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

    @Override
    public JPanel getRoot() {
        return root;
    }
}

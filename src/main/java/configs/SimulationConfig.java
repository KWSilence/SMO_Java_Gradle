package configs;

import com.google.gson.Gson;
import smo_system.component.Buffer;
import smo_system.component.Processor;
import smo_system.component.Source;
import smo_system.manager.ProductionManager;
import smo_system.manager.SelectionManager;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimulationConfig {
    public static class ConfigJSON {
        private final int requestsCount;
        private final int bufferCapacity;
        private final List<Double> sources;
        private final List<Double> processors;

        ConfigJSON() {
            sources = null;
            bufferCapacity = 0;
            processors = null;
            requestsCount = 0;
        }

        public ConfigJSON(List<Double> sources, List<Double> processors, int bufferCapacity, int requestsCount) {
            this.sources = sources;
            this.bufferCapacity = bufferCapacity;
            this.processors = processors;
            this.requestsCount = requestsCount;
        }

        public List<Double> getSources() {
            return sources;
        }

        public List<Double> getProcessors() {
            return processors;
        }

        public int getBufferCapacity() {
            return bufferCapacity;
        }

        public int getRequestsCount() {
            return requestsCount;
        }
    }

    private ArrayList<Source> sources;
    private ArrayList<Processor> processors;
    private Buffer buffer;
    private ProductionManager productionManager;
    private SelectionManager selectionManager;
    private final ConfigJSON config;

    public SimulationConfig(String fileName) {
        this.config = readJSON(fileName);
        parseConfig(config);
    }

    public SimulationConfig(ConfigJSON config) {
        this.config = config;
        parseConfig(config);
    }

    public ConfigJSON getConfig() {
        return config;
    }

    public static ConfigJSON readJSON(String fileName) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(new FileReader(fileName), ConfigJSON.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ConfigJSON();
    }

    public static String getDefaultConfigPath(boolean debug) {
        return debug ? "src/main/resources/config.json" : "config.json";
    }

    public static SimulationConfig useDefaultConfigFile(boolean debug) {
        return new SimulationConfig(getDefaultConfigPath(debug));
    }

    public static ConfigJSON getDefaultConfigJSON() {
        ArrayList<Double> defaultSources = new ArrayList<>(Arrays.asList(1.0, 1.0, 1.0));
        ArrayList<Double> defaultProcessors = new ArrayList<>(Arrays.asList(1.0, 1.0));
        return new ConfigJSON(defaultSources, defaultProcessors, 3, 1000);
    }

    public static void initDefaultConfigFile(boolean debug) {
        File configFile = new File(getDefaultConfigPath(debug));
        if (!configFile.exists()) {
            saveConfigFile(configFile, getDefaultConfigJSON());
        }
    }

    public static void saveConfigFile(File configFile, ConfigJSON config) {
        try {
            PrintWriter writer = new PrintWriter(configFile, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            writer.print(gson.toJson(config));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseConfig(ConfigJSON config) {
        this.sources = new ArrayList<>();
        for (int i = 0; i < config.sources.size(); i++) {
            this.sources.add(new Source(i, config.sources.get(i)));
        }
        this.buffer = new Buffer(config.bufferCapacity);
        this.processors = new ArrayList<>();
        for (int i = 0; i < config.processors.size(); i++) {
            this.processors.add(new Processor(i, config.processors.get(i)));
        }
        this.productionManager = new ProductionManager(sources, buffer, config.requestsCount);
        this.selectionManager = new SelectionManager(processors, buffer, sources.size());
    }

    public List<Source> getSources() {
        return sources;
    }

    public List<Processor> getProcessors() {
        return processors;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public ProductionManager getProductionManager() {
        return productionManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }
}

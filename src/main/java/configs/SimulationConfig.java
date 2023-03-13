package configs;

import com.google.gson.Gson;
import smo_system.component.Buffer;
import smo_system.component.Processor;
import smo_system.component.Source;
import smo_system.manager.ProductionManager;
import smo_system.manager.SelectionManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SimulationConfig {
    public static class ConfigJSON implements Serializable {
        private final int requestsCount;
        private final int bufferCapacity;
        private final List<Double> sources;
        private final List<Double> processors;
        private transient boolean onError;

        ConfigJSON() {
            this.bufferCapacity = 3;
            this.requestsCount = 1000;
            this.sources = List.of(1.0, 1.0, 1.0);
            this.processors = List.of(1.0, 1.0);
            this.onError = false;
        }

        public static ConfigJSON getDefaultConfig() {
            return new ConfigJSON();
        }

        public static ConfigJSON getDefaultConfigOnError() {
            ConfigJSON configJSON = new ConfigJSON();
            configJSON.onError = true;
            return configJSON;
        }

        public ConfigJSON(int requestsCount, int bufferCapacity, List<Double> sources, List<Double> processors) {
            this.bufferCapacity = bufferCapacity;
            this.requestsCount = requestsCount;
            this.sources = new ArrayList<>(sources);
            this.processors = new ArrayList<>(processors);
            this.onError = false;
        }

        public List<Double> getSources() {
            return new ArrayList<>(sources);
        }

        public List<Double> getProcessors() {
            return new ArrayList<>(processors);
        }

        public int getBufferCapacity() {
            return bufferCapacity;
        }

        public int getRequestsCount() {
            return requestsCount;
        }

        public boolean createdOnError() {
            return onError;
        }
    }

    private final ConfigJSON config;

    public SimulationConfig(ConfigJSON config) {
        this.config = config;
    }

    public ConfigJSON getConfig() {
        return config;
    }

    public static ConfigJSON readJSON(String fileName) {
        Gson gson = new Gson();
        try {
            Path path = Path.of(fileName);
            Charset charset = StandardCharsets.UTF_8;
            String json = Files.readString(path, charset);
            return gson.fromJson(json, ConfigJSON.class);
        } catch (Exception e) {
            return ConfigJSON.getDefaultConfigOnError();
        }
    }

    public static boolean saveConfigFile(File configFile, ConfigJSON config) {
        try (PrintWriter writer = new PrintWriter(configFile, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            writer.print(gson.toJson(config == null ? ConfigJSON.getDefaultConfig() : config));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public List<Source> createSources() {
        List<Source> sources = new ArrayList<>();
        for (int i = 0; i < config.sources.size(); i++) {
            sources.add(new Source(i, config.sources.get(i)));
        }
        return sources;
    }

    public List<Processor> createProcessors() {
        List<Processor> processors = new ArrayList<>();
        for (int i = 0; i < config.processors.size(); i++) {
            processors.add(new Processor(i, config.processors.get(i)));
        }
        return processors;
    }

    public Buffer createBuffer() {
        return new Buffer(config.bufferCapacity);
    }

    public ProductionManager createProductionManager(Buffer buffer) {
        return new ProductionManager(createSources(), buffer, config.requestsCount);
    }

    public SelectionManager createSelectionManager(Buffer buffer) {
        return new SelectionManager(createProcessors(), buffer, config.sources.size());
    }
}

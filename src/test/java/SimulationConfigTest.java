import configs.SimulationConfig;
import configs.SimulationConfig.ConfigJSON;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import smo_system.component.Buffer;
import smo_system.component.Processor;
import smo_system.component.Source;
import smo_system.manager.ProductionManager;
import smo_system.manager.SelectionManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulationConfigTest {
    private static final String TEST_DIRECTORY = "test_directory/";

    @BeforeAll
    static void prepareTestDir() {
        File testDir = new File(TEST_DIRECTORY);
        testDir.mkdir();
    }

    @AfterAll
    static void clearTestDir() {
        try {
            Files.walk(Path.of(TEST_DIRECTORY))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testReadingCorrectConfigJSON() {
        String correctConfigFilePath = getConfigPath(new Object() {});
        ConfigJSON expectedConfigJSON = new ConfigJSON(
                1000,
                3,
                List.of(1.0, 2.0, 2.0),
                List.of(1.0, 2.0)
        );
        String correctConfigFileContent = getConfigStringJSON(expectedConfigJSON);

        File configFile = new File(correctConfigFilePath);
        try (PrintWriter configWriter = new PrintWriter(configFile)) {
            configWriter.println(correctConfigFileContent);
        } catch (Exception e) {
            fail(e);
        }

        ConfigJSON config = SimulationConfig.readJSON(correctConfigFilePath);
        assertFalse(config.createdOnError());
        compareConfigs(expectedConfigJSON, config);
        configFile.delete();
    }

    @Test
    void testOverrideConfigJSON() {
        String overrideConfigFilePath = getConfigPath(new Object() {});
        ConfigJSON defaultConfig = ConfigJSON.getDefaultConfig();
        List<Double> changedProcessors = List.of(5.5, 3.7, 9.1);
        ConfigJSON expectedConfigJSON = new ConfigJSON(
                defaultConfig.getRequestsCount(),
                defaultConfig.getBufferCapacity(),
                defaultConfig.getSources(),
                changedProcessors
        );
        String processorsStr = String.join(",", changedProcessors.stream().map(String::valueOf).toList());
        String overrideConfigFileContent = "{processors:[" + processorsStr + "]}";

        File configFile = new File(overrideConfigFilePath);
        try (PrintWriter configWriter = new PrintWriter(configFile)) {
            configWriter.println(overrideConfigFileContent);
        } catch (Exception e) {
            fail(e);
        }

        ConfigJSON config = SimulationConfig.readJSON(overrideConfigFilePath);
        assertFalse(config.createdOnError());
        compareConfigs(expectedConfigJSON, config);
        configFile.delete();
    }

    @Test
    void testReadingInvalidConfigJSON() {
        String invalidConfigFilePath = getConfigPath(new Object() {
        });
        String invalidConfigFileContent = "invalid content";
        ConfigJSON configOnError = ConfigJSON.getDefaultConfigOnError();

        File configFile = new File(invalidConfigFilePath);
        try (PrintWriter configWriter = new PrintWriter(configFile)) {
            configWriter.println(invalidConfigFileContent);
        } catch (Exception e) {
            fail(e);
        }

        ConfigJSON config = SimulationConfig.readJSON(invalidConfigFilePath);
        assertTrue(config.createdOnError());
        compareConfigs(configOnError, config);
        configFile.delete();
    }

    @Test
    void testSavingConfigFile() {
        Path configFilePath = Paths.get(getConfigPath(new Object() {}));
        File configFile = configFilePath.toFile();
        ConfigJSON configJSON = new ConfigJSON(
                1234,
                5,
                List.of(5.3, 2.1, 4.6),
                List.of(9.9, 7.8)
        );
        String expectedConfigContent = getConfigStringJSON(configJSON);
        boolean saved = SimulationConfig.saveConfigFile(configFile, configJSON);
        assertTrue(saved);
        String configContent = null;
        try {
            configContent = Files.readString(configFilePath);
        } catch (IOException e) {
            fail(e);
        }
        assertNotNull(configContent);
        assertEquals(expectedConfigContent, configContent);
        configFile.delete();

        expectedConfigContent = getConfigStringJSON(ConfigJSON.getDefaultConfig());
        saved = SimulationConfig.saveConfigFile(configFile, null);
        assertTrue(saved);
        String defaultConfigContent = null;
        try {
            defaultConfigContent = Files.readString(configFilePath);
        } catch (IOException e) {
            fail(e);
        }
        assertNotNull(defaultConfigContent);
        assertEquals(expectedConfigContent, defaultConfigContent);
        configFile.delete();

        File wrongFile = new File(TEST_DIRECTORY + "/wrong_dir/config.json");
        saved = SimulationConfig.saveConfigFile(wrongFile, configJSON);
        assertFalse(saved);
    }

    @Test
    void testComponentsCreation() {
        ConfigJSON configJSON = new ConfigJSON(
                1234,
                5,
                List.of(5.3, 2.1, 4.6),
                List.of(9.9, 7.8)
        );
        SimulationConfig simulationConfig = new SimulationConfig(configJSON);
        compareConfigs(configJSON, simulationConfig.getConfig());

        List<Source> sources = simulationConfig.createSources();
        assertEquals(configJSON.getSources().size(), sources.size(), "created sources count is not correct");
        for (int i = 0; i < sources.size(); ++i) {
            Source source = sources.get(i);
            double lambda = configJSON.getSources().get(i);
            assertEquals(i, source.getNumber(), "sources number presented in wrong order");
            assertEquals(lambda, source.getLambda(), "source lambda is not set correctly");
        }

        List<Processor> processors = simulationConfig.createProcessors();
        assertEquals(configJSON.getProcessors().size(), processors.size(), "created processors count is not correct");
        for (int i = 0; i < processors.size(); ++i) {
            Processor processor = processors.get(i);
            double lambda = configJSON.getProcessors().get(i);
            assertEquals(i, processor.getNumber(), "processors number presented in wrong order");
            assertEquals(lambda, processor.getLambda(), "processor lambda is not set correctly");
        }

        Buffer buffer = simulationConfig.createBuffer();
        assertEquals(configJSON.getBufferCapacity(), buffer.getCapacity(), "buffer capacity is not set correctly");

        ProductionManager productionManager = simulationConfig.createProductionManager(buffer);
        CompareUtil.compareLists(sources, productionManager.getSources(), CompareUtil::compareSourcesWithoutRandom);
        assertEquals(sources.size(), productionManager.getRejectedRequests().size());

        SelectionManager selectionManager = simulationConfig.createSelectionManager(buffer);
        CompareUtil.compareLists(processors, selectionManager.getProcessors(), CompareUtil::compareProcessors);
        assertEquals(sources.size(), selectionManager.getSuccessRequests().size());

        try {
            Field productionBufferField = productionManager.getClass().getDeclaredField("buffer");
            productionBufferField.setAccessible(true);
            Buffer productionBuffer = (Buffer) productionBufferField.get(productionManager);

            Field selectionBufferField = selectionManager.getClass().getDeclaredField("buffer");
            selectionBufferField.setAccessible(true);
            Buffer selectionBuffer = (Buffer) selectionBufferField.get(selectionManager);

            assertEquals(productionBuffer, selectionBuffer);
        } catch (Exception e) {
            fail(e);
        }
    }

    private String getConfigPath(Object object) {
        return TEST_DIRECTORY + object.getClass().getEnclosingMethod().getName() + ".json";
    }

    private void compareConfigs(SimulationConfig.ConfigJSON expectedConfig, SimulationConfig.ConfigJSON actualConfig) {
        assertEquals(expectedConfig.getRequestsCount(), actualConfig.getRequestsCount());
        assertEquals(expectedConfig.getBufferCapacity(), actualConfig.getBufferCapacity());
        assertEquals(expectedConfig.createdOnError(), actualConfig.createdOnError());
        CompareUtil.compareLists(expectedConfig.getSources(), actualConfig.getSources(), Assertions::assertEquals);
        CompareUtil.compareLists(expectedConfig.getProcessors(), actualConfig.getProcessors(), Assertions::assertEquals);
    }

    private String getConfigStringJSON(SimulationConfig.ConfigJSON configJSON) {
        return "{\"requestsCount\":" + configJSON.getRequestsCount() + "," +
                "\"bufferCapacity\":" + configJSON.getBufferCapacity() + "," +
                "\"sources\":[" + String.join(",", configJSON.getSources().stream().map(String::valueOf).toList()) + "]," +
                "\"processors\":[" + String.join(",", configJSON.getProcessors().stream().map(String::valueOf).toList()) + "]}";
    }
}

import configs.SimulationConfig;
import configs.SimulationConfig.ConfigJSON;
import org.junit.jupiter.api.AfterAll;
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

/**
 * Tests related to SimulationConfig and its inner class ConfigJSON
 **/
class SimulationConfigTest {
    // test directory path
    private static final String TEST_DIRECTORY = "test_directory/";

    /**
     * prepare test environment before all tests - create test directory
     **/
    @BeforeAll
    static void prepareTestDir() {
        File testDir = new File(TEST_DIRECTORY);
        testDir.mkdir();
    }

    /**
     * remove test environment after all tests - remove test directory
     **/
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

    /**
     * Check readJSON function with correct config file
     **/
    @Test
    void testReadingCorrectConfigJSON() {
        // get config file path
        String correctConfigFilePath = getConfigPath(new Object() {});
        // create expected ConfigJSON class
        ConfigJSON expectedConfigJSON = new ConfigJSON(
                1000,
                3,
                List.of(1.0, 2.0, 2.0),
                List.of(1.0, 2.0)
        );
        // create json string from expected ConfigJSON
        String correctConfigFileContent = getConfigStringJSON(expectedConfigJSON);

        // write json string to file
        File configFile = new File(correctConfigFilePath);
        try (PrintWriter configWriter = new PrintWriter(configFile)) {
            configWriter.println(correctConfigFileContent);
        } catch (Exception e) {
            fail(e);
        }

        // read config file
        ConfigJSON config = SimulationConfig.readJSON(correctConfigFilePath);
        // check reading without error
        assertFalse(config.createdOnError());
        // compare expected ConfigJSON with ConfigJSON after read
        CompareUtil.compareConfigs(expectedConfigJSON, config);
        // delete config file
        configFile.delete();
    }

    /**
     * Checking override some fields of ConfigJSON.
     * In config file should be declared some fields to override.
     * After reading config file ConfigJSON should change only override fields, other use default values
     **/
    @Test
    void testOverrideConfigJSON() {
        // get config file path
        String overrideConfigFilePath = getConfigPath(new Object() {});
        // get default config class
        ConfigJSON defaultConfig = ConfigJSON.getDefaultConfig();
        // processor lambdas to override
        List<Double> changedProcessors = List.of(5.5, 3.7, 9.1);
        // expected config class with default values, only processors values changed
        ConfigJSON expectedConfigJSON = new ConfigJSON(
                defaultConfig.getRequestsCount(),
                defaultConfig.getBufferCapacity(),
                defaultConfig.getSources(),
                changedProcessors
        );
        // form lambdas string
        String processorsStr = String.join(",", changedProcessors.stream().map(String::valueOf).toList());
        // form json string with only processors lambdas
        String overrideConfigFileContent = "{processors:[" + processorsStr + "]}";

        // write json string to config file
        File configFile = new File(overrideConfigFilePath);
        try (PrintWriter configWriter = new PrintWriter(configFile)) {
            configWriter.println(overrideConfigFileContent);
        } catch (Exception e) {
            fail(e);
        }

        // read config file
        ConfigJSON config = SimulationConfig.readJSON(overrideConfigFilePath);
        // check reading without error
        assertFalse(config.createdOnError());
        // compare expected ConfigJSON with ConfigJSON after read
        CompareUtil.compareConfigs(expectedConfigJSON, config);
        // delete config file
        configFile.delete();
    }

    /**
     * Checking reading of invalid config file.
     * Config file should contain non json string.
     * ConfigJSON after read should be equal to default config class on error (createOnError flag = true).
     **/
    @Test
    void testReadingInvalidConfigJSON() {
        // config file path
        String invalidConfigFilePath = getConfigPath(new Object() {});
        // invalid json content
        String invalidConfigFileContent = "invalid content";
        // get default config class on error
        ConfigJSON configOnError = ConfigJSON.getDefaultConfigOnError();

        // write invalid json content
        File configFile = new File(invalidConfigFilePath);
        try (PrintWriter configWriter = new PrintWriter(configFile)) {
            configWriter.println(invalidConfigFileContent);
        } catch (Exception e) {
            fail(e);
        }

        // read config class
        ConfigJSON config = SimulationConfig.readJSON(invalidConfigFilePath);
        // check reading with error flag
        assertTrue(config.createdOnError());
        // error config should be equal to its default
        CompareUtil.compareConfigs(configOnError, config);
        // delete config file
        configFile.delete();
    }

    /**
     * Checking saving config file function.
     * Check creation of config file with normal ConfigJSON (filled), return true flag.
     * Check creation of config file with null ConfigJSON, return true flag. Config file should contain default config values.
     * Check creation of config file with non-existing directory, return false flag.
     **/
    @Test
    void testSavingConfigFile() {
        // config file path
        Path configFilePath = Paths.get(getConfigPath(new Object() {}));
        File configFile = configFilePath.toFile();
        // create filled config class
        ConfigJSON configJSON = new ConfigJSON(
                1234,
                5,
                List.of(5.3, 2.1, 4.6),
                List.of(9.9, 7.8)
        );
        // create expected json string from config class
        String expectedConfigContent = getConfigStringJSON(configJSON);
        // save config class, returned value should be true
        boolean saved = SimulationConfig.saveConfigFile(configFile, configJSON);
        assertTrue(saved);
        // check that json string equal to file content
        String configContent = null;
        try {
            configContent = Files.readString(configFilePath);
        } catch (IOException e) {
            fail(e);
        }
        assertNotNull(configContent);
        assertEquals(expectedConfigContent, configContent);
        configFile.delete();

        // check save null config class saving, return value - true, content equals to default config
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

        // check save to non-existing directory, return value - false
        File wrongFile = new File(TEST_DIRECTORY + "/wrong_dir/config.json");
        saved = SimulationConfig.saveConfigFile(wrongFile, configJSON);
        assertFalse(saved);
    }

    /**
     * Checking creation main components and composite components (managers).
     * Check lambdas values and numbers (equals to order in array) of created processors and sources. Check buffer capacity.
     * Check request count in productionManager. Buffer of production and selection managers should be same (same pointers).
     * Check productionManager source list and rejected requests list size equals to sources count.
     * Check selectionManager processor list equals processors count, success request list equals to sources count.
     **/
    @Test
    void testComponentsCreation() {
        // create config class and simulation config
        ConfigJSON configJSON = new ConfigJSON(
                1234,
                5,
                List.of(5.3, 2.1, 4.6),
                List.of(9.9, 7.8)
        );
        SimulationConfig simulationConfig = new SimulationConfig(configJSON);
        CompareUtil.compareConfigs(configJSON, simulationConfig.getConfig());

        // compare source lambdas and number
        List<Source> sources = simulationConfig.createSources();
        assertEquals(configJSON.getSources().size(), sources.size(), "created sources count is not correct");
        for (int i = 0; i < sources.size(); ++i) {
            Source source = sources.get(i);
            double lambda = configJSON.getSources().get(i);
            assertEquals(i, source.getNumber(), "sources number presented in wrong order");
            assertEquals(lambda, source.getLambda(), "source lambda is not set correctly");
        }

        // compare processors lambdas and number
        List<Processor> processors = simulationConfig.createProcessors();
        assertEquals(configJSON.getProcessors().size(), processors.size(), "created processors count is not correct");
        for (int i = 0; i < processors.size(); ++i) {
            Processor processor = processors.get(i);
            double lambda = configJSON.getProcessors().get(i);
            assertEquals(i, processor.getNumber(), "processors number presented in wrong order");
            assertEquals(lambda, processor.getLambda(), "processor lambda is not set correctly");
        }

        // check buffer capacity
        Buffer buffer = simulationConfig.createBuffer();
        assertEquals(configJSON.getBufferCapacity(), buffer.getCapacity(), "buffer capacity is not set correctly");

        // check productionManager request count, sources list and rejected request list
        ProductionManager productionManager = simulationConfig.createProductionManager(buffer);
        assertEquals(configJSON.getRequestsCount(), productionManager.getMaxRequestCount());
        CompareUtil.compareLists(sources, productionManager.getSources(), CompareUtil::compareSourcesWithoutRandom);
        assertEquals(sources.size(), productionManager.getRejectedRequests().size());

        // check productionManager sources list and rejected request list
        SelectionManager selectionManager = simulationConfig.createSelectionManager(buffer);
        CompareUtil.compareLists(processors, selectionManager.getProcessors(), CompareUtil::compareProcessors);
        assertEquals(sources.size(), selectionManager.getSuccessRequests().size());

        // check buffer pointers in managers using reflection
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

    /**
     * Get config path using name of test method
     **/
    private String getConfigPath(Object object) {
        return TEST_DIRECTORY + object.getClass().getEnclosingMethod().getName() + ".json";
    }

    /**
     * Get json string from ConfigJSON
     **/
    private String getConfigStringJSON(SimulationConfig.ConfigJSON configJSON) {
        return "{\"requestsCount\":" + configJSON.getRequestsCount() + "," +
                "\"bufferCapacity\":" + configJSON.getBufferCapacity() + "," +
                "\"sources\":[" + String.join(",", configJSON.getSources().stream().map(String::valueOf).toList()) + "]," +
                "\"processors\":[" + String.join(",", configJSON.getProcessors().stream().map(String::valueOf).toList()) + "]}";
    }
}

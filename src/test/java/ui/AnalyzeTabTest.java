package ui;

import gui.ComponentHelper.AnalyzeHelper;
import gui.ComponentHelper.MainHelper;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

class AnalyzeTabTest extends AbstractUiTest {
    private JComboBoxFixture selectorComboBox;
    private JTextComponentFixture toTextField;
    private JTextComponentFixture visualStepTextField;
    private JButtonFixture startButton;
    private JButtonFixture stopButton;

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeTabTest.class);

    @Override
    protected void onSetUp() {
        super.onSetUp();
        frame.tabbedPane(MainHelper.tabbedPane).selectTab(MainHelper.analyzeTabTitle);
        selectorComboBox = frame.comboBox(AnalyzeHelper.selector);
        toTextField = frame.textBox(AnalyzeHelper.to);
        visualStepTextField = frame.textBox(AnalyzeHelper.visualStep);
        startButton = frame.button(AnalyzeHelper.start);
        stopButton = frame.button(AnalyzeHelper.stop);
    }

    @Override
    protected void onTearDown() {
        super.onTearDown();
        selectorComboBox = null;
        toTextField = null;
        visualStepTextField = null;
        startButton = null;
        stopButton = null;
    }

    @Test
    void startStopAnalyzeTest() {
        LOGGER.debug("Analyze: [ start startStopAnalyzeTest ]");
        LOGGER.debug("Analyze: start stop iterations:");
        for (int iter = 0; iter < 4; iter++) {
            long startTime = System.currentTimeMillis();
            startButton.click();
            Pause.pause(1, TimeUnit.SECONDS);
            stopButton.requireEnabled().click();
            startButton.requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Analyze:     iter [{}] - {}", iter, stopTime - startTime);
            Pause.pause(3, TimeUnit.SECONDS);
        }
        LOGGER.debug("Analyze: [ end startStopAnalyzeTest ]");
    }

    @Test
    void multipleStartAnalyzeTest() {
        LOGGER.debug("Analyze: [ start multipleStartAnalyzeTest ]");
        LOGGER.debug("Analyze: start iterations:");
        for (int iter = 0; iter < 4; iter++) {
            long startTime = System.currentTimeMillis();
            startButton.click().requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Analyze:     iter [{}] - {}", iter, stopTime - startTime);
            Pause.pause(3, TimeUnit.SECONDS);
        }
        LOGGER.debug("Analyze: [ end multipleStartAnalyzeTest ]");
    }

    @Test
    void differentVisualStepAnalyzeTest() {
        LOGGER.debug("Analyze: [ start differentVisualStepAnalyzeTest ]");
        int[] stepArray = {5, 10, 15, 20};
        LOGGER.debug("Analyze: different visual step iterations:");
        for (int step : stepArray) {
            visualStepTextField.setText(String.valueOf(step));
            long startTime = System.currentTimeMillis();
            startButton.click().requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Analyze:     step [{}] - {}", step, stopTime - startTime);
            Pause.pause(3, TimeUnit.SECONDS);
        }
        LOGGER.debug("Analyze: [ end differentVisualStepAnalyzeTest ]");
    }

    @Test
    void differentSelectorAnalyzeTest() {
        LOGGER.debug("Analyze: [ start differentSelectorAnalyzeTest ]");
        String[] selectorArray = {AnalyzeHelper.selectorSourceOption, AnalyzeHelper.selectorProcessorOption, AnalyzeHelper.selectorBufferOption};
        LOGGER.debug("Analyze: different selector iterations:");
        for (String selector : selectorArray) {
            selectorComboBox.selectItem(selector);
            long startTime = System.currentTimeMillis();
            startButton.click().requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Analyze:     selector [{}] - {}", selector, stopTime - startTime);
            Pause.pause(3, TimeUnit.SECONDS);
        }
        LOGGER.debug("Analyze: [ end differentSelectorAnalyzeTest ]");
    }

    @Test
    void differentRangeAnalyzeTest() {
        LOGGER.debug("Analyze: [ start differentRangeAnalyzeTest ]");
        int[] rangeArray = {50, 100, 150, 200};
        LOGGER.debug("Analyze: different range iterations:");
        for (int range : rangeArray) {
            toTextField.setText(String.valueOf(range));
            long startTime = System.currentTimeMillis();
            startButton.click().requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Analyze:     range [{}] - {}", range, stopTime - startTime);
            Pause.pause(3, TimeUnit.SECONDS);
        }
        LOGGER.debug("Analyze: [ end differentRangeAnalyzeTest ]");
    }
}

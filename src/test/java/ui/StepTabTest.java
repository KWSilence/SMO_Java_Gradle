package ui;

import gui.ComponentHelper.MainHelper;
import gui.ComponentHelper.SettingsHelper;
import gui.ComponentHelper.StepHelper;
import org.assertj.swing.data.Index;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

class StepTabTest extends AbstractUiTest {

    private JButtonFixture startButton;
    private JButtonFixture stopButton;
    private JButtonFixture skipButton;

    private static final Logger LOGGER = LoggerFactory.getLogger(StepTabTest.class);

    @Override
    protected void onSetUp() {
        super.onSetUp();
        frame.tabbedPane(MainHelper.tabbedPane).selectTab(MainHelper.stepTabTitle);
        startButton = frame.button(StepHelper.start);
        stopButton = frame.button(StepHelper.stop);
        skipButton = frame.button(StepHelper.skip);
    }

    @Override
    protected void onTearDown() {
        super.onTearDown();
        startButton = null;
        stopButton = null;
        skipButton = null;
    }

    @Test
    void multipleStartStopStepTest() {
        LOGGER.debug("Step: [ start multipleStartStopStepTest ]");
        int oldRequestCount = setupRequestCount(1000_000);
        try {
            LOGGER.debug("Step: multiple start stop iterations:");
            for (int iter = 0; iter < 4; iter++) {
                long startTime = System.currentTimeMillis();
                startButton.click();
                Pause.pause(1, TimeUnit.SECONDS);
                stopButton.requireEnabled().click();
                long stopTime = System.currentTimeMillis();
                LOGGER.debug("Step:     iter [{}] - {}", iter, stopTime - startTime);
            }
        } catch (Exception e) {
            setupRequestCount(oldRequestCount);
            throw e;
        }
        setupRequestCount(oldRequestCount);
        LOGGER.debug("Step: [ end multipleStartStopStepTest ]");
    }

    @Test
    void multipleStartSkipStopStepTest() {
        LOGGER.debug("Step: [ start multipleStartSkipStopStepTest ]");
        int oldRequestCount = setupRequestCount(1000_000);
        try {
            LOGGER.debug("Step: multiple start skip stop iterations:");
            for (int iter = 0; iter < 4; iter++) {
                long startTime = System.currentTimeMillis();
                startButton.click();
                skipButton.requireEnabled().click();
                Pause.pause(1, TimeUnit.SECONDS);
                stopButton.requireEnabled().click();
                long stopTime = System.currentTimeMillis();
                LOGGER.debug("Step:     iter [{}] - {}", iter, stopTime - startTime);
            }
        } catch (Exception e) {
            setupRequestCount(oldRequestCount);
            throw e;
        }
        setupRequestCount(oldRequestCount);
        LOGGER.debug("Step: [ end multipleStartSkipStopStepTest ]");
    }

    @Test
    void multipleStartNextStopStepTest() {
        LOGGER.debug("Step: [ start multipleStartNextStopStepTest ]");
        int oldRequestCount = setupRequestCount(1000_000);
        try {
            LOGGER.debug("Step: multiple start next stop iterations:");
            int[] nextArray = {5, 10, 15, 20};
            for (int iter = 0; iter < 4; iter++) {
                int nextCount = nextArray[iter];
                long startTime = System.currentTimeMillis();
                startButton.click();
                for (int nextIter = 0; nextIter < nextCount; nextIter++) {
                    Pause.pause(100, TimeUnit.MILLISECONDS);
                    startButton.requireEnabled().click();
                }
                stopButton.requireEnabled().click();
                long stopTime = System.currentTimeMillis();
                LOGGER.debug("Step:     iter next [{}, {}] - {}", iter, nextCount, stopTime - startTime);
            }
        } catch (Exception e) {
            setupRequestCount(oldRequestCount);
            throw e;
        }
        setupRequestCount(oldRequestCount);
        LOGGER.debug("Step: [ end multipleStartNextStopStepTest ]");
    }

    @Test
    void multipleStartNextSkipStopStepTest() {
        LOGGER.debug("Step: [ start multipleStartNextSkipStopStepTest ]");
        int oldRequestCount = setupRequestCount(1000_000);
        try {
            LOGGER.debug("Step: multiple start next skip stop iterations:");
            int[] nextArray = {5, 10, 15, 20};
            for (int iter = 0; iter < 4; iter++) {
                int nextCount = nextArray[iter];
                long startTime = System.currentTimeMillis();
                startButton.click();
                for (int nextIter = 0; nextIter < nextCount; nextIter++) {
                    Pause.pause(100, TimeUnit.MILLISECONDS);
                    startButton.requireEnabled().click();
                }
                skipButton.requireEnabled().click();
                Pause.pause(1, TimeUnit.SECONDS);
                stopButton.requireEnabled().click();
                long stopTime = System.currentTimeMillis();
                LOGGER.debug("Step:     iter next [{}, {}] - {}", iter, nextCount, stopTime - startTime);
            }
        } catch (Exception e) {
            setupRequestCount(oldRequestCount);
            throw e;
        }
        setupRequestCount(oldRequestCount);
        LOGGER.debug("Step: [ end multipleStartNextSkipStopStepTest ]");
    }

    @Test
    void multipleStartSkipAnalyzeStepTest() {
        LOGGER.debug("Step: [ start multipleStartSkipAnalyzeStepTest ]");
        JTabbedPaneFixture pane = frame.tabbedPane(MainHelper.tabbedPane);
        LOGGER.debug("Step: multiple start skip analyze iterations:");
        for (int iter = 0; iter < 4; iter++) {
            long startTime = System.currentTimeMillis();
            startButton.click();
            skipButton.click();
            startButton.requireEnabled(Timeout.timeout(1, TimeUnit.MINUTES)).click();
            pane.requireSelectedTab(Index.atIndex(0));
            long stopTime = System.currentTimeMillis();
            pane.selectTab(MainHelper.stepTabTitle);
            LOGGER.debug("Step:     iter [{}] - {}", iter, stopTime - startTime);
        }
        LOGGER.debug("Step: [ end multipleStartSkipAnalyzeStepTest ]");
    }

    private int setupRequestCount(int requestCount) {
        frame.tabbedPane(MainHelper.tabbedPane).selectTab(MainHelper.settingsTabTitle);
        JTextComponentFixture requestCountTextBox = frame.textBox(SettingsHelper.requestsCount);
        String oldRequestCount = requestCountTextBox.text();
        requestCountTextBox.setText(String.valueOf(requestCount));
        frame.button(SettingsHelper.save).click();
        frame.tabbedPane(MainHelper.tabbedPane).selectTab(MainHelper.stepTabTitle);
        LOGGER.debug("Step: request count change {} -> {}", oldRequestCount, requestCount);
        return Integer.parseInt(oldRequestCount);
    }
}

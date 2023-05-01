package ui;

import gui.ComponentHelper.AutoHelper;
import gui.ComponentHelper.MainHelper;
import gui.ComponentHelper.SettingsHelper;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JCheckBoxFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

class AutoTabTest extends AbstractUiTest {
    private JButtonFixture startButton;
    private JButtonFixture stopButton;
    private JCheckBoxFixture useN0ComboBox;
    private JTextComponentFixture n0TextField;

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoTabTest.class);

    @Override
    protected void onSetUp() {
        super.onSetUp();
        frame.tabbedPane(MainHelper.tabbedPane).selectTab(MainHelper.autoTabTitle);
        startButton = frame.button(AutoHelper.start);
        stopButton = frame.button(AutoHelper.stop);
        useN0ComboBox = frame.checkBox(AutoHelper.useN0);
        n0TextField = frame.textBox(AutoHelper.n0);
    }

    @Override
    protected void onTearDown() {
        super.onTearDown();
        startButton = null;
        stopButton = null;
        useN0ComboBox = null;
        n0TextField = null;
    }

    @Test
    void multipleStartStopNormalAutoTest() {
        LOGGER.debug("Auto: [ start multipleStartStopNormalAutoTest ]");
        int oldRequestCount = setupRequestCount(1000_000);
        try {
            LOGGER.debug("Auto: multiple start stop normal iterations:");
            useN0ComboBox.check(false);
            for (int iter = 0; iter < 4; iter++) {
                long startTime = System.currentTimeMillis();
                startButton.click();
                Pause.pause(1, TimeUnit.SECONDS);
                stopButton.requireEnabled().click();
                startButton.requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
                long stopTime = System.currentTimeMillis();
                LOGGER.debug("Auto:     iter [{}] - {}", iter, stopTime - startTime);
                Pause.pause(3, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            setupRequestCount(oldRequestCount);
            throw e;
        }
        setupRequestCount(oldRequestCount);
        LOGGER.debug("Auto: [ end multipleStartStopNormalAutoTest ]");
    }

    @Test
    void multipleStartStopUseN0AutoTest() {
        LOGGER.debug("Auto: [ start multipleStartStopUseN0AutoTest ]");
        int oldRequestCount = setupRequestCount(1000_000);
        try {
            LOGGER.debug("Auto: multiple start stop useN0 iterations:");
            useN0ComboBox.check(true);
            n0TextField.setText("1000000");
            for (int iter = 0; iter < 4; iter++) {
                long startTime = System.currentTimeMillis();
                startButton.click();
                Pause.pause(1, TimeUnit.SECONDS);
                stopButton.requireEnabled().click();
                startButton.requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
                long stopTime = System.currentTimeMillis();
                LOGGER.debug("Auto:     iter [{}] - {}", iter, stopTime - startTime);
                Pause.pause(3, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            setupRequestCount(oldRequestCount);
            throw e;
        }
        setupRequestCount(oldRequestCount);
        LOGGER.debug("Auto: [ end multipleStartStopUseN0AutoTest ]");
    }

    @Test
    void multipleStartStopWithN0AutoTest() {
        LOGGER.debug("Auto: [ start multipleStartStopWithN0AutoTest ]");
        int oldRequestCount = setupRequestCount(1000_000);
        try {
            LOGGER.debug("Auto: multiple start stop mix iterations:");
            useN0ComboBox.check(true);
            n0TextField.setText("1000000");
            for (int iter = 0; iter < 4; iter++) {
                useN0ComboBox.check(iter % 2 == 0);
                long startTime = System.currentTimeMillis();
                startButton.click();
                Pause.pause(1, TimeUnit.SECONDS);
                stopButton.requireEnabled().click();
                startButton.requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
                long stopTime = System.currentTimeMillis();
                LOGGER.debug("Auto:     iter useN0 [{}, {}] - {}", iter, iter % 2 == 0 ? "useN0" : "normal", stopTime - startTime);
                Pause.pause(3, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            setupRequestCount(oldRequestCount);
            throw e;
        }
        setupRequestCount(oldRequestCount);
        LOGGER.debug("Auto: [ end multipleStartStopWithN0AutoTest ]");
    }

    @Test
    void multipleUseN0WithDifferentN0AutoTest() {
        LOGGER.debug("Auto: [ start multipleUseN0WithDifferentN0AutoTest ]");
        int[] n0Array = {50, 100, 150, 200};
        LOGGER.debug("Auto: multiple useN0 with different N0 iterations:");
        for (int n0 : n0Array) {
            useN0ComboBox.check();
            n0TextField.requireEnabled(Timeout.timeout(2, TimeUnit.SECONDS)).setText(String.valueOf(n0));
            long startTime = System.currentTimeMillis();
            startButton.click().requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Auto:     N0 [{}] - {}", n0, stopTime - startTime);
            Pause.pause(3, TimeUnit.SECONDS);
        }
        LOGGER.debug("Auto: [ end multipleUseN0WithDifferentN0AutoTest ]");
    }

    @Test
    void multipleStartNormalAutoTest() {
        LOGGER.debug("Auto: [ start multipleStartNormalAutoTest ]");
        LOGGER.debug("Auto: multiple start normal iterations:");
        useN0ComboBox.check(false);
        for (int iter = 0; iter < 4; iter++) {
            long startTime = System.currentTimeMillis();
            startButton.click().requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Auto:     start [{}] - {}", iter, stopTime - startTime);
            Pause.pause(3, TimeUnit.SECONDS);
        }
        LOGGER.debug("Auto: [ end multipleStartNormalAutoTest ]");
    }

    @Test
    void multipleStartUseN0AutoTest() {
        LOGGER.debug("Auto: [ start multipleStartUseN0AutoTest ]");
        LOGGER.debug("Auto: multiple start useN0 iterations:");
        useN0ComboBox.check(true);
        for (int iter = 0; iter < 4; iter++) {
            long startTime = System.currentTimeMillis();
            startButton.click().requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Auto:     start [{}] - {}", iter, stopTime - startTime);
            Pause.pause(3, TimeUnit.SECONDS);
        }
        LOGGER.debug("Auto: [ end multipleStartUseN0AutoTest ]");
    }

    @Test
    void multipleStartWithUseN0AutoTest() {
        LOGGER.debug("Auto: [ start multipleStartWithUseN0AutoTest ]");
        LOGGER.debug("Auto: multiple start mix iterations:");
        for (int iter = 0; iter < 4; iter++) {
            useN0ComboBox.check(iter % 2 == 0);
            long startTime = System.currentTimeMillis();
            startButton.click().requireEnabled(Timeout.timeout(5, TimeUnit.MINUTES));
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Auto:     start useN0 [{}, {}] - {}", iter, iter % 2 == 0 ? "useN0" : "normal", stopTime - startTime);
            Pause.pause(3, TimeUnit.SECONDS);
        }
        LOGGER.debug("Auto: [ end multipleStartWithUseN0AutoTest ]");
    }

    private int setupRequestCount(int requestCount) {
        frame.tabbedPane(MainHelper.tabbedPane).selectTab(MainHelper.settingsTabTitle);
        JTextComponentFixture requestCountTextBox = frame.textBox(SettingsHelper.requestsCount);
        String oldRequestCount = requestCountTextBox.text();
        requestCountTextBox.setText(String.valueOf(requestCount));
        frame.button(SettingsHelper.save).click();
        frame.tabbedPane(MainHelper.tabbedPane).selectTab(MainHelper.autoTabTitle);
        LOGGER.debug("Auto: request count change {} -> {}", oldRequestCount, requestCount);
        return Integer.parseInt(oldRequestCount);
    }
}

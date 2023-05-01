package ui;

import gui.ComponentHelper.MainHelper;
import gui.ComponentHelper.SettingsHelper;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

class SettingsTabTest extends AbstractUiTest {

    private JTextComponentFixture requestCountTextField;
    private JButtonFixture refreshButton;
    private JButtonFixture saveButton;
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsTabTest.class);

    @Override
    protected void onSetUp() {
        super.onSetUp();
        frame.tabbedPane(MainHelper.tabbedPane).selectTab(MainHelper.settingsTabTitle);
        requestCountTextField = frame.textBox(SettingsHelper.requestsCount);
        refreshButton = frame.button(SettingsHelper.refresh);
        saveButton = frame.button(SettingsHelper.save);
    }

    @Override
    protected void onTearDown() {
        super.onTearDown();
        requestCountTextField = null;
        refreshButton = null;
        saveButton = null;
    }

    @Test
    void multipleSaveSettingsTest() {
        LOGGER.debug("Settings: [ start multipleSaveSettingsTest ]");
        LOGGER.debug("Settings: multiple save iterations:");
        for (int iter = 0; iter < 4; iter++) {
            long startTime = System.currentTimeMillis();
            saveButton.requireEnabled().click();
            if (iter < 2) Pause.pause(1, TimeUnit.SECONDS);
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Settings:     iter [{}] - {}", iter, stopTime - startTime);
        }
        LOGGER.debug("Settings: [ end multipleSaveSettingsTest ]");
    }

    @Test
    void multipleRefreshSettingsTest() {
        LOGGER.debug("Settings: [ start multipleRefreshSettingsTest ]");
        LOGGER.debug("Settings: multiple refresh iterations:");
        for (int iter = 0; iter < 4; iter++) {
            long startTime = System.currentTimeMillis();
            refreshButton.requireEnabled().click();
            if (iter < 2) Pause.pause(1, TimeUnit.SECONDS);
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Settings:     iter [{}] - {}", iter, stopTime - startTime);
        }
        LOGGER.debug("Settings: [ end multipleRefreshSettingsTest ]");
    }

    @Test
    void multipleSaveRefreshSettingsTest() {
        LOGGER.debug("Settings: [ start multipleSaveRefreshSettingsTest ]");
        LOGGER.debug("Settings: multiple save refresh iterations:");
        for (int iter = 0; iter < 4; iter++) {
            long startTime = System.currentTimeMillis();
            saveButton.requireEnabled().click();
            if (iter < 2) Pause.pause(1, TimeUnit.SECONDS);
            refreshButton.requireEnabled().click();
            if (iter < 2) Pause.pause(1, TimeUnit.SECONDS);
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Settings:     iter [{}] - {}", iter, stopTime - startTime);
        }
        LOGGER.debug("Settings: [ end multipleSaveRefreshSettingsTest ]");
    }

    @Test
    void multipleRequestCountSaveRefreshSettingsTest() {
        LOGGER.debug("Settings: [ start multipleRequestCountSaveRefreshSettingsTest ]");
        LOGGER.debug("Settings: multiple save refresh with request count change iterations:");
        int currentRequestCount = Integer.parseInt(requestCountTextField.text());
        int[] requestCountArray = {10, 100, 1000, currentRequestCount};
        for (int iter = 0; iter < 4; iter++) {
            int requestCount = requestCountArray[iter];
            requestCountTextField.setText(String.valueOf(requestCount));
            long startTime = System.currentTimeMillis();
            saveButton.requireEnabled().click();
            if (iter < 2) Pause.pause(1, TimeUnit.SECONDS);
            requestCountTextField.setText("0").requireText("0");
            refreshButton.requireEnabled().click();
            if (iter < 2) Pause.pause(1, TimeUnit.SECONDS);
            Pause.pause();
            requestCountTextField.requireText(String.valueOf(requestCount));
            long stopTime = System.currentTimeMillis();
            LOGGER.debug("Settings:     iter count [{}, {}] - {}", iter, requestCount, stopTime - startTime);
        }

        LOGGER.debug("Settings: [ end multipleRequestCountSaveRefreshSettingsTest ]");
    }
}

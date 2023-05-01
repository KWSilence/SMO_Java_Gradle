package ui;

import gui.MainGUI;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.edt.GuiQuery;
import org.assertj.swing.exception.ActionFailedException;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.testing.AssertJSwingTestCaseTemplate;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

abstract class AbstractUiTest extends AssertJSwingTestCaseTemplate {

    protected FrameFixture frame;
    protected static final boolean useWait = Boolean.parseBoolean(System.getenv("USE_WAIT"));
    private static boolean waitUsed = false;
    protected static final boolean useGC = Boolean.parseBoolean(System.getenv("USE_GC")) || System.getenv("USE_GC") == null;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUiTest.class);

    @BeforeEach
    public final void setUp() {
        LOGGER.debug("UITest: { start setUp }");
        super.setUpRobot();
        JFrame root = GuiActionRunner.execute(new GuiQuery<>() {
            @Override
            protected JFrame executeInEDT() {
                MainGUI mainGUI = new MainGUI(true);
                mainGUI.showWindow();
                return mainGUI.getRoot();
            }
        });
        frame = new FrameFixture(super.robot(), root);
        frame.show();
        frame.resizeTo(new Dimension(1000, 500));
        frame.moveTo(new Point(350,150));
        if (useWait && !waitUsed) {
            LOGGER.debug("UITest: Using wait once option");
            waitUntilFocus(2000);
            waitUsed = true;
            LOGGER.debug("UITest: Wait once option was used");
        }
        onSetUp();
        performGC();
        LOGGER.debug("UITest: { end setUp }");
    }

    protected void onSetUp() {
    }

    @AfterEach
    public final void tearDown() {
        LOGGER.debug("UITest: { start tearDown }");
        try {
            onTearDown();
            frame = null;
        } finally {
            super.cleanUp();
        }
        performGC();
        LOGGER.debug("UITest: { end tearDown }");
    }

    protected void onTearDown() {
    }

    private void performGC() {
        if (useGC) {
            LOGGER.debug("UITest: { Prepare to GC }");
            Pause.pause(3, TimeUnit.SECONDS);
            LOGGER.debug("UITest: { GC }");
            System.gc();
            Pause.pause(6, TimeUnit.SECONDS);
            LOGGER.debug("UITest: { GC was performed }");
        }
    }

    protected final void waitUntilFocus(long checkDelay) {
        frame.iconify();
        while (true) {
            Pause.pause(checkDelay, TimeUnit.MILLISECONDS);
            try {
                frame.click();
            } catch (ActionFailedException e) {
                continue;
            }
            break;
        }
    }
}

package client;

import me.furetur.concurrency4d.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

public class CommonLogging {
    private static final long TIMEOUT = 7000;
    protected Log log = new Log(this);
    protected Thread logDumper;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        log.debug("==================== TEST START " + testInfo.getDisplayName() + " ====================");
        log.debug("Starting test");
        logDumper = Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(TIMEOUT);
                Log.flush();
            } catch (InterruptedException e) {
                // it's ok
            }
        });
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        log.debug("Exiting test");
        log.debug("==================== TEST END " + testInfo.getDisplayName() + " ====================");
        logDumper.interrupt();
        Log.flush();
    }
}

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import smo_system.component.Request;
import smo_system.component.Source;

import static org.junit.jupiter.api.Assertions.*;

class SourceTest {

    @Test
    void testSourceConstructors() {
        int sourceNumber = 1;
        double lambda = 1.2;
        Source source = new Source(sourceNumber, lambda);
        assertEquals(sourceNumber, source.getNumber(), "source number is not set correctly on init");
        assertEquals(lambda, source.getLambda(), "source lambda is not set correctly on init");
        assertEquals(0, source.getRequestCount(), "source request count more than 0 on init");
        assertTrue(source.getTime() > 0, "source has no next request time");

        Source sourceCopy = new Source(source);
        CompareUtil.compareSources(source, sourceCopy);
        Request requestOrig1 = source.getRequestAndGenerate();
        Request requestCopy1 = sourceCopy.getRequestAndGenerate();
        CompareUtil.compareRequests(requestOrig1, requestCopy1);

        Source sourceCopyAfterGenerate = new Source(source);
        CompareUtil.compareSources(source, sourceCopyAfterGenerate);
        assertThrows(AssertionFailedError.class, () -> CompareUtil.compareSources(source, sourceCopy));
    }

    @Test
    void testGeneration() {
        int sourceNumber = 1;
        Source source = new Source(sourceNumber, 1.0);
        assertEquals(0, source.getRequestCount(), "source request count more than 0 on init");

        Request request = source.getRequestAndGenerate();
        assertNotNull(request, "source should generate request");
        assertEquals(sourceNumber, request.getSourceNumber(), "request source number is not equal to its source");
        assertEquals(0, request.getNumber(), "first request number is not 0");
        assertEquals(1, source.getRequestCount(), "request count is not increment after getting");

        Request request1 = source.getRequestAndGenerate();
        assertNotNull(request1, "source should generate request");
        assertEquals(sourceNumber, request1.getSourceNumber(), "request source number is not equal to its source");
        assertEquals(1, request1.getNumber(), "second request number is not 1");
        assertEquals(2, source.getRequestCount(), "request count is not increment after getting");
    }

    @Test
    void testGenerationTime() {
        Source source = new Source(1, 1.0);
        double time1 = source.getTime();

        assertEquals(0, source.getRequestCount(), "source request count more than 0 on init");
        Request request1 = source.getRequestAndGenerate();
        assertNotNull(request1, "source can not generate request");
        assertEquals(time1, request1.getTime());
        assertEquals(1, source.getRequestCount(), "request count is not increment after getting");

        double time2 = source.getTime();
        assertTrue(time1 < time2, "request time less than previous one");
        Request request2 = source.getRequestAndGenerate();
        assertNotNull(request2, "source can not generate request");
        assertEquals(time2, request2.getTime());
        assertEquals(2, source.getRequestCount(), "request count is not increment after getting");
    }
}

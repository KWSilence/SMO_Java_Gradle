import org.junit.jupiter.api.Test;
import smo_system.component.Request;
import smo_system.component.Source;

import static org.junit.jupiter.api.Assertions.*;

class SourceTest {
    @Test
    void testGeneration() {
        Source source = new Source(1, 1.0);
        assertEquals(1, source.getNumber());

        assertEquals(0, source.getRequestCount());
        Request request = source.getNewRequest();
        assertNotNull(request);
        assertEquals(1, request.getSourceNumber());
        assertEquals(0, request.getNumber());
        assertEquals(1, source.getRequestCount());
    }

    @Test
    void testGenerationTime() {
        Source source = new Source(1, 1.0);
        double time1 = source.getTime();

        assertEquals(0, source.getRequestCount());
        Request request1 = source.getNewRequest();
        assertNotNull(request1);
        assertEquals(time1, request1.getTime());
        assertEquals(1, source.getRequestCount());

        double time2 = source.getTime();
        assertTrue(time1 < time2);
        Request request2 = source.getNewRequest();
        assertNotNull(request2);
        assertEquals(time2, request2.getTime());
        assertEquals(2, source.getRequestCount());
    }

    @Test
    void testCopyConstructor() {
        Source source = new Source(1, 1.0);
        Source sourceCopy = new Source(source);

        assertEquals(source.getNumber(), sourceCopy.getNumber());
        assertEquals(source.getTime(), sourceCopy.getTime());
        assertEquals(source.getRequestCount(), sourceCopy.getRequestCount());

        Request requestOrig1 = source.getNewRequest();
        Request requestCopy1 = sourceCopy.getNewRequest();
        compareRequests(requestOrig1, requestCopy1);

        Source sourceCopyAfterGenerate = new Source(source);
        Request requestOrig2 = source.getNewRequest();
        Request requestCopy2 = sourceCopyAfterGenerate.getNewRequest();
        compareRequests(requestOrig2, requestCopy2);
    }

    private void compareRequests(Request expectedRequest, Request actualRequest) {
        assertEquals(expectedRequest.getSourceNumber(), actualRequest.getSourceNumber());
        assertEquals(expectedRequest.getNumber(), actualRequest.getNumber());
        assertEquals(expectedRequest.getTime(), actualRequest.getTime());
    }
}

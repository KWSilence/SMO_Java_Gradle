import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import system.component.Request;
import system.component.Source;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests related to Source
 **/
class SourceTest {

    /**
     * Test constructors:
     * Default - Source(int number, double lambda). Checking initial state (main fields equal to expected initial values).
     *     Checking invalid lambda (equal or less than 0) throws IllegalArgumentException.
     * Copy - Source(Source). Checking equality of main fields.
     * Fields to check: number, lambda, requestCount, time.
     **/
    @Test
    void testSourceConstructors() {
        // create source with invalid lambda (equal or less than 0)
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Source(0, -1),
                "Source with lambda <= 0 should throw exception"
        );
        assertEquals("Source lambda should be greater than 0", exception.getMessage());

        // create source using number and lambda
        int sourceNumber = 1;
        double lambda = 1.2;
        Source source = new Source(sourceNumber, lambda);
        // check initial state
        assertEquals(sourceNumber, source.getNumber(), "source number is not set correctly on init");
        assertEquals(lambda, source.getLambda(), "source lambda is not set correctly on init");
        assertEquals(0, source.getRequestCount(), "source request count more than 0 on init");
        assertTrue(source.getTime() > 0, "source has no next request time");

        // create source copy and compare
        Source sourceCopy = new Source(source);
        CompareUtil.compareSources(source, sourceCopy);

        // change source state with generation
        source.getRequestAndGenerate();

        // create source copy after changing state
        Source sourceCopyAfterGenerate = new Source(source);
        CompareUtil.compareSources(source, sourceCopyAfterGenerate);
        // check that previous copy has not changed after state change
        assertThrows(AssertionFailedError.class, () -> CompareUtil.compareSources(source, sourceCopy));
    }

    /**
     * Checking request generation function.
     * After request generation request count and request number should increase.
     * Request source number should equal to its generated number of source that generated it.
     **/
    @Test
    void testGeneration() {
        // create source with source number
        int sourceNumber = 1;
        Source source = new Source(sourceNumber, 1.0);
        assertEquals(0, source.getRequestCount(), "source request count more than 0 on init");

        // generate request and check increase of request count and request number field, request source number
        Request request = source.getRequestAndGenerate();
        assertNotNull(request, "source should generate request");
        assertEquals(sourceNumber, request.getSourceNumber(), "request source number is not equal to its source");
        assertEquals(0, request.getNumber(), "first request number is not 0");
        assertEquals(1, source.getRequestCount(), "request count is not increment after getting");

        // same checks with second request generation
        Request request1 = source.getRequestAndGenerate();
        assertNotNull(request1, "source should generate request");
        assertEquals(sourceNumber, request1.getSourceNumber(), "request source number is not equal to its source");
        assertEquals(1, request1.getNumber(), "second request number is not 1");
        assertEquals(2, source.getRequestCount(), "request count is not increment after getting");
    }

    /**
     * Checking time generation of requests in source.
     * Previous request time should be less than current time
     **/
    @Test
    void testGenerationTime() {
        // create source
        Source source = new Source(1, 1.0);
        // get time of first request
        double time1 = source.getTime();

        assertEquals(0, source.getRequestCount(), "source request count more than 0 on init");
        // get first request and generate second request
        Request request1 = source.getRequestAndGenerate();
        // check first request time and request count
        assertNotNull(request1, "source can not generate request");
        assertEquals(time1, request1.getTime(), "request time should be same as source getTime method returns");
        assertEquals(1, source.getRequestCount(), "request count is not increment after getting");

        // get time of second request
        double time2 = source.getTime();
        // compare requests time
        assertTrue(time1 < time2, "request time less than previous one");
        // get second request and generate new request
        Request request2 = source.getRequestAndGenerate();
        // check second request time and request count
        assertNotNull(request2, "source can not generate request");
        assertEquals(time2, request2.getTime(), "request time should be same as source getTime method returns");
        assertEquals(2, source.getRequestCount(), "request count is not increment after getting");
    }
}

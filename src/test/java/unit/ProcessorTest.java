package unit;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import system.component.Processor;
import system.component.Request;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests related to Processor
 **/
class ProcessorTest {

    /**
     * Test constructors:
     * Default - Processor(int number, double lambda). Checking initial state (main fields equal to expected initial values).
     *     Checking invalid lambda (equal or less than 0) throws IllegalArgumentException.
     * Copy - Processor(Processor). Checking equality of main fields.
     * Fields to check: processorNumber, lambda, isWait, request, processTime, workTime.
     **/
    @Test
    void testProcessorConstructors() {
        // create processor with invalid lambda (equal or less than 0)
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Processor(0, -1),
                "Processor with lambda <= 0 should throw exception"
        );
        assertEquals("Processor lambda should be greater than 0", exception.getMessage());

        // declare main args form default constructor
        int processorNumber = 1;
        double lambda = 1.3;
        // create processor with declared args
        Processor processor = new Processor(processorNumber, lambda);
        // check initial state of processor, processor fields equal to default
        assertEquals(processorNumber, processor.getNumber(), "processor number is not set correctly on init");
        assertEquals(lambda, processor.getLambda(), "processor lambda is not set correctly on init");
        assertTrue(processor.isWait(), "processor should wait on init");
        assertNull(processor.getRequest(), "processor should not contain request on init");
        assertEquals(0, processor.getProcessTime(), "processor process time should be 0 on init");
        assertEquals(0, processor.getWorkTime(), "processor work time should be 0 on init");

        // copy initial processor
        Processor processorCopy = new Processor(processor);
        // compare processors fields
        CompareUtil.compareProcessors(processor, processorCopy);

        // actions to change state of processor
        Request request1 = new Request(1, 1, 1);
        Request request2 = new Request(2, 3, 4);
        processor.process(request1);
        processor.free();
        processor.process(request2);

        // copy processor with changed state
        Processor processorAfterActions = new Processor(processor);
        CompareUtil.compareProcessors(processor, processorAfterActions);
        // check that previous copy has not changed after state change
        assertThrows(AssertionFailedError.class, () -> CompareUtil.compareProcessors(processor, processorCopy));
    }

    /**
     * Checking for free Processor call 'free' method and null request processing.
     * Calling 'free' for free processor should return null. Work time and process time should not be changed.
     * Calling 'process' for null request should return false. Wait flag, work time and process time should not be changed.
     **/
    @Test
    void testFreeEmptyProcessor() {
        int processorNumber = 1;
        double lambda = 1.5;
        // create processor with declared args
        Processor processor = new Processor(processorNumber, lambda);
        // check wait flag for processor, flag should be true (free processor)
        assertTrue(processor.isWait());
        // 'free' processor return null for free processor
        assertNull(processor.free(), "processor should not contain request -> can not free");
        // work time and process time should not be changed
        assertEquals(0, processor.getWorkTime());
        assertEquals(0, processor.getProcessTime());

        // 'process' for null request return false
        assertFalse(processor.process(null), "processor should not process null request");
        // wait flag, work time and process time should not be changed
        assertTrue(processor.isWait());
        assertEquals(0, processor.getWorkTime());
        assertEquals(0, processor.getProcessTime());
    }

    /**
     * Checking processor state change while processing request.
     * After calling 'process' return true and should change only request (request to process),
     *   processTime (add lambda to request take time) and wait (false) fields.
     * After calling 'free' return request (should equal to initial request pointer)
     *   and should change wait flag (true), workTime (add lambda)
     **/
    @Test
    void testRequestStateChangeAfterProcessing() {
        // create processor with declared lambda
        double lambda = 1.5;
        Processor processor = new Processor(1, lambda);

        // create request with creation time and buffer time
        double request1CreationTime = 4;
        double request1TimeInBuffer = 1;
        Request request1 = new Request(2, 3, 4);
        // check correct creation time
        assertEquals(request1CreationTime, request1.getTime());
        request1.setTimeInBuffer(request1TimeInBuffer);
        // check correct time in buffer
        assertEquals(request1TimeInBuffer, request1.getTimeInBuffer());

        // the same actions as it was in the request1
        double request2CreationTime = 5;
        double request2TimeInBuffer = 2;
        Request request2 = new Request(3, 4, request2CreationTime);
        assertEquals(request2CreationTime, request2.getTime());
        request2.setTimeInBuffer(request2TimeInBuffer);
        assertEquals(request2TimeInBuffer, request2.getTimeInBuffer());

        // create requests array
        Request[] requests = new Request[]{request1, request2};

        // for each request in requests array
        for (Request request: requests) {
            // get workTime before processing
            double workTime = processor.getWorkTime();
            // 'process' should return on request
            assertTrue(processor.process(request), "free processor can start process");
            // assert changes only for request and isWait (to false), other fields keep their initial values
            assertEquals(workTime, processor.getWorkTime(), "when processor take request work time should not be changed");
            assertEquals(0, request.getTimeInProcessor(), "request process time should not be changed on process start");
            assertEquals(request, processor.getRequest(), "requests pointers should by same");
            // check new process time (increment request take time by lambda)
            assertEquals(request.getTime() + request.getTimeInBuffer() + lambda, processor.getProcessTime());
            // wait flag -> false (processor processes request)
            assertFalse(processor.isWait(), "processor should switch wait flag when start process");

            // 'free' should return processed request
            Request processedRequest = processor.free();
            // request and processed request should have same painter
            assertEquals(request, processedRequest, "processed and initial request should have same pointer");
            // set time in processor = lambda
            assertEquals(lambda, request.getTimeInProcessor(), "processed and initial request should have same pointer");
            // wait flag -> true (free processor)
            assertTrue(processor.isWait(), "processor should switch wait flag after free");
            assertEquals(workTime + lambda, processor.getWorkTime(), "processor should increase work time after free");
            // request lifeTime should be increased by lambda
            assertEquals(request.getTimeInBuffer() + lambda, request.getLifeTime());
        }
    }

    /**
     * Check that the processor cannot process multiple requests before the release.
     * Calling 'process' again without 'free' will not change state of processor or request.
     **/
    @Test
    void testMultipleRequestProcessing() {
        // create processor with lambda
        double lambda = 1.5;
        Processor processor = new Processor(1, lambda);

        // create request with creation time and time in buffer
        double requestCreationTime = 2;
        double requestTimeInBuffer = 3;
        Request request = new Request(2, 3, requestCreationTime);
        request.setTimeInBuffer(requestTimeInBuffer);
        assertEquals(requestTimeInBuffer, request.getTimeInBuffer());

        // process should return true for free processor
        assertTrue(processor.process(request), "free processor can start process");
        // save current process time
        double processTime = processor.getProcessTime();
        assertEquals(request, processor.getRequest(), "requests pointers should by same");
        assertFalse(processor.isWait(), "processor should switch wait flag when start process");

        // create another request and process it
        Request request1 = new Request(3, 4, requestCreationTime);
        assertFalse(processor.process(request1), "working processor should not take new request until free");
        assertNotEquals(request1, processor.getRequest(), "second request should not be in processor");
        assertEquals(0, request.getTimeInProcessor(), "request process time should not be changed on process start");
        assertFalse(processor.isWait(), "processor should switch wait flag when start process");
        // process time is not changed
        assertEquals(processTime, processor.getProcessTime(), "process time should not be changed after second process call");

        // free processor and processed request pointer should equal to first request
        Request processedRequest = processor.free();
        assertEquals(request, processedRequest, "processed and initial request should have same pointer");
    }
}

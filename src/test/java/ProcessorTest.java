import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import smo_system.component.Processor;
import smo_system.component.Request;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorTest {

    @Test
    void testProcessorConstructors() {
        int processorNumber = 1;
        double lambda = 1.3;
        Processor processor = new Processor(processorNumber, lambda);
        assertEquals(processorNumber, processor.getNumber(), "processor number is not set correctly on init");
        assertEquals(lambda, processor.getLambda(), "processor lambda is not set correctly on init");
        assertTrue(processor.isWait(), "processor should wait on init");
        assertNull(processor.getRequest(), "processor should not contain request on init");
        assertEquals(0, processor.getProcessTime(), "processor process time should be 0 on init");
        assertEquals(0, processor.getWorkTime(), "processor work time should be 0 on init");

        Processor processorCopy = new Processor(processor);
        CompareUtil.compareProcessors(processor, processorCopy);

        Request request1 = new Request(1, 1, 1);
        Request request2 = new Request(2, 3, 4);
        processor.process(request1);
        processor.free();
        processor.process(request2);
        Processor processorAfterActions = new Processor(processor);
        CompareUtil.compareProcessors(processor, processorAfterActions);
        assertThrows(AssertionFailedError.class, () -> CompareUtil.compareProcessors(processor, processorCopy));
    }

    @Test
    void testFreeEmptyProcessor() {
        int processorNumber = 1;
        double lambda = 1.5;
        Processor processor = new Processor(processorNumber, lambda);
        assertTrue(processor.isWait());
        assertNull(processor.free(), "processor should not contain request -> can not free");
        assertEquals(0, processor.getWorkTime());
        assertEquals(0, processor.getProcessTime());

        assertFalse(processor.process(null), "processor should not process null request");
        assertTrue(processor.isWait());
        assertEquals(0, processor.getWorkTime());
        assertEquals(0, processor.getProcessTime());
    }

    @Test
    void testRequestStateChangeAfterProcessing() {
        double lambda = 1.5;
        Processor processor = new Processor(1, lambda);

        double request1CreationTime = 4;
        double request1TimeInBuffer = 1;
        Request request1 = new Request(2, 3, 4);
        // check correct creation time
        assertEquals(request1CreationTime, request1.getTime());
        request1.setTimeInBuffer(request1TimeInBuffer);
        assertEquals(request1TimeInBuffer, request1.getTimeInBuffer());

        double request2CreationTime = 5;
        double request2TimeInBuffer = 2;
        Request request2 = new Request(3, 4, request2CreationTime);
        assertEquals(request2CreationTime, request2.getTime());
        request2.setTimeInBuffer(request2TimeInBuffer);
        assertEquals(request2TimeInBuffer, request2.getTimeInBuffer());

        Request[] requests = new Request[]{request1, request2};

        for (Request request: requests) {
            double workTime = processor.getWorkTime();
            assertTrue(processor.process(request), "free processor can start process");
            assertEquals(workTime, processor.getWorkTime(), "when processor take request work time should not be changed");
            assertEquals(0, request.getTimeInProcessor(), "request process time should not be changed on process start");
            assertEquals(request, processor.getRequest(), "requests pointers should by same");
            assertEquals(request.getTime() + request.getTimeInBuffer() + lambda, processor.getProcessTime());
            assertFalse(processor.isWait(), "processor should switch wait flag when start process");

            Request processedRequest = processor.free();
            assertEquals(request, processedRequest, "processed and initial request should have same pointer");
            assertEquals(lambda, request.getTimeInProcessor(), "processed and initial request should have same pointer");
            assertTrue(processor.isWait(), "processor should switch wait flag after free");
            assertEquals(workTime + lambda, processor.getWorkTime(), "processor should increase work time after free");
            assertEquals(request.getTimeInBuffer() + lambda, request.getLifeTime());
        }
    }

    @Test
    void testMultipleRequestProcessing() {
        double lambda = 1.5;
        Processor processor = new Processor(1, lambda);

        double requestCreationTime = 2;
        double requestTimeInBuffer = 3;
        Request request = new Request(2, 3, requestCreationTime);
        request.setTimeInBuffer(requestTimeInBuffer);
        assertEquals(requestTimeInBuffer, request.getTimeInBuffer());

        assertTrue(processor.process(request), "free processor can start process");
        double processTime = processor.getProcessTime();
        assertEquals(request, processor.getRequest(), "requests pointers should by same");
        assertFalse(processor.isWait(), "processor should switch wait flag when start process");

        Request request1 = new Request(3, 4, requestCreationTime);
        assertFalse(processor.process(request1), "working processor should not take new request until free");
        assertNotEquals(request1, processor.getRequest(), "second request should not be in processor");
        assertEquals(0, request.getTimeInProcessor(), "request process time should not be changed on process start");
        assertFalse(processor.isWait(), "processor should switch wait flag when start process");
        assertEquals(processTime, processor.getProcessTime(), "process time should not be changed after second process call");

        Request processedRequest = processor.free();
        assertEquals(request, processedRequest, "processed and initial request should have same pointer");
    }
}

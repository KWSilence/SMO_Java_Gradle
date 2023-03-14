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
        Request request1 = new Request(1, 1, 1);
        Request request2 = new Request(2, 3, 4);
        Processor processor = new Processor(processorNumber, lambda);
        assertEquals(processorNumber, processor.getNumber(), "processor number is not set correctly on init");
        assertEquals(lambda, processor.getLambda(), "processor lambda is not set correctly on init");
        assertTrue(processor.isWait(), "processor should wait on init");
        assertNull(processor.getRequest(), "processor should not contain request on init");
        assertEquals(0, processor.getProcessTime(), "processor process time should be 0 on init");
        assertEquals(0, processor.getWorkTime(), "processor work time should be 0 on init");

        Processor processorCopy = new Processor(processor);
        CompareUtil.compareProcessors(processor, processorCopy);

        processor.process(request1);
        processor.free();
        processor.process(request2);
        Processor processorAfterActions = new Processor(processor);
        CompareUtil.compareProcessors(processor, processorAfterActions);
        assertThrows(AssertionFailedError.class, () -> CompareUtil.compareProcessors(processor, processorCopy));
    }

    @Test
    void testFreeEmptyProcessor() {
        double lambda = 1.5;
        Processor processor = new Processor(1, lambda);
        assertNull(processor.free(), "processor should not contain request -> can not free");
        assertEquals(0, processor.getWorkTime());
        assertEquals(0, processor.getProcessTime());
        assertFalse(processor.process(null), "processor should not process null request");
        assertEquals(0, processor.getProcessTime());
    }

    @Test
    void testRequestStateChangeAfterProcessing() {
        double lambda = 1.5;
        double requestCreationTime = 2;
        double requestTimeInBuffer = 3;
        Processor processor = new Processor(1, lambda);

        Request request = new Request(2, 3, requestCreationTime);
        request.setTimeInBuffer(requestTimeInBuffer);
        assertEquals(requestTimeInBuffer, request.getTimeInBuffer());
        assertTrue(processor.process(request), "free processor can start process");
        assertEquals(0, processor.getWorkTime(), "when processor take request work time should not be changed");
        assertEquals(0, request.getTimeInProcessor(), "request process time should not be changed on process start");
        assertEquals(request, processor.getRequest(), "requests pointers should by same");
        assertEquals(requestCreationTime + requestTimeInBuffer + lambda, processor.getProcessTime());
        assertFalse(processor.isWait(), "processor should switch wait flag when start process");

        Request processedRequest = processor.free();
        assertEquals(request, processedRequest, "processed and initial request should have same pointer");
        assertEquals(lambda, request.getTimeInProcessor(), "processed and initial request should have same pointer");
        assertTrue(processor.isWait(), "processor should switch wait flag after free");
        assertEquals(lambda, processor.getWorkTime(), "processor should increase work time after free");
        assertEquals(requestTimeInBuffer + lambda, request.getLifeTime());
    }

    @Test
    void testMultipleRequestProcessing() {
        double lambda = 1.5;
        double requestCreationTime = 2;
        double requestTimeInBuffer = 3;
        Processor processor = new Processor(1, lambda);

        Request request = new Request(2, 3, requestCreationTime);
        request.setTimeInBuffer(requestTimeInBuffer);
        assertEquals(requestTimeInBuffer, request.getTimeInBuffer());
        assertTrue(processor.process(request), "free processor can start process");
        assertEquals(request, processor.getRequest(), "requests pointers should by same");
        assertFalse(processor.isWait(), "processor should switch wait flag when start process");

        Request request1 = new Request(3, 4, requestCreationTime);
        assertFalse(processor.process(request1), "working processor should not take new request until free");
        assertNotEquals(request1, processor.getRequest());
        assertEquals(0, request.getTimeInProcessor(), "request process time should not be changed on process start");
        assertFalse(processor.isWait(), "processor should switch wait flag when start process");

        Request processedRequest = processor.free();
        assertEquals(request, processedRequest, "processed and initial request should have same pointer");
    }
}

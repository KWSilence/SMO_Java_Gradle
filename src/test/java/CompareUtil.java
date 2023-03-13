import smo_system.component.Buffer;
import smo_system.component.Processor;
import smo_system.component.Request;
import smo_system.component.Source;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompareUtil {
    private CompareUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static void compareRequests(Request expectedRequest, Request actualRequest) {
        if (expectedRequest != null && actualRequest != null) {
            assertEquals(expectedRequest.getSourceNumber(), actualRequest.getSourceNumber());
            assertEquals(expectedRequest.getNumber(), actualRequest.getNumber());
            assertEquals(expectedRequest.getTime(), actualRequest.getTime());
            assertEquals(expectedRequest.getTimeInBuffer(), actualRequest.getTimeInBuffer());
            assertEquals(expectedRequest.getTimeInProcessor(), actualRequest.getTimeInProcessor());
            assertEquals(expectedRequest.getLifeTime(), actualRequest.getLifeTime());
        }
    }

    public static void compareSources(Source expectedSource, Source actualSource) {
        assertEquals(expectedSource.getNumber(), actualSource.getNumber());
        assertEquals(expectedSource.getLambda(), actualSource.getLambda());
        assertEquals(expectedSource.getTime(), actualSource.getTime());
        assertEquals(expectedSource.getRequestCount(), actualSource.getRequestCount());
        compareRequests(expectedSource.getRequestCopy(), actualSource.getRequestCopy());
    }

    public static void compareSourcesWithoutRandom(Source expectedSource, Source actualSource) {
        assertEquals(expectedSource.getNumber(), actualSource.getNumber());
        assertEquals(expectedSource.getLambda(), actualSource.getLambda());
        assertEquals(expectedSource.getRequestCount(), actualSource.getRequestCount());
    }

    public static void compareBuffers(Buffer expectedBuffer, Buffer actualBuffer) {
        assertEquals(expectedBuffer.getCapacity(), actualBuffer.getCapacity());
        assertEquals(expectedBuffer.getSize(), actualBuffer.getSize());
        assertEquals(expectedBuffer.isEmpty(), actualBuffer.isEmpty());
        assertEquals(expectedBuffer.isFull(), actualBuffer.isFull());
        assertEquals(expectedBuffer.getTakeIndex(), actualBuffer.getTakeIndex());
        compareLists(expectedBuffer.getList(), actualBuffer.getList(), CompareUtil::compareRequests);
        compareLists(expectedBuffer.getRequestsPackage(), actualBuffer.getRequestsPackage(), CompareUtil::compareRequests);
    }

    public static void compareProcessors(Processor expectedProcessor, Processor actualProcessor) {
        assertEquals(expectedProcessor.getNumber(), actualProcessor.getNumber());
        assertEquals(expectedProcessor.getLambda(), actualProcessor.getLambda());
        assertEquals(expectedProcessor.isWait(), actualProcessor.isWait());
        assertEquals(expectedProcessor.getProcessTime(), actualProcessor.getProcessTime());
        assertEquals(expectedProcessor.getWorkTime(), actualProcessor.getWorkTime());
        Request expectedRequest = expectedProcessor.getRequest();
        Request actualRequest = actualProcessor.getRequest();
        compareRequests(expectedRequest, actualRequest);
    }

    @FunctionalInterface
    public interface Comparator<T> {
        void compare(T expected, T actual);
    }

    public static <T> void compareLists(List<T> expectedList, List<T> actualList, Comparator<T> comparator) {
        assertEquals(expectedList.size(), actualList.size());
        for (int i = 0; i < expectedList.size(); ++i) {
            T expectedElement = expectedList.get(i);
            T actualElement = actualList.get(i);
            comparator.compare(expectedElement, actualElement);
        }
    }
}

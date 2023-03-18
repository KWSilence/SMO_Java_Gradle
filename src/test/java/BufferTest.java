import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import smo_system.component.Buffer;
import smo_system.component.Request;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BufferTest {

    @Test
    void testBufferConstructors() {
        int bufferCapacity = 4;
        Buffer buffer = new Buffer(bufferCapacity);
        List<Request> requestsToPut = List.of(
                new Request(0, 0, 1),
                new Request(1, 2, 5),
                new Request(1, 0, 6)
        );

        assertEquals(bufferCapacity, buffer.getCapacity(), "buffer capacity is not set correctly on init");
        assertEquals(0, buffer.getSize(), "buffer size more than 0 on init");
        assertTrue(buffer.isEmpty(), "buffer is not empty on init");
        assertFalse(buffer.isFull(), "buffer is full on init");
        assertEquals(-1, buffer.getTakeIndex(), "buffer has take index on init");
        assertTrue(buffer.getList().isEmpty(), "buffer list is not empty on init");
        assertTrue(buffer.getRequestsPackage().isEmpty(), "buffer package list is not empty on init");

        Buffer bufferCopy = new Buffer(buffer);
        CompareUtil.compareBuffers(buffer, bufferCopy);

        requestsToPut.forEach(buffer::putRequest);
        buffer.createPackage();
        Buffer bufferCopyWithRequests = new Buffer(buffer);
        CompareUtil.compareBuffers(buffer, bufferCopyWithRequests);
        assertTrue(
                bufferCopyWithRequests.getList().containsAll(bufferCopyWithRequests.getRequestsPackage()),
                "requests list should contain requests package items"
        );
        assertThrows(AssertionFailedError.class, () -> CompareUtil.compareBuffers(buffer, bufferCopy));
    }

    @Test
    void testOverflow() {
        Buffer buffer = new Buffer(2);
        assertEquals(0, buffer.getSize(), "initial buffer size should be 0");
        assertTrue(buffer.isEmpty(), "buffer after creation should be empty");
        assertFalse(buffer.isFull(), "buffer after creation should not be full");
        assertTrue(buffer.getList().isEmpty(), "buffer list after creation should be empty");
        assertTrue(buffer.getRequestsPackage().isEmpty(), "buffer package list after creation should be empty");

        Request request1 = new Request(0, 1, 1);
        assertTrue(buffer.putRequest(request1), "buffer should return true when request put into not full buffer");
        assertEquals(1, buffer.getSize(), "buffer size should increase");
        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertTrue(buffer.getList().contains(request1), "buffer should add request when it is not full");

        Request request2 = new Request(0, 2, 2);
        assertTrue(buffer.putRequest(request2), "buffer should return true when request put into not full buffer");
        assertEquals(2, buffer.getSize(), "buffer size should increase");
        assertFalse(buffer.isEmpty());
        assertTrue(buffer.isFull());
        assertTrue(buffer.getList().contains(request2), "buffer should add request when it is not full");

        Request request3 = new Request(0, 3, 3);
        assertFalse(buffer.putRequest(request3), "buffer should return false when request put into full buffer");
        assertNotEquals(3, buffer.getSize(), "buffer size should not increase when it is full");
        assertFalse(buffer.isEmpty());
        assertTrue(buffer.isFull());
        assertFalse(buffer.getList().contains(request3), "buffer should not add request to list when it is full");
    }

    @Test
    void testPackageCreation() {
        Buffer buffer = new Buffer(4);
        Map<Request, Boolean> requestsToTest = Map.of(
                new Request(0, 1, 1.0), false,
                new Request(0, 0, 2.0), true,
                new Request(0, 2, 3.0), false,
                new Request(1, 0, 4.0), true
        );

        assertTrue(buffer.getList().isEmpty(), "initially buffer list should be empty");
        assertDoesNotThrow(buffer::createPackage);
        assertTrue(buffer.getRequestsPackage().isEmpty(), "package should not create with empty list");

        requestsToTest.forEach((request, toPackage) -> buffer.putRequest(request));
        buffer.createPackage();

        requestsToTest.forEach((request, toPackageFlag) -> {
            String requestStr = request.getSourceNumber() + "." + request.getNumber();
            assertEquals(
                    toPackageFlag,
                    buffer.getRequestsPackage().contains(request),
                    "package is not created by request " + requestStr
            );
            assertTrue(
                    buffer.getList().contains(request),
                    "list is not contain package " + request.getSourceNumber()
            );
        });
    }

    @Test
    void testTakeRequest() {
        Request request1 = new Request(0, 2, 1.0);
        Request request2 = new Request(0, 0, 2.0);
        Request request3 = new Request(0, 1, 3.0);
        Request request4 = new Request(1, 0, 4.0);
        List<Request> requestsToPut = List.of(request1, request2, request3, request4);
        List<Request> requestsToTake = List.of(request2, request4, request3, request1);
        List<Integer> takeIndexes = List.of(1, 2, 1, 0);

        Buffer buffer = new Buffer(4);
        assertNull(buffer.takeRequest());

        requestsToPut.forEach(buffer::putRequest);
        CompareUtil.compareLists(buffer.getList(), requestsToPut, Assertions::assertEquals);
        for (int i = 0; i < buffer.getCapacity(); ++i) {
            Request expectedTakenRequest = requestsToTake.get(i);
            Request actualTakenRequest = buffer.takeRequest();
            assertEquals(expectedTakenRequest, actualTakenRequest);

            int expectedTakeIndex = takeIndexes.get(i);
            int actualTakeIndex = buffer.getTakeIndex();
            assertEquals(expectedTakeIndex, actualTakeIndex);
        }
    }
}

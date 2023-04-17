import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import system.component.Buffer;
import system.component.Request;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests related to Buffer
 **/
class BufferTest {

    /**
     * Test constructors:
     * Default - Buffer(int capacity). Checking initial state (main fields equal to expected initial values).
     *     Checking invalid capacity (less than 1) throws IllegalArgumentException.
     * Copy - Buffer(Buffer). Checking equality of main fields.
     * Fields to check: capacity, size, empty/full, takeIndex, list, requestsPackage.
     **/
    @Test
    void testBufferConstructors() {
        // create buffer with invalid capacity (less than 1)
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Buffer(-1),
                "Buffer with capacity < 1 should throw exception"
        );
        assertEquals("Buffer capacity should be greater than 0", exception.getMessage());

        // create buffer with capacity
        int bufferCapacity = 4;
        Buffer buffer = new Buffer(bufferCapacity);
        // create request list to put in the buffer
        List<Request> requestsToPut = List.of(
                new Request(0, 0, 1),
                new Request(1, 2, 5),
                new Request(1, 0, 6)
        );

        // assert main fields
        assertEquals(bufferCapacity, buffer.getCapacity(), "buffer capacity is not set correctly on init");
        assertEquals(0, buffer.getSize(), "buffer size more than 0 on init");
        assertTrue(buffer.isEmpty(), "buffer is not empty on init");
        assertFalse(buffer.isFull(), "buffer is full on init");
        assertEquals(-1, buffer.getTakeIndex(), "buffer has take index on init");
        assertTrue(buffer.getList().isEmpty(), "buffer list is not empty on init");
        assertTrue(buffer.getRequestsPackage().isEmpty(), "buffer package list is not empty on init");

        // create buffer copy
        Buffer bufferCopy = new Buffer(buffer);
        // assert main fields
        CompareUtil.compareBuffers(buffer, bufferCopy);

        // change state of buffer by putting requests and creating package
        requestsToPut.forEach(buffer::putRequest);
        buffer.createPackage();

        // create buffer copy after changing state
        Buffer bufferCopyWithRequests = new Buffer(buffer);
        // compare buffer and its copy
        CompareUtil.compareBuffers(buffer, bufferCopyWithRequests);
        // list should contain all package requests
        assertTrue(
                bufferCopyWithRequests.getList().containsAll(bufferCopyWithRequests.getRequestsPackage()),
                "requests list should contain requests package items"
        );
        // check that previous copy has not changed after state change
        assertThrows(AssertionFailedError.class, () -> CompareUtil.compareBuffers(buffer, bufferCopy));
    }

    /**
     * Check buffer overflow.
     * Initially buffer size = 0, buffer and its lists is empty.
     * If call 'put' to null Request returns false and state does not change.
     * Buffer size should change after putting request to buffer.
     * When the buffer size reaches capacity, the flag full = true.
     * Next call 'put' returns false, the state of the buffer does not change.
     **/
    @Test
    void testOverflow() {
        // create buffer with capacity
        int bufferCapacity = 2;
        Buffer buffer = new Buffer(bufferCapacity);
        // check initial state of buffer
        assertEquals(0, buffer.getSize(), "initial buffer size should be 0");
        assertTrue(buffer.isEmpty(), "buffer after creation should be empty");
        assertFalse(buffer.isFull(), "buffer after creation should not be full");
        assertTrue(buffer.getList().isEmpty(), "buffer list after creation should be empty");
        assertTrue(buffer.getRequestsPackage().isEmpty(), "buffer package list after creation should be empty");

        // put null request to buffer
        assertFalse(buffer.putRequest(null));
        // check state is not changed after putting null Request
        CompareUtil.compareBuffers(new Buffer(bufferCapacity), buffer);

        // put first request to buffer
        Request request1 = new Request(0, 1, 1);
        assertTrue(buffer.putRequest(request1), "buffer should return true when request put into not full buffer");
        assertEquals(1, buffer.getSize(), "buffer size should increase");
        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertTrue(buffer.getList().contains(request1), "buffer should add request when it is not full");

        // put second request to buffer and reach buffer capacity
        Request request2 = new Request(0, 2, 2);
        assertTrue(buffer.putRequest(request2), "buffer should return true when request put into not full buffer");
        assertEquals(2, buffer.getSize(), "buffer size should increase");
        assertFalse(buffer.isEmpty());
        assertTrue(buffer.isFull());
        assertTrue(buffer.getList().contains(request2), "buffer should add request when it is not full");

        // put extra request when buffer capacity is reached
        Request request3 = new Request(0, 3, 3);
        assertFalse(buffer.putRequest(request3), "buffer should return false when request put into full buffer");
        assertNotEquals(3, buffer.getSize(), "buffer size should not increase when it is full");
        assertFalse(buffer.isEmpty());
        assertTrue(buffer.isFull());
        assertFalse(buffer.getList().contains(request3), "buffer should not add request to list when it is full");
    }

    /**
     * Checking the package creation function.
     * Creating package when buffer is empty should not throw exception.
     * Creating package must depend on priority of source (smaller source number has higher priority).
     **/
    @Test
    void testPackageCreation() {
        // create buffer with capacity
        Buffer buffer = new Buffer(4);
        // create requests map (request to put, flag future package creation)
        Map<Request, Boolean> requestsToTest = Map.of(
                new Request(0, 1, 1.0), false,
                new Request(0, 0, 2.0), true,
                new Request(0, 2, 3.0), false,
                new Request(1, 0, 4.0), true
        );

        // check package creation when buffer is empty
        assertTrue(buffer.getList().isEmpty(), "initially buffer list should be empty");
        assertDoesNotThrow(buffer::createPackage);
        assertTrue(buffer.getRequestsPackage().isEmpty(), "package should not create with empty list");

        // put requests from requestsToTest to buffer and create package
        requestsToTest.forEach((request, toPackage) -> buffer.putRequest(request));
        buffer.createPackage();

        // for each request in requestsToTest
        requestsToTest.forEach((request, toPackageFlag) -> {
            String requestStr = request.getSourceNumber() + "." + request.getNumber();
            // check package list creation by flag in map
            assertEquals(
                    toPackageFlag,
                    buffer.getRequestsPackage().contains(request),
                    "package is not created by request " + requestStr
            );
            // check that requests list contains putted request
            assertTrue(
                    buffer.getList().contains(request),
                    "list is not contain package " + request.getSourceNumber()
            );
        });
    }

    /**
     * Check takeRequest and getTakeIndex functions of buffer.
     * Check return null request when takeRequest from empty buffer.
     * Check correct order of taking request that depends on source priority.
     * Notice: takeRequest will create package if it is empty.
     **/
    @Test
    void testTakeRequest() {
        // create requests with different source numbers, at least one package must have size grater than 1
        Request request1 = new Request(0, 2, 1.0);
        Request request2 = new Request(0, 0, 2.0);
        Request request3 = new Request(0, 1, 3.0);
        Request request4 = new Request(1, 0, 4.0);
        // order of requests to put in buffer
        List<Request> requestsToPut = List.of(request1, request2, request3, request4);
        // order of requests to take from buffer
        List<Request> requestsToTake = List.of(request2, request4, request3, request1);
        // order of take indexes
        List<Integer> takeIndexes = List.of(1, 2, 1, 0);

        // create buffer with capacity
        Buffer buffer = new Buffer(4);
        // take of null request form empty buffer
        assertNull(buffer.takeRequest());

        // put requests from requestsToPut list
        requestsToPut.forEach(buffer::putRequest);
        // check correct order after put
        CompareUtil.compareLists(buffer.getList(), requestsToPut, Assertions::assertEquals);
        for (int i = 0; i < buffer.getCapacity(); ++i) {
            // check take request
            Request expectedTakenRequest = requestsToTake.get(i);
            Request actualTakenRequest = buffer.takeRequest();
            assertEquals(expectedTakenRequest, actualTakenRequest);

            // check take index
            int expectedTakeIndex = takeIndexes.get(i);
            int actualTakeIndex = buffer.getTakeIndex();
            assertEquals(expectedTakeIndex, actualTakeIndex);
        }
    }
}

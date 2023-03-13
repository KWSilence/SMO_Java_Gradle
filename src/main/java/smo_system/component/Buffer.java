package smo_system.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Buffer {
    private final int capacity;
    private final List<Request> list;
    private final List<Request> requestsPackage;
    private int takeIndex;

    public Buffer(int capacity) {
        this.capacity = capacity;
        this.list = new ArrayList<>(capacity);
        this.requestsPackage = new ArrayList<>(capacity);
        this.takeIndex = -1;
    }

    public Buffer(Buffer buffer) {
        this.capacity = buffer.capacity;
        Map<Request, Request> requestMap = new HashMap<>();
        buffer.list.forEach(request -> requestMap.put(request, new Request(request)));
        this.list = buffer.list.stream().map(requestMap::get).toList();
        this.requestsPackage = buffer.requestsPackage.stream().map(requestMap::get).toList();
        this.takeIndex = buffer.takeIndex;
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean isFull() {
        return (list.size() == capacity);
    }

    public int getTakeIndex() {
        return takeIndex;
    }

    public int getSize() {
        return list.size();
    }

    public List<Request> getList() {
        return list;
    }

    public List<Request> getRequestsPackage() {
        return requestsPackage;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean putRequest(Request request) {
        if (list.size() < capacity) {
            list.add(request);
            return true;
        }
        return false;
    }

    public Request takeRequest() {
        return isEmpty() ? null : getPriorityRequest();
    }

    public void createPackage() {
        if (requestsPackage.isEmpty() && !list.isEmpty()) {
            int priority = list.get(0).getSourceNumber();
            for (Request request : list) {
                int current = request.getSourceNumber();
                if (priority == current) {
                    requestsPackage.add(request);
                }
                if (priority > current) {
                    priority = current;
                    requestsPackage.clear();
                    requestsPackage.add(request);
                }
            }
        }
    }

    private Request getPriorityRequest() {
        createPackage();
        Request request = requestsPackage.remove(0);
        takeIndex = list.indexOf(request);
        list.remove(takeIndex);
        return request;
    }
}

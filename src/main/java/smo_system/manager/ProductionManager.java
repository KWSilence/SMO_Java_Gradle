package smo_system.manager;

import smo_system.component.Buffer;
import smo_system.component.Request;
import smo_system.component.Source;

import java.util.ArrayList;

public class ProductionManager {
    private final ArrayList<Source> sources;
    private Source currentSource;
    private final int maxRequestCount;
    private final Buffer buffer;
    private int currentRequestCount;
    private final ArrayList<ArrayList<Request>> rejectedRequests;
    private Request lastRequest;

    public ProductionManager(ArrayList<Source> sources, Buffer buffer, int maxRequestCount) {
        this.sources = sources;
        this.buffer = buffer;
        this.currentSource = null;
        this.maxRequestCount = maxRequestCount;
        this.currentRequestCount = 0;
        this.rejectedRequests = new ArrayList<>();
        this.lastRequest = null;
        for (int i = 0; i < sources.size(); i++) {
            rejectedRequests.add(new ArrayList<>());
        }
    }

    public ArrayList<Source> getSources() {
        return sources;
    }

    public ArrayList<ArrayList<Request>> getRejectedRequests() {
        return rejectedRequests;
    }

    public double getTime() {
        return currentSource.getTime();
    }

    public int getMaxRequestCount() {
        return maxRequestCount;
    }

    public int getCurrentRequestCount() {
        return currentRequestCount;
    }

    public boolean canGenerate() {
        return currentRequestCount != maxRequestCount;
    }

    public Request getLastRequest() {
        return lastRequest;
    }

    public int getFullRejectCount() {
        int counter = 0;
        for (ArrayList<Request> ar : rejectedRequests) {
            counter += ar.size();
        }
        return counter;
    }

    public void generate() {
        lastRequest = currentSource.getNewRequest();
        currentRequestCount++;
    }

    public boolean putToBuffer() {
        boolean full = buffer.isFull();
        if (!full) {
            buffer.putRequest(lastRequest);
        } else {
            rejectedRequests.get(lastRequest.getSourceNumber()).add(lastRequest);
        }
        return !full;
    }

    public void selectNearestEvent() {
        currentSource = sources.get(0);
        for (Source source : sources) {
            double current = source.getTime();
            if (currentSource.getTime() > current) {
                currentSource = source;
            }
        }
    }
}

package smo_system.component;

import java.util.Random;

public class Source {
    private final int number;
    private Request lastRequest;
    private Request currentRequest;
    private int requestCount;
    private final double lambda;

    private final Random random;

    public Source(int number, double lambda) {
        this.number = number;
        this.lambda = lambda;
        this.lastRequest = null;
        this.currentRequest = null;
        this.requestCount = 0;
        this.random = new Random();
        generateRequest();
    }

    public int getNumber() {
        return number;
    }

    public Request getRequest() {
        requestCount++;
        generateRequest();
        return lastRequest;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public double getTime() {
        return currentRequest.getTime();
    }

    private void generateRequest() {
        double additionalTime = (-1 / lambda) * Math.log(random.nextDouble());
        if (currentRequest == null) {
            currentRequest = new Request(requestCount, number, additionalTime);
        } else {
            lastRequest = currentRequest;
            currentRequest = new Request(requestCount, number, lastRequest.getTime() + additionalTime);
        }
    }
}

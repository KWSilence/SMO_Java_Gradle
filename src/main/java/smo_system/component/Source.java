package smo_system.component;

import smo_system.util.TakeUtil;

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

    public Source(Source source) {
        this.number = source.number;
        this.lambda = source.lambda;
        this.lastRequest = TakeUtil.transformOrNull(source.lastRequest, Request::new);
        this.currentRequest = TakeUtil.transformOrNull(source.currentRequest, Request::new);
        this.requestCount = source.requestCount;
        this.random = source.random;
    }

    public int getNumber() {
        return number;
    }

    public Request getNewRequest() {
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

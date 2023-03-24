package smo_system.component;

import smo_system.util.TakeUtil;

import java.util.Random;

public class Source {
    private final int number;
    private double lastRequestTime;
    private Request currentRequest;
    private int requestCount;
    private final double lambda;

    private final Random random;

    public Source(int number, double lambda) {
        if (lambda <= 0) throw new IllegalArgumentException("Source lambda should be greater than 0");
        this.number = number;
        this.lambda = lambda;
        this.lastRequestTime = 0;
        this.requestCount = 0;
        this.random = new Random();
        this.currentRequest = generateRequest();
    }

    public Source(Source source) {
        this.number = source.number;
        this.lambda = source.lambda;
        this.lastRequestTime = source.lastRequestTime;
        this.requestCount = source.requestCount;
        this.random = source.random;
        this.currentRequest = TakeUtil.transformOrNull(source.currentRequest, Request::new);
    }

    public int getNumber() {
        return number;
    }

    public double getLambda() {
        return lambda;
    }

    public Request getRequestCopy() {
        return new Request(currentRequest);
    }

    public Request getRequestAndGenerate() {
        requestCount++;
        Request lastRequest = currentRequest;
        lastRequestTime = currentRequest.getTime();
        currentRequest = generateRequest();
        return lastRequest;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public double getTime() {
        return currentRequest.getTime();
    }

    private Request generateRequest() {
        double additionalTime = (-1 / lambda) * Math.log(random.nextDouble());
        return new Request(requestCount, number, lastRequestTime + additionalTime);
    }
}

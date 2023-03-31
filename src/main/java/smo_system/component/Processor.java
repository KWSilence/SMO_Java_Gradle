package smo_system.component;

import smo_system.util.TakeUtil;

public class Processor {
    private final int number;
    private Request currentRequest;
    private final double lambda;
    private double workTime;
    private double processTime;
    private boolean wait;

    public Processor(int number, double lambda) {
        if (lambda <= 0) throw new IllegalArgumentException("Processor lambda should be greater than 0");
        this.number = number;
        this.lambda = lambda;
        this.workTime = 0;
        this.processTime = 0;
        this.currentRequest = null;
        this.wait = true;
    }

    public Processor(Processor processor) {
        this.number = processor.number;
        this.lambda = processor.lambda;
        this.workTime = processor.workTime;
        this.processTime = processor.processTime;
        this.currentRequest = TakeUtil.transformOrNull(processor.currentRequest, Request::new);
        this.wait = processor.wait;
    }

    public int getNumber() {
        return number;
    }

    public double getLambda() {
        return lambda;
    }

    public double getProcessTime() {
        return processTime;
    }

    public boolean isWait() {
        return wait;
    }

    public Request getRequest() {
        return currentRequest;
    }

    public double getWorkTime() {
        return workTime;
    }

    public boolean process(Request request) {
        if (wait && request != null) {
            currentRequest = request;
            processTime = currentRequest.getTime() + currentRequest.getTimeInBuffer() + lambda;
            wait = false;
            return true;
        }
        return false;
    }

    public Request free() {
        if (currentRequest == null) return null;
        workTime += lambda;
        currentRequest.setTimeInProcessor(lambda);
        Request requestToReturn = currentRequest;
        currentRequest = null;
        wait = true;
        return requestToReturn;
    }
}

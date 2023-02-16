package smo_system.component;

public class Request {
    private final int number;
    private final int sourceNumber;
    private final double creationTime;

    private double timeInBuffer;
    private double timeInProcessor;

    Request(int number, int sourceNumber, double creationTime) {
        this.number = number;
        this.sourceNumber = sourceNumber;
        this.creationTime = creationTime;
        this.timeInBuffer = 0;
        this.timeInProcessor = 0;
    }

    public int getNumber() {
        return number;
    }

    public int getSourceNumber() {
        return sourceNumber;
    }

    public double getTime() {
        return creationTime;
    }

    public void setTimeInBuffer(double timeInBuffer) {
        this.timeInBuffer = timeInBuffer;
    }

    public double getTimeInBuffer() {
        return timeInBuffer;
    }

    public void setTimeInProcessor(double timeInProcessor) {
        this.timeInProcessor = timeInProcessor;
    }

    public double getTimeInProcessor() {
        return timeInProcessor;
    }

    public double getLifeTime() {
        return timeInBuffer + timeInProcessor;
    }
}

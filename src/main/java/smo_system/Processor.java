package smo_system;

public class Processor
{
  private final int number;
  private Request currentRequest;
  private final double lambda;
  private double workTime;
  private double processTime;
  private boolean wait;

  public Processor(int number, double lambda)
  {
    this.number = number;
    this.lambda = lambda;
    this.workTime = 0;
    this.processTime = 0;
    this.currentRequest = null;
    this.wait = true;
  }

  public int getNumber()
  {
    return number;
  }

  public double getProcessTime()
  {
    return processTime;
  }

  public boolean isWait()
  {
    return wait;
  }

  public Request getRequest()
  {
    return currentRequest;
  }

  public double getWorkTime()
  {
    return workTime;
  }

  public void process(Request request)
  {
    currentRequest = request;
    workTime += lambda;
    processTime = currentRequest.getTime() + currentRequest.getTimeInBuffer() + lambda;
    wait = false;
  }

  public Request free()
  {
    wait = true;
    currentRequest.setTimeInProcessor(lambda);
    return currentRequest;
  }

}

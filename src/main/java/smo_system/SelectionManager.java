package smo_system;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class SelectionManager
{
  private final ArrayList<Processor> processors;
  private final ArrayList<ArrayList<Request>> successRequests;
  private final Buffer buffer;
  private Processor takeProcessor;
  private Processor freeProcessor;

  private final NumberFormat formatter = new DecimalFormat("#0.000");

  public SelectionManager(ArrayList<Processor> processors, Buffer buffer)
  {
    this.processors = processors;
    this.takeProcessor = null;
    this.freeProcessor = null;
    this.buffer = buffer;
    this.successRequests = new ArrayList<>();
    for (int i = 0; i < processors.size(); i++)
    {
      successRequests.add(new ArrayList<>());
    }
  }

  public ArrayList<Processor> getProcessors()
  {
    return processors;
  }

  public ArrayList<ArrayList<Request>> getSuccessRequests()
  {
    return successRequests;
  }

  public boolean canTake()
  {
    return takeProcessor != null && !buffer.isEmpty();
  }

  public boolean canFree()
  {
    return freeProcessor != null;
  }

  public double getFreeTime()
  {
    return freeProcessor.getProcessTime();
  }

  public void putToProcessor()
  {
    selectNearestWorkEvent();
    ;
    if (canTake())
    {
      Request bufferRequest = buffer.getRequest();
      double time = takeProcessor.getProcessTime() - bufferRequest.getTime();
      bufferRequest.setTimeInBuffer((time > 0) ? time : 0);
      System.out.println(
        "Processor #" + (takeProcessor.getNumber() + 1) + " take #" + (bufferRequest.getSourceNumber() + 1) + "." +
        (bufferRequest.getNumber() + 1) + " (GT:" + formatter.format(bufferRequest.getTime()) + ", BT:" +
        formatter.format(bufferRequest.getTimeInBuffer()) + ", ST:" +
        formatter.format(bufferRequest.getTimeInBuffer() + bufferRequest.getTime()) + ")");
      takeProcessor.process(bufferRequest);
    }
  }

  public double freeProcessor()
  {
    Request request = freeProcessor.free();
    successRequests.get(request.getSourceNumber()).add(request);
    System.out.println(
      "Processor #" + (freeProcessor.getNumber() + 1) + " end #" + (request.getSourceNumber() + 1) + "." +
      (request.getNumber() + 1) + " (PT:" + formatter.format(freeProcessor.getRequest().getTimeInProcessor()) +
      ", ET:" + formatter.format(freeProcessor.getProcessTime()) + ") ");
    return freeProcessor.getProcessTime();
  }


  private void selectNearestWorkEvent()
  {
    takeProcessor = processors.get(0);
    for (Processor current : processors)
    {
      if (takeProcessor.getProcessTime() >= current.getProcessTime() && current.isWait())
      {
        takeProcessor = current;
        return;
      }
    }
    if (!takeProcessor.isWait())
    {
      takeProcessor = null;
    }
  }

  public void selectNearestFreeEvent()
  {
    freeProcessor = processors.get(0);
    for (Processor current : processors)
    {
      if ((freeProcessor.getProcessTime() >= current.getProcessTime() && !current.isWait()) ||
          (!current.isWait() && freeProcessor.isWait()))
      {
        freeProcessor = current;
      }
    }
    if (freeProcessor.isWait())
    {
      freeProcessor = null;
    }
  }
}

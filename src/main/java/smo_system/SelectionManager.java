package smo_system;

import java.util.ArrayList;

public class SelectionManager
{
  private final ArrayList<Processor> processors;
  private final ArrayList<ArrayList<Request>> successRequests;
  private final Buffer buffer;
  private Processor takeProcessor;
  private Processor freeProcessor;
  private Request lastRequest;

  public SelectionManager(ArrayList<Processor> processors, Buffer buffer, int sourceCount)
  {
    this.processors = processors;
    this.takeProcessor = null;
    this.freeProcessor = null;
    this.buffer = buffer;
    this.successRequests = new ArrayList<>();
    for (int i = 0; i < sourceCount; i++)
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

  public Request getLastRequest()
  {
    return lastRequest;
  }

  public Processor getTakeProcessor()
  {
    return takeProcessor;
  }

  public Processor getFreeProcessor()
  {
    return freeProcessor;
  }

  public int getFullSuccessCount()
  {
    int counter = 0;
    for (ArrayList<Request> ar : successRequests)
    {
      counter += ar.size();
    }
    return counter;
  }

  public boolean putToProcessor()
  {
    selectNearestWorkEvent();

    boolean take = canTake();

    if (take)
    {
      lastRequest = buffer.getRequest();
      double time = takeProcessor.getProcessTime() - lastRequest.getTime();
      lastRequest.setTimeInBuffer((time > 0) ? time : 0);
      takeProcessor.process(lastRequest);
    }
    return take;
  }

  public double freeProcessor()
  {
    lastRequest = freeProcessor.free();
    successRequests.get(lastRequest.getSourceNumber()).add(lastRequest);
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

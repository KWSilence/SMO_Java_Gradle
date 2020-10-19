package smo_system;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class ProductionManager
{
  private final ArrayList<Source> sources;
  private Source currentSource;
  private final int maxRequestCount;
  private final Buffer buffer;
  private int currentRequestCount;
  private final ArrayList<ArrayList<Request>> rejectedRequests;
  private Request lastRequest;

  private final NumberFormat formatter = new DecimalFormat("#0.000");

  public ProductionManager(ArrayList<Source> sources, Buffer buffer, int maxRequestCount)
  {
    this.sources = sources;
    this.buffer = buffer;
    this.currentSource = null;
    this.maxRequestCount = maxRequestCount;
    this.currentRequestCount = 0;
    this.rejectedRequests = new ArrayList<>();
    this.lastRequest = null;
    for (int i = 0; i < sources.size(); i++)
    {
      rejectedRequests.add(new ArrayList<>());
    }
  }

  public ArrayList<Source> getSources()
  {
    return sources;
  }

  public ArrayList<ArrayList<Request>> getRejectedRequests()
  {
    return rejectedRequests;
  }

  public double getTime()
  {
    return currentSource.getTime();
  }

  public int getMaxRequestCount()
  {
    return maxRequestCount;
  }

  public int getCurrentRequestCount()
  {
    return currentRequestCount;
  }

  public boolean canGenerate()
  {
    return currentRequestCount != maxRequestCount;
  }

  public Request getLastRequest()
  {
    return lastRequest;
  }

  public void generate()
  {
    lastRequest = currentSource.getRequest();
    currentRequestCount++;
  }

  public boolean putToBuffer()
  {
    boolean full = buffer.isFull();
    if (!full)
    {
      buffer.putRequest(lastRequest);
    }
    else
    {
      rejectedRequests.get(lastRequest.getSourceNumber()).add(lastRequest);
    }
    return !full;
  }

  public void selectNearestEvent()
  {
    currentSource = sources.get(0);
    for (Source source : sources)
    {
      double current = source.getTime();
      if (currentSource.getTime() > current)
      {
        currentSource = source;
      }
    }
  }

}

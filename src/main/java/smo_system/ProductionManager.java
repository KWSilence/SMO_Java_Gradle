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

  private final NumberFormat formatter = new DecimalFormat("#0.000");

  public ProductionManager(ArrayList<Source> sources, Buffer buffer, int maxRequestCount)
  {
    this.sources = sources;
    this.buffer = buffer;
    this.currentSource = null;
    this.maxRequestCount = maxRequestCount;
    this.currentRequestCount = 0;
    this.rejectedRequests = new ArrayList<>();
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

  public boolean putToBuffer()
  {
    Request request = currentSource.getRequest();
    if (!buffer.isFull())
    {
      buffer.putRequest(request);
      System.out.println(
        "Request #" + (request.getSourceNumber() + 1) + "." + (request.getNumber() + 1) + " generated " +
        formatter.format(request.getTime()) + " put to buffer (" + (buffer.getSize()) + ")");
    }
    else
    {
      rejectedRequests.get(request.getSourceNumber()).add(request);
      System.out.println(
        "Request #" + (request.getSourceNumber() + 1) + "." + (request.getNumber() + 1) + " generated " +
        formatter.format(request.getTime()) + " rejected (" + (buffer.getSize()) + ")");
    }
    currentRequestCount++;
    return !(currentRequestCount == maxRequestCount);
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

package smo_system;

import configs.SimulationConfig;
import gui.SimulatorEvent;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class Simulator extends Thread
{
  //  private final ArrayList<Source> sources;
  private final Buffer buffer;
  //  private final ArrayList<Processor> processors;
  private double endTime;
  private boolean useSteps;
  private final SimulatorEvent lastEvent;

  private final ProductionManager productionManager;
  private final SelectionManager selectionManager;

  private final NumberFormat formatter = new DecimalFormat("#0.000");

  public Simulator(String fileName)
  {
    SimulationConfig config = new SimulationConfig(fileName);

//    this.sources = config.getSources();
    this.buffer = config.getBuffer();
//    this.processors = config.getProcessors();
    this.productionManager = config.getProductionManager();
    this.selectionManager = config.getSelectionManager();
    this.endTime = 0;
    this.useSteps = false;
    this.lastEvent = new SimulatorEvent();
  }

  public Simulator(SimulationConfig config)
  {
//    this.sources = config.getSources();
    this.buffer = config.getBuffer();
//    this.processors = config.getProcessors();
    this.productionManager = config.getProductionManager();
    this.selectionManager = config.getSelectionManager();
    this.endTime = 0;
    this.useSteps = false;
    this.lastEvent = new SimulatorEvent();
  }

  Simulator(ArrayList<Source> sources, Buffer buffer, ArrayList<Processor> processors, int requestsCount)
  {
//    this.sources = sources;
    this.buffer = buffer;
//    this.processors = processors;
    this.productionManager = new ProductionManager(sources, buffer, requestsCount);
    this.selectionManager = new SelectionManager(processors, buffer);
    this.endTime = 0;
    this.useSteps = false;
    this.lastEvent = new SimulatorEvent();
  }

  public double getEndTime()
  {
    return endTime;
  }

  public ProductionManager getProductionManager()
  {
    return productionManager;
  }

  public SelectionManager getSelectionManager()
  {
    return selectionManager;
  }

  public void setUseSteps(boolean useSteps)
  {
    this.useSteps = useSteps;
  }

  public SimulatorEvent getLastEvent()
  {
    return lastEvent;
  }

  public int getProgress()
  {
    ArrayList<ArrayList<Request>> rr = productionManager.getRejectedRequests();
    ArrayList<ArrayList<Request>> sr = selectionManager.getSuccessRequests();
    int count = 0;
    for (int i = 0; i < rr.size(); i++)
    {
      count += rr.get(i).size() + sr.get(i).size();
    }
    return count;
  }

  @Override
  public void run()
  {
    startSimulation(useSteps);
  }

  public synchronized void startSimulation(boolean steps)
  {
    try
    {
      wait();
      while (true)
      {
        productionManager.selectNearestEvent();
        selectionManager.selectNearestFreeEvent();
        if (productionManager.canGenerate() &&
            ((selectionManager.canFree() && productionManager.getTime() < selectionManager.getFreeTime()) ||
             !selectionManager.canFree()))
        {
          productionManager.generate();
          Request request = productionManager.getLastRequest();
          if (steps)
          {
            //sGenerate
            lastEvent.setType(SimulatorEvent.EventType.GENERATE);
            lastEvent.setRequest(request);
            lastEvent.setLog(
              "Request #" + request.getSourceNumber() + "." + request.getNumber() + " was generated in " +
              formatter.format(request.getTime()) + " [" + productionManager.getCurrentRequestCount() + "/" +
              productionManager.getMaxRequestCount() + "]\n");
            notify();
            wait();
            boolean successPutToBuffer = productionManager.putToBuffer();
            boolean successTake = selectionManager.putToProcessor();
            if (successPutToBuffer)
            {
              if (successTake)
              {
                //sTake
                Processor takeProc = selectionManager.getTakeProcessor();
                lastEvent.setType(SimulatorEvent.EventType.TAKE);
                lastEvent.setRequest(request);
                lastEvent.setProcessor(takeProc);
                lastEvent.setBuffer(null);
                lastEvent.setLog(
                  "Processor #" + takeProc.getNumber() + " take Request #" + request.getSourceNumber() + "." +
                  request.getNumber() + " in " + formatter.format(request.getTime() + request.getTimeInBuffer()) +
                  "\n");
              }
              else
              {
                //sBuffer
                lastEvent.setType(SimulatorEvent.EventType.BUFFER);
                lastEvent.setRequest(request);
                lastEvent.setBuffer(buffer);
                lastEvent.setLog(
                  "Request #" + request.getSourceNumber() + "." + request.getNumber() + " put to Buffer " +
                  buffer.getSize() + "/" + buffer.getCapacity() + "\n");
              }
            }
            else
            {
              //sReject
              lastEvent.setType(SimulatorEvent.EventType.REJECT);
              lastEvent.setRequest(request);
              lastEvent.setLog("Request #" + request.getSourceNumber() + "." + request.getNumber() + " was rejected\n");
            }
            notify();
            wait();
          }
        }
        else
        {
          if (selectionManager.canFree())
          {
            endTime = selectionManager.freeProcessor();
            if (steps)
            {
              //sRelease
              Request request = selectionManager.getLastRequest();
              Processor processor = selectionManager.getFreeProcessor();
              lastEvent.setType(SimulatorEvent.EventType.RELEASE);
              lastEvent.setRequest(request);
              lastEvent.setProcessor(processor);
              lastEvent.setLog(
                "Processor #" + processor.getNumber() + " release Request #" + request.getSourceNumber() + "." +
                request.getNumber() + " in " + formatter.format(processor.getProcessTime()) + "\n");
              notify();
              wait();
            }
          }
          else
          {
            //sEnd_Work
            lastEvent.setType(SimulatorEvent.EventType.WORK_END);
            lastEvent.setLog("Simulation complete. You can create Result Table.");
            notify();
            wait();
            lastEvent.setType(SimulatorEvent.EventType.ANALYZE);
            notify();
            return;
          }
        }
        boolean successTake = selectionManager.putToProcessor();
        if (steps && successTake)
        {
          //sTake
          Processor takeProc = selectionManager.getTakeProcessor();
          Request request = takeProc.getRequest();
          lastEvent.setType(SimulatorEvent.EventType.TAKE);
          lastEvent.setRequest(request);
          lastEvent.setProcessor(takeProc);
          lastEvent.setBuffer(buffer);
          lastEvent.setLog("Processor #" + takeProc.getNumber() + " take Request #" + request.getSourceNumber() + "." +
                           request.getNumber() + " in " +
                           formatter.format(request.getTime() + request.getTimeInBuffer()) + " from Buffer(" +
                           buffer.getTakeIndex() + ")\n");
          notify();
          wait();
        }
//        buffer.printList();

      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}

package smo_system;

import configs.SimulationConfig;
import gui.SimulatorEvent;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class Simulator extends Thread
{
  private final Buffer buffer;
  private double endTime = 0;
  private boolean useSteps = false;
  private final SimulatorEvent lastEvent;

  private final ProductionManager productionManager;
  private final SelectionManager selectionManager;

  private final NumberFormat formatter = new DecimalFormat("#0.000");

  public Simulator(String fileName)
  {
    SimulationConfig config = new SimulationConfig(fileName);

    this.buffer = config.getBuffer();
    this.productionManager = config.getProductionManager();
    this.selectionManager = config.getSelectionManager();
    this.lastEvent = new SimulatorEvent();
  }

  public Simulator(SimulationConfig config)
  {
    this.buffer = config.getBuffer();
    this.productionManager = config.getProductionManager();
    this.selectionManager = config.getSelectionManager();
    this.lastEvent = new SimulatorEvent();
  }

  Simulator(ArrayList<Source> sources, Buffer buffer, ArrayList<Processor> processors, int requestsCount)
  {
    this.buffer = buffer;
    this.productionManager = new ProductionManager(sources, buffer, requestsCount);
    this.selectionManager = new SelectionManager(processors, buffer, sources.size());
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
    return (productionManager.getFullRejectCount() + selectionManager.getFullSuccessCount());
  }

  @Override
  public void run()
  {
    startSimulation(useSteps);
  }

  public void startSimulation(boolean steps)
  {
    try
    {
      if (steps)
      {
        wait();
      }
      while (!interrupted())
      {
        productionManager.selectNearestEvent();
        selectionManager.selectNearestFreeEvent();
        if (productionManager.canGenerate() &&
            ((selectionManager.canFree() && productionManager.getTime() < selectionManager.getFreeTime()) ||
             !selectionManager.canFree()))
        {
          productionManager.generate();
          Request request = productionManager.getLastRequest();

          //sGenerate
          lastEvent.setType(SimulatorEvent.EventType.GENERATE);
          lastEvent.setRequest(request);
          lastEvent.setLog("Request #" + request.getSourceNumber() + "." + request.getNumber() + " was generated in " +
                           formatter.format(request.getTime()) + " [" + productionManager.getCurrentRequestCount() +
                           "/" + productionManager.getMaxRequestCount() + "]\n");
          if (steps)
          {
            notify();
            wait();
          }
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
                request.getNumber() + " in " + formatter.format(request.getTime() + request.getTimeInBuffer()) + "\n");
            }
            else
            {
              //sBuffer
              lastEvent.setType(SimulatorEvent.EventType.BUFFER);
              lastEvent.setRequest(request);
              lastEvent.setBuffer(buffer);
              lastEvent.setLog("Request #" + request.getSourceNumber() + "." + request.getNumber() + " put to Buffer " +
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

          if (steps)
          {
            notify();
            wait();
          }

        }
        else
        {
          if (selectionManager.canFree())
          {
            endTime = selectionManager.freeProcessor();

            //sRelease
            Request request = selectionManager.getLastRequest();
            Processor processor = selectionManager.getFreeProcessor();
            lastEvent.setType(SimulatorEvent.EventType.RELEASE);
            lastEvent.setRequest(request);
            lastEvent.setProcessor(processor);
            lastEvent.setLog(
              "Processor #" + processor.getNumber() + " release Request #" + request.getSourceNumber() + "." +
              request.getNumber() + " in " + formatter.format(processor.getProcessTime()) + "\n");
            if (steps)
            {
              notify();
              wait();
            }
          }
          else
          {
            //sEnd_Work
            lastEvent.setType(SimulatorEvent.EventType.WORK_END);
            lastEvent.setLog("Simulation complete. You can create Result Table.\n");
            if (steps)
            {
              notify();
              wait();
            }
            lastEvent.setType(SimulatorEvent.EventType.ANALYZE);
            if (steps)
            {
              notify();
            }
            return;
          }
        }
        boolean successTake = selectionManager.putToProcessor();
        if (successTake)
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
          if (steps)
          {
            notify();
            wait();
          }
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}

package smo_system;

import configs.SimulationConfig;

import java.util.ArrayList;

public class Simulator
{
  private final ArrayList<Source> sources;
  private final Buffer buffer;
  private final ArrayList<Processor> processors;
  private double endTime;

  private final ProductionManager productionManager;
  private final SelectionManager selectionManager;

  public Simulator(String fileName)
  {
    SimulationConfig config = new SimulationConfig(fileName);

    this.sources = config.getSources();
    this.buffer = config.getBuffer();
    this.processors = config.getProcessors();
    this.productionManager = config.getProductionManager();
    this.selectionManager = config.getSelectionManager();
    this.endTime = 0;
  }

  Simulator(ArrayList<Source> sources, Buffer buffer, ArrayList<Processor> processors, int requestsCount)
  {
    this.sources = sources;
    this.buffer = buffer;
    this.processors = processors;
    this.productionManager = new ProductionManager(sources, buffer, requestsCount);
    this.selectionManager = new SelectionManager(processors, buffer);
    this.endTime = 0;
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

  public void startSimulation(boolean steps)
  {
    boolean continueGeneration = true;
    while (true)
    {
      productionManager.selectNearestEvent();
      selectionManager.selectNearestFreeEvent();
      if (continueGeneration &&
          ((selectionManager.canFree() && productionManager.getTime() < selectionManager.getFreeTime()) ||
           !selectionManager.canFree()))
      {
        continueGeneration = productionManager.putToBuffer();
        selectionManager.putToProcessor();
      }
      else
      {
        if (selectionManager.canFree())
        {
          endTime = selectionManager.freeProcessor();
        }
        else
        {
          System.out.println("---===[Simulation end]===---");
          System.out.println();
          break;
        }
      }
      selectionManager.putToProcessor();
      buffer.printList();
      if (steps)
      {
        try
        {
          System.in.read();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }
  }
}

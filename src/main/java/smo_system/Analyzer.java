package smo_system;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class Analyzer
{
  private class Results
  {
    public boolean isSource;
    public int number = 0;

    public int requestCount = 0;
    public double rejectProbability = 0;
    public double lifeTime = 0;
    public double bufferTime = 0;
    public double processTime = 0;
    public double bufferTimeDispersion = 0;
    public double processTimeDispersion = 0;

    public double usageRate = 0;

    Results(boolean isSource)
    {
      this.isSource = isSource;
    }
  }


  private final Simulator simulator;
  private final ArrayList<ArrayList<Results>> results;

  private final NumberFormat formatter = new DecimalFormat("#0.000");

  Analyzer(Simulator simulator)
  {
    this.simulator = simulator;
    this.results = new ArrayList<>();
  }

  public Analyzer(String fileName)
  {
    this.simulator = new Simulator(fileName);
    this.results = new ArrayList<>();
  }

  public void analyze(boolean simulated)
  {
    if (!simulated)
    {
      simulator.startSimulation(false);
    }
    analyzeSources();
    analyzeProcessors();
    printResults();
  }

  private void printResults()
  {
    System.out.println("--------------Analyzer--------------");
    for (ArrayList<Results> ar : results)
    {
      if (ar.get(0).isSource)
      {
        System.out.println("Sources:");
        System.out.println("S\t|\tRC\t|\tRP\t\t|\tLT\t\t|\tBT\t\t|\tPT\t\t|\tBTD\t\t|\tPTD");
        for (Results r : ar)
        {
          System.out.println(
            (r.number + 1) + "\t|\t" + r.requestCount + "\t|\t" + formatter.format(r.rejectProbability) + "\t|\t" +
            formatter.format(r.lifeTime) + "\t|\t" + formatter.format(r.bufferTime) + "\t|\t" +
            formatter.format(r.processTime) + "\t|\t" + formatter.format(r.bufferTimeDispersion) + "\t|\t" +
            formatter.format(r.processTimeDispersion));
        }
      }
      else
      {
        System.out.println("Processors:");
        System.out.println("P\t|\tUR");
        for (Results r : ar)
        {
          System.out.println((r.number + 1) + "\t|\t" + formatter.format(r.usageRate));
        }
      }
    }
  }

  private void analyzeSources()
  {
    ArrayList<Results> sourcesResult = new ArrayList<>();
    ProductionManager pm = simulator.getProductionManager();
    ArrayList<Source> sources = pm.getSources();
    ArrayList<ArrayList<Request>> rejected = pm.getRejectedRequests();
    SelectionManager sm = simulator.getSelectionManager();
    ArrayList<ArrayList<Request>> success = sm.getSuccessRequests();
    for (Source s : sources)
    {
      Results r = new Results(true);
      r.number = s.getNumber();
      r.requestCount = s.getRequestCount();
      r.rejectProbability = ((double) rejected.get(r.number).size()) / ((double) r.requestCount);
      r.lifeTime =
        success.get(r.number).stream().mapToDouble(a -> (a.getTimeInProcessor() + a.getTimeInBuffer())).sum() /
        r.requestCount;
      r.bufferTime = success.get(r.number).stream().mapToDouble(Request::getTimeInBuffer).sum() / r.requestCount;
      r.processTime = success.get(r.number).stream().mapToDouble(Request::getTimeInProcessor).sum() / r.requestCount;
      r.bufferTimeDispersion = success.get(r.number).stream().mapToDouble(
        a -> Math.pow((a.getTimeInBuffer() - r.bufferTime), 2) / (r.requestCount - 1)).sum();
      r.processTimeDispersion = success.get(r.number).stream().mapToDouble(
        a -> Math.pow((a.getTimeInProcessor() - r.processTime), 2) / (r.requestCount - 1)).sum();
      sourcesResult.add(r);
    }
    results.add(sourcesResult);
  }

  private void analyzeProcessors()
  {
    ArrayList<Results> processorsResult = new ArrayList<>();
    SelectionManager sm = simulator.getSelectionManager();
    ArrayList<Processor> processors = sm.getProcessors();
    double endTime = simulator.getEndTime();

    for (Processor p : processors)
    {
      Results r = new Results(false);
      r.number = p.getNumber();
      r.usageRate = p.getWorkTime() / endTime;
      processorsResult.add(r);
    }
    results.add(processorsResult);
  }
}

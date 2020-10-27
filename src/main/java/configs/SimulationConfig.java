package configs;

import com.google.gson.Gson;
import smo_system.*;

import java.io.FileReader;
import java.util.ArrayList;

public class SimulationConfig
{
  private class ConfigJSON
  {
    public ArrayList<Double> sources;
    public int bufferCapacity;
    public ArrayList<Double> processors;
    public int requestsCount;

    ConfigJSON()
    {
      sources = null;
      bufferCapacity = 0;
      processors = null;
      requestsCount = 0;
    }
  }

  private ArrayList<Source> sources;
  private ArrayList<Processor> processors;
  private Buffer buffer;
  private ProductionManager productionManager;
  private SelectionManager selectionManager;
  private ConfigJSON config;

  public SimulationConfig(String fileName)
  {
    Gson gson = new Gson();
    config = new ConfigJSON();
    try
    {
      config = gson.fromJson(new FileReader(fileName), ConfigJSON.class);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    parseConfig(config);
  }

  public SimulationConfig(ConfigJSON config)
  {
    parseConfig(config);
  }

  public ConfigJSON getConfig()
  {
    return config;
  }

  private void parseConfig(ConfigJSON config)
  {
    this.sources = new ArrayList<>();
    for (int i = 0; i < config.sources.size(); i++)
    {
      this.sources.add(new Source(i, config.sources.get(i)));
    }
    this.buffer = new Buffer(config.bufferCapacity);
    this.processors = new ArrayList<>();
    for (int i = 0; i < config.processors.size(); i++)
    {
      this.processors.add(new Processor(i, config.processors.get(i)));
    }
    this.productionManager = new ProductionManager(sources, buffer, config.requestsCount);
    this.selectionManager = new SelectionManager(processors, buffer, sources.size());
  }

  public ArrayList<Source> getSources()
  {
    return sources;
  }

  public ArrayList<Processor> getProcessors()
  {
    return processors;
  }

  public Buffer getBuffer()
  {
    return buffer;
  }

  public ProductionManager getProductionManager()
  {
    return productionManager;
  }

  public SelectionManager getSelectionManager()
  {
    return selectionManager;
  }
}

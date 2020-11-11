package gui;

import smo_system.Buffer;
import smo_system.Processor;
import smo_system.Request;

public class SimulatorEvent
{
  public enum EventType
  {
    GENERATE, PACKAGE, TAKE, BUFFER, REJECT, RELEASE, WORK_END, ANALYZE
  }

  private EventType type;
  private Request request;
  private Processor processor;
  private Buffer buffer;
  private String log;

  public SimulatorEvent()
  {
    this.type = null;
    this.request = null;
    this.processor = null;
    this.buffer = null;
    this.log = "";
  }

  public EventType getType()
  {
    return type;
  }

  public void setType(EventType type)
  {
    this.type = type;
  }

  public Request getRequest()
  {
    return request;
  }

  public void setRequest(Request request)
  {
    this.request = request;
  }

  public Processor getProcessor()
  {
    return processor;
  }

  public void setProcessor(Processor processor)
  {
    this.processor = processor;
  }

  public Buffer getBuffer()
  {
    return buffer;
  }

  public void setBuffer(Buffer buffer)
  {
    this.buffer = buffer;
  }

  public String getLog()
  {
    return log;
  }

  public void setLog(String log)
  {
    this.log = log;
  }
}


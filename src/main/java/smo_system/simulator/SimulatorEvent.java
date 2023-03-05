package smo_system.simulator;

import smo_system.component.Buffer;
import smo_system.component.Processor;
import smo_system.component.Request;
import smo_system.util.TakeUtil;

public class SimulatorEvent {
    public enum EventType {
        GENERATE, PACKAGE, TAKE, BUFFER, REJECT, RELEASE, WORK_END, ANALYZE
    }

    private EventType type;
    private Request request;
    private Processor processor;
    private Buffer buffer;
    private String log;

    public SimulatorEvent() {
        this.type = null;
        this.request = null;
        this.processor = null;
        this.buffer = null;
        this.log = "";
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Request getRequest() {
        return TakeUtil.transformOrNull(request, Request::new);
    }

    public void setRequest(Request request) {
        this.request = TakeUtil.transformOrNull(request, Request::new);
    }

    public Processor getProcessor() {
        return TakeUtil.transformOrNull(processor, Processor::new);
    }

    public void setProcessor(Processor processor) {
        this.processor = TakeUtil.transformOrNull(processor, Processor::new);
    }

    public Buffer getBuffer() {
        return TakeUtil.transformOrNull(buffer, Buffer::new);
    }

    public void setBuffer(Buffer buffer) {
        this.buffer = TakeUtil.transformOrNull(buffer, Buffer::new);
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}

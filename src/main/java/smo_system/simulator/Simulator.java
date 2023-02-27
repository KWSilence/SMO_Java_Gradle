package smo_system.simulator;

import configs.SimulationConfig;
import smo_system.component.Buffer;
import smo_system.component.Processor;
import smo_system.component.Request;
import smo_system.component.Source;
import smo_system.manager.ProductionManager;
import smo_system.manager.SelectionManager;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

public class Simulator {
    enum SimulationStep {
        INIT, GENERATE, PLACE, RELEASE, PACKAGE, TAKE, END, ANALYZE
    }

    private final Buffer buffer;
    private double endTime = 0;
    private final SimulatorEvent lastEvent;

    private final ProductionManager productionManager;
    private final SelectionManager selectionManager;

    private final NumberFormat formatter = new DecimalFormat("#0.000");
    private SimulationStep nextStep = SimulationStep.INIT;
    private Request lastRequest = null;
    private boolean nextIteration = false;

    public Simulator(String fileName) {
        SimulationConfig config = new SimulationConfig(fileName);

        this.buffer = config.getBuffer();
        this.productionManager = config.getProductionManager();
        this.selectionManager = config.getSelectionManager();
        this.lastEvent = new SimulatorEvent();
    }

    public Simulator(SimulationConfig config) {
        this.buffer = config.getBuffer();
        this.productionManager = config.getProductionManager();
        this.selectionManager = config.getSelectionManager();
        this.lastEvent = new SimulatorEvent();
    }

    public Simulator(List<Source> sources, Buffer buffer, List<Processor> processors, int requestsCount) {
        this.buffer = buffer;
        this.productionManager = new ProductionManager(sources, buffer, requestsCount);
        this.selectionManager = new SelectionManager(processors, buffer, sources.size());
        this.lastEvent = new SimulatorEvent();
    }

    public double getEndTime() {
        return endTime;
    }

    public ProductionManager getProductionManager() {
        return productionManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public SimulatorEvent getLastEvent() {
        return lastEvent;
    }

    public int getProgress() {
        return (productionManager.getFullRejectCount() + selectionManager.getFullSuccessCount());
    }

    public boolean canContinue() {
        return nextStep != null;
    }

    public boolean simulationStep() {
        try {
            if (nextStep == null) {
                return false;
            }

            processInitStep();
            processGenerateStep();
            processPlaceStep();
            processReleaseStep();
            processEndStep();
            processAnalyzeStep();
            processPackageStep();
            processTakeStep();

            if (!nextIteration) {
                nextIteration = true;
                nextStep = SimulationStep.GENERATE;
                boolean state = simulationStep();
                if (state) nextIteration = false;
                return state;
            } else {
                nextStep = null;
                return false;
            }
        } catch (SimulationStepCompleteException complete) {
            return complete.getState();
        }
    }

    private void processInitStep() {
        if (nextStep == SimulationStep.INIT) {
            nextStep = SimulationStep.GENERATE;
        }
    }

    private void processGenerateStep() throws SimulationStepCompleteException {
        if (nextStep == SimulationStep.GENERATE) {
            productionManager.selectNearestEvent();
            selectionManager.selectNearestFreeEvent();
            if (productionManager.canGenerate() &&
                    ((selectionManager.canFree() && productionManager.getTime() < selectionManager.getFreeTime()) ||
                            !selectionManager.canFree())) {
                productionManager.generate();
                Request request = productionManager.getLastRequest();

                //sGenerate
                lastEvent.setType(SimulatorEvent.EventType.GENERATE);
                lastEvent.setRequest(request);
                lastEvent.setLog(getRequestString(request) + " was generated in " +
                        formatter.format(request.getTime()) + " [" + productionManager.getCurrentRequestCount() +
                        "/" + productionManager.getMaxRequestCount() + "]\n");

                lastRequest = request;
                nextStep = SimulationStep.PLACE;
                throw new SimulationStepCompleteException(true);
            }
            nextStep = SimulationStep.RELEASE;
        }
    }

    private void processPlaceStep() throws SimulationStepCompleteException {
        if (nextStep == SimulationStep.PLACE) {
            Request request = lastRequest;
            boolean successPutToBuffer = productionManager.putToBuffer();
            boolean successTake = selectionManager.putToProcessor();
            if (successPutToBuffer) {
                if (successTake) {
                    //sTake
                    Processor takeProc = selectionManager.getTakeProcessor();
                    lastEvent.setType(SimulatorEvent.EventType.TAKE);
                    lastEvent.setRequest(request);
                    lastEvent.setProcessor(takeProc);
                    lastEvent.setBuffer(null);
                    lastEvent.setLog(getProcessorString(takeProc) + " take " + getRequestString(request) + " in " +
                            formatter.format(request.getTime() + request.getTimeInBuffer()) + "\n");
                } else {
                    //sBuffer
                    lastEvent.setType(SimulatorEvent.EventType.BUFFER);
                    lastEvent.setRequest(request);
                    lastEvent.setBuffer(buffer);
                    lastEvent.setLog(getRequestString(request) + " put to Buffer " +
                            buffer.getSize() + "/" + buffer.getCapacity() + "\n");
                }
            } else {
                //sReject
                lastEvent.setType(SimulatorEvent.EventType.REJECT);
                lastEvent.setRequest(request);
                lastEvent.setLog(getRequestString(request) + " was rejected\n");
            }

            lastRequest = null;
            nextStep = SimulationStep.PACKAGE;
            throw new SimulationStepCompleteException(true);
        }
    }

    private void processReleaseStep() throws SimulationStepCompleteException {
        if (nextStep == SimulationStep.RELEASE) {
            if (selectionManager.canFree()) {
                endTime = selectionManager.freeProcessor();

                //sRelease
                Request request = selectionManager.getLastRequest();
                Processor processor = selectionManager.getFreeProcessor();
                lastEvent.setType(SimulatorEvent.EventType.RELEASE);
                lastEvent.setRequest(request);
                lastEvent.setProcessor(processor);
                lastEvent.setLog(getProcessorString(processor) + " release " + getRequestString(request) +
                        " in " + formatter.format(processor.getProcessTime()) + "\n");
                nextStep = SimulationStep.PACKAGE;
                throw new SimulationStepCompleteException(true);
            }
            nextStep = SimulationStep.END;
            throw new SimulationStepCompleteException(true);
        }
    }

    private void processEndStep() throws SimulationStepCompleteException {
        if (nextStep == SimulationStep.END) {
            lastEvent.setType(SimulatorEvent.EventType.WORK_END);
            lastEvent.setLog("Simulation complete.\nYou can create Result Table. Press 'Next Step'\n");
            nextStep = SimulationStep.ANALYZE;
            throw new SimulationStepCompleteException(true);
        }
    }

    private void processAnalyzeStep() throws SimulationStepCompleteException {
        if (nextStep == SimulationStep.ANALYZE) {
            lastEvent.setType(SimulatorEvent.EventType.ANALYZE);
            nextStep = null;
            throw new SimulationStepCompleteException(false);
        }
    }

    private void processTakeStep() throws SimulationStepCompleteException {
        if (nextStep == SimulationStep.TAKE) {
            boolean successTake = selectionManager.putToProcessor();
            if (successTake) {
                //sTake
                Processor takeProc = selectionManager.getTakeProcessor();
                Request request = takeProc.getRequest();
                lastEvent.setType(SimulatorEvent.EventType.TAKE);
                lastEvent.setRequest(request);
                lastEvent.setProcessor(takeProc);
                lastEvent.setBuffer(buffer);
                lastEvent.setLog(getProcessorString(takeProc) + " take " + getRequestString(request) + " in " +
                        formatter.format(request.getTime() + request.getTimeInBuffer()) + " from Buffer(" +
                        buffer.getTakeIndex() + ")\n");
                nextStep = SimulationStep.GENERATE;
                throw new SimulationStepCompleteException(true);
            }
        }
    }

    private void processPackageStep() throws SimulationStepCompleteException {
        if (nextStep == SimulationStep.PACKAGE) {
            selectionManager.selectNearestWorkEvent();
            if (buffer.getRequestsPackage().isEmpty() && selectionManager.canTake()) {
                buffer.createPackage();
                lastEvent.setType(SimulatorEvent.EventType.PACKAGE);
                lastEvent.setBuffer(buffer);
                StringBuilder s = new StringBuilder();
                for (Request r : buffer.getRequestsPackage()) {
                    s.append(r.getSourceNumber()).append('.').append(r.getNumber()).append(", ");
                }
                lastEvent.setLog("Create Package: " + s + "\n");
                nextStep = SimulationStep.TAKE;
                throw new SimulationStepCompleteException(true);
            }
            nextStep = SimulationStep.TAKE;
        }
    }

    private String getProcessorString(Processor processor) {
        return "Processor #" + processor.getNumber();
    }

    private String getRequestString(Request request) {
        return "Request #" + request.getSourceNumber() + "." + request.getNumber();
    }

    public void fullSimulation() {
        do {
            simulationStep();
        } while (canContinue());
    }
}

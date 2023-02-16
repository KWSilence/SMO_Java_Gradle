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
import java.util.ArrayList;

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

    public Simulator(ArrayList<Source> sources, Buffer buffer, ArrayList<Processor> processors, int requestsCount) {
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
        if (nextStep == null) return false;

        if (nextStep == SimulationStep.INIT) {
            nextStep = SimulationStep.GENERATE;
        }

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
                lastEvent.setLog("Request #" + request.getSourceNumber() + "." + request.getNumber() + " was generated in " +
                        formatter.format(request.getTime()) + " [" + productionManager.getCurrentRequestCount() +
                        "/" + productionManager.getMaxRequestCount() + "]\n");

                lastRequest = request;
                nextStep = SimulationStep.PLACE;
                return true;
            }

            nextStep = SimulationStep.RELEASE;
        }

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
                    lastEvent.setLog(
                            "Processor #" + takeProc.getNumber() + " take Request #" + request.getSourceNumber() + "." +
                                    request.getNumber() + " in " + formatter.format(request.getTime() + request.getTimeInBuffer()) + "\n");
                } else {
                    //sBuffer
                    lastEvent.setType(SimulatorEvent.EventType.BUFFER);
                    lastEvent.setRequest(request);
                    lastEvent.setBuffer(buffer);
                    lastEvent.setLog("Request #" + request.getSourceNumber() + "." + request.getNumber() + " put to Buffer " +
                            buffer.getSize() + "/" + buffer.getCapacity() + "\n");
                }
            } else {
                //sReject
                lastEvent.setType(SimulatorEvent.EventType.REJECT);
                lastEvent.setRequest(request);
                lastEvent.setLog("Request #" + request.getSourceNumber() + "." + request.getNumber() + " was rejected\n");
            }

            lastRequest = null;
            nextStep = SimulationStep.PACKAGE;
            return true;
        }

        if (nextStep == SimulationStep.RELEASE) {
            if (selectionManager.canFree()) {
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

                nextStep = SimulationStep.PACKAGE;
                return true;
            }

            nextStep = SimulationStep.END;
            return true;
        }

        if (nextStep == SimulationStep.END) {
            lastEvent.setType(SimulatorEvent.EventType.WORK_END);
            lastEvent.setLog("Simulation complete.\nYou can create Result Table. Press 'Next Step'\n");

            nextStep = SimulationStep.ANALYZE;
            return true;
        }

        if (nextStep == SimulationStep.ANALYZE) {
            lastEvent.setType(SimulatorEvent.EventType.ANALYZE);

            nextStep = null;
            return false;
        }

        if (nextStep == SimulationStep.PACKAGE) {
            selectionManager.selectNearestWorkEvent();
            //sPackage
            if (buffer.getRequestsPackage().isEmpty() && selectionManager.canTake()) {
                buffer.createPackage();
                lastEvent.setType(SimulatorEvent.EventType.PACKAGE);
                lastEvent.setBuffer(buffer);
                StringBuilder s = new StringBuilder();
                for (Request r : buffer.getRequestsPackage()) {
                    s.append(r.getSourceNumber()).append(".").append(r.getNumber()).append(", ");
                }
                lastEvent.setLog("Create Package: " + s + "\n");

                nextStep = SimulationStep.TAKE;
                return true;
            }

            nextStep = SimulationStep.TAKE;
        }

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
                lastEvent.setLog("Processor #" + takeProc.getNumber() + " take Request #" + request.getSourceNumber() + "." +
                        request.getNumber() + " in " +
                        formatter.format(request.getTime() + request.getTimeInBuffer()) + " from Buffer(" +
                        buffer.getTakeIndex() + ")\n");

                nextStep = SimulationStep.GENERATE;
                return true;
            }
        }
        if (!nextIteration) {
            nextIteration = true;
            nextStep = SimulationStep.GENERATE;
            boolean state = simulationStep();
            if (nextStep != null) nextIteration = false;
            return state;
        } else {
            nextStep = null;
            return false;
        }
    }

    public void fullSimulation() {
        do {
            simulationStep();
        } while (canContinue());
    }
}

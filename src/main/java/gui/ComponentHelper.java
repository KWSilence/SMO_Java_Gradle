package gui;

public class ComponentHelper {
    public static final class MainHelper {
        public static final String title = "Service System";

        public static final String root = "MainWindow";
        public static final String tabbedPane = "MainTabbedPane";

        public static final String autoTabTitle = "auto";
        public static final String stepTabTitle = "step";
        public static final String analyzeTabTitle = "analyze";
        public static final String settingsTabTitle = "settings";
    }

    public static final class AutoHelper {
        public static final String stop = "AutoButtonStop";
        public static final String start = "AutoButtonStart";
        public static final String useN0 = "AutoCheckBoxUseN0";
        public static final String n0 = "AutoTextFieldN0";
    }

    public static final class StepHelper {
        public static final String stop = "StepButtonStop";
        public static final String start = "StepButtonStart";
        public static final String skip = "StepButtonSkip";
        public static final String autoScroll = "StepCheckBoxAutoScroll";
    }

    public static final class AnalyzeHelper {
        public static final String selector = "AnalyzeComboBoxSelector";
        public static final String from = "AnalyzeTextFieldFrom";
        public static final String to = "AnalyzeTextFieldTo";
        public static final String lambda = "AnalyzeTextFieldLambda";
        public static final String visualStep = "AnalyzeTextFieldVisualStep";
        public static final String stop = "AnalyzeButtonStop";
        public static final String start = "AnalyzeButtonStart";

        public static final String selectorSourceOption = "Source";
        public static final String selectorProcessorOption = "Processor";
        public static final String selectorBufferOption = "Buffer";
    }

    public static final class SettingsHelper {
        public static final String sourcesCount = "SettingsTextFieldSourcesCount";
        public static final String processorsCount = "SettingsTextFieldProcessorsCount";
        public static final String sourcesLambdas = "SettingsTextFieldSourcesLambdas";
        public static final String processorsLambdas = "SettingsTextFieldProcessorsLambdas";
        public static final String sourcesCountSet = "SettingsButtonSourcesCountSet";
        public static final String processorsCountSet = "SettingsButtonProcessorsCountSet";
        public static final String sourcesLambdasSet = "SettingsButtonSourcesLambdasSet";
        public static final String processorsLambdasSet = "SettingsButtonProcessorsLambdasSet";
        public static final String bufferCapacity = "SettingsTextFieldBufferCapacity";
        public static final String requestsCount = "SettingsTextFieldRequestsCount";
        public static final String refresh = "SettingsButtonRefresh";
        public static final String save = "SettingsButtonSave";
    }
}

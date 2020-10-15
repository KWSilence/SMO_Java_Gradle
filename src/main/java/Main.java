import smo_system.Analyzer;
import smo_system.Simulator;

public class Main
{
  public static void main(String[] args)
  {
    String fileName = "./config.json";
//    String fileName = "src/main/resources/config.json";

    Analyzer analyzer = new Analyzer(fileName, false);
    analyzer.analyze();
  }
}
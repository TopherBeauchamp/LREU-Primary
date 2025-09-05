import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides functionality to run multiple simulations
 * with different battery levels and network configurations.
 */
public class BatchTester {
    
    public static void main(String[] args) {
        // Define battery levels to test (in Wh)
        double[] batteryLevels = {50, 70, 90};
        
        // Define which network files to use
        String[] networkFiles = {"N1.txt", "N2.txt", "N3.txt", "N4.txt"};
        
        // Define transmission range
        int transmissionRange = 500;
        
        // Create exporter
        DataExporter exporter = new DataExporter();
        
        // Run simulations for each battery level and network
        for (double battery : batteryLevels) {
            System.out.println("Running simulations for battery level: " + battery + "Wh");
            
            for (String networkFile : networkFiles) {
                try {
                    // Load the network
                    String fullFilePath = "Networks/" + networkFile;
                    InputNetwork inputNetwork = new InputNetwork(fullFilePath, transmissionRange);
                    
                    // Set up the simulation
                    ListGraph graph = inputNetwork.getGraph();
                    List<Node> nodeList = inputNetwork.getNodeList();
                    Robot robot = new Robot(battery, new ArrayList<>(nodeList));
                    robot.setFeasibleNodes();
                    
                    // Run the algorithm
                    System.out.println("  Running " + networkFile + " with " + battery + "Wh...");
                    long startTime = System.currentTimeMillis();
                    
                    List<Node> feasibleNodes = robot.getFeasibleNodes();
                    while (feasibleNodes.size() != 0) {
                        robot.findBestPCR();
                        robot.moveRobotToNode(robot.getGreatestNode());
                        graph.updatePrizes(robot.getGreatestNode().getNetwork());
                        robot.setFeasibleNodes();
                        feasibleNodes = robot.getFeasibleNodes();
                    }
                    
                    robot.returnHome();
                    
                    long computationalTime = System.currentTimeMillis() - startTime;
                    
                    // Add result to exporter
                    DataExporter.SimulationResult result = new DataExporter.SimulationResult(
                        networkFile,
                        robot.getTotalPackets(),
                        robot.getTotalDistance(),
                        computationalTime,
                        battery
                    );
                    
                    exporter.addResult(result);
                    
                    System.out.println("    Completed: " + result.getDataPackets() + " packets, " + 
                                      String.format("%.2f", result.getDistanceTraveled()) + " distance, " +
                                      result.getComputationalTime() + "ms");
                    
                } catch (FileNotFoundException e) {
                    System.out.println("  Error: Could not find network file " + networkFile);
                }
            }
        }
        
        // Calculate statistics and export to CSV
        exporter.calculateAllStatistics();
        try {
            exporter.exportToCSV("batch_test_results.csv");
            System.out.println("Results exported to batch_test_results.csv");
        } catch (IOException e) {
            System.out.println("Error exporting results: " + e.getMessage());
        }
    }
}

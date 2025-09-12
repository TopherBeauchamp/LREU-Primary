import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.IOException;

public class SensorNetworkRunner {
    
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        
        System.out.println("Do you want to run a single simulation (1) or multiple simulations (2)?");
        int choice = scan.nextInt();
        
        if (choice == 1) {
            runSingleSimulation(scan);
        } else if (choice == 2) {
            runMultipleSimulations(scan);
        } else {
            System.out.println("Invalid choice. Exiting.");
        }
        
        scan.close();
    }
    
    private static void runSingleSimulation(Scanner scan) {
        int inputtingNetwork = 0;
        AutomatedSetup autoSetup = new AutomatedSetup();
        
        System.out.println("Will you be inputting a network? 1 for yes & 0 for no");
        inputtingNetwork = scan.nextInt();
        
        ListGraph graph = null;
        Robot robot = null;
        List<Node> nodeList = null;
        List<Node> immutableNodeList = null;
        List<Node> modify = null;
        int transmissionRange = 0;
        double battery = 0.0;

        if (inputtingNetwork == 0) {
            autoSetup.setVariables();
            graph = autoSetup.createNetwork();
            robot = autoSetup.createRobot();
            battery = robot.getBattery(); // Assuming we add this getter
        } else if (inputtingNetwork == 1) {
            System.out.println("Enter the transmission range:");
            transmissionRange = scan.nextInt();
            System.out.println("Enter the filename:");
            String filename = scan.next();
            
            // Network file path
            String fullFilePath = "Networks/" + filename; // Modified to use relative path
            
            try {
                InputNetwork inputNetwork = new InputNetwork(fullFilePath, transmissionRange);
                graph = inputNetwork.getGraph();
                nodeList = inputNetwork.getNodeList();
                modify = new ArrayList<>(nodeList);
                modify.add(new Node(0, 0, 0, 0));
                immutableNodeList = Collections.unmodifiableList(new ArrayList<>(modify));
                
                System.out.println("Enter the amount of battery attributed to robot in watts:");
                battery = scan.nextDouble();
                robot = new Robot(battery, nodeList);
                robot.setFeasibleNodes();
            } catch (FileNotFoundException e) {
                System.out.println("File not found. Exiting.");
                return;
            }
        }

        // Run the algorithm and gather results
        DataExporter.SimulationResult result =
            runAlgorithm(robot, graph, inputtingNetwork, autoSetup, nodeList, immutableNodeList, transmissionRange, battery);

        
        // Create exporter and export results
        DataExporter exporter = new DataExporter();
        exporter.addResult(result);
        exporter.calculateAllStatistics();
        
        try {
            exporter.exportToCSV("single_simulation_results.csv");
            System.out.println("Results exported to single_simulation_results.csv");
        } catch (IOException e) {
            System.out.println("Error exporting results: " + e.getMessage());
        }
    }
    
    private static void runMultipleSimulations(Scanner scan) {
        System.out.println("Enter the number of battery levels to test:");
        int batteryLevels = scan.nextInt();
        
        double[] batteryValues = new double[batteryLevels];
        for (int i = 0; i < batteryLevels; i++) {
            System.out.println("Enter battery level " + (i+1) + " (in Wh):");
            batteryValues[i] = scan.nextDouble();
        }
        
        System.out.println("Enter the number of networks to test per battery level:");
        int networksPerBattery = scan.nextInt();
        
        System.out.println("Enter the transmission range:");
        int transmissionRange = scan.nextInt();
        
        DataExporter exporter = new DataExporter();
        
        // For each battery level
        for (double battery : batteryValues) {
            System.out.println("Running simulations for battery level: " + battery + "Wh");
            
            // For each network
            for (int networkNum = 1; networkNum <= networksPerBattery; networkNum++) {
                String filename = "N" + networkNum + ".txt";
                String fullFilePath = "Networks/" + filename;
                
                try {
                    InputNetwork inputNetwork = new InputNetwork(fullFilePath, transmissionRange);
                    ListGraph graph = inputNetwork.getGraph();
                    List<Node> nodeList = inputNetwork.getNodeList();
                    List<Node> modify = new ArrayList<>(nodeList);
                    modify.add(new Node(0, 0, 0, 0));
                    List<Node> immutableNodeList = Collections.unmodifiableList(new ArrayList<>(modify));
                    
                    Robot robot = new Robot(battery, nodeList);
                    robot.setFeasibleNodes();
                    
                    // Run algorithm without visualization
                    DataExporter.SimulationResult result =
                        runAlgorithm(robot, graph, 1, null, nodeList, immutableNodeList, transmissionRange, battery);
                    
                    exporter.addResult(result);
                    
                    System.out.println("  Completed network " + networkNum + " with " + 
                                      result.getDataPackets() + " packets, " + 
                                      result.getDistanceTraveled() + " distance");
                    
                } catch (FileNotFoundException e) {
                    System.out.println("File not found: " + fullFilePath);
                    System.out.println("Skipping to next network...");
                }
            }
        }
        
        exporter.calculateAllStatistics();
        
        try {
            exporter.exportToCSV("multi_simulation_results.csv");
            System.out.println("Results exported to multi_simulation_results.csv");
        } catch (IOException e) {
            System.out.println("Error exporting results: " + e.getMessage());
        }
    }
        
    private static DataExporter.SimulationResult runAlgorithm(
        Robot robot,
        ListGraph graph,
        int inputtingNetwork,
        AutomatedSetup autoSetup,
        List<Node> nodeList,
        List<Node> immutableNodeList,
        int transmissionRange,
        double battery // <-- ADD THIS
    ) {
        List<Node> feasibleNodes = robot.getFeasibleNodes();
        long initialTime = System.currentTimeMillis();

        while (!feasibleNodes.isEmpty()) {
            robot.findBestPCR();
            robot.moveRobotToNode(robot.getGreatestNode());
            graph.updatePrizes(robot.getGreatestNode().getNetwork());
            robot.setFeasibleNodes();
            feasibleNodes = robot.getFeasibleNodes();
        }

        robot.returnHome();
        long computationalTime = System.currentTimeMillis() - initialTime;

        return new DataExporter.SimulationResult(
            "Network",
            robot.getTotalPackets(),
            robot.getTotalDistance(),
            computationalTime,
            battery // ✅ Pass it here instead of setting later
        );
    }
}
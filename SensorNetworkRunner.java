import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorNetworkRunner {
    
    public static void main(String[] args){ 
        Scanner scan = new Scanner(System.in);
        int inputtingNetwork = 0; 
        AutomatedSetup autoSetup = new AutomatedSetup(); // This can be declared at start because constructor doesn't require anything 
        
        System.out.println("Will you be inputting a network? 1 for yes & 0 for no");
        inputtingNetwork = scan.nextInt(); // Switch to determine whether network is generated or based on a given file 
        
        // Sensor Network Variables 
        ListGraph graph = null; 
        Robot robot = null; 
        List<Node> nodeList = null;
        List<Node> immutableNodeList = null; 
        List<Node> modify = null; 
        int transmissionRange = 0; 

        if(inputtingNetwork == 0){
            // Generating network logic 
            autoSetup.setVariables(); 
            graph = autoSetup.createNetwork();
            robot = autoSetup.createRobot();
        } else if (inputtingNetwork == 1) {
            // Network based on file logic 
            System.out.println("Enter the transmission range:");
            transmissionRange = scan.nextInt();
            System.out.println("Enter the filename:");
            String filename = scan.next();
            

            /* *********************** IMPORTANT **************************
             * The path below must be altered in order for this to work properly 
             * in your environment
             */
            String fullFilePath = "C:\\Users\\tophe\\OneDrive\\Desktop\\LREU-Final\\Networks\\" + filename;
            try {
                InputNetwork inputNetwork = new InputNetwork(fullFilePath, transmissionRange);
                graph = inputNetwork.getGraph();
                nodeList = inputNetwork.getNodeList();
                modify = new ArrayList<>(nodeList);
                modify.add(new Node(0, 0, 0, 0));
                immutableNodeList = Collections.unmodifiableList(new ArrayList<>(modify));
                for(Node node : nodeList){
                    System.out.println(node);
                }
                int count = nodeList.size()+1;
                System.out.println("There are " + count + " nodes in the network including initial deposit");   
                // Battery and energy coefficient need to be inputted 
                System.out.println("Enter the amount of battery attributed to robot in watts:");
                double battery = scan.nextDouble();
                robot = new Robot(battery, nodeList);
                robot.setFeasibleNodes();
            } catch (FileNotFoundException e) {
                System.out.println("File not found. Exiting.");
                scan.close();
                return;
            }
        }
        scan.close();

    
        List<Node> feasibleNodes = robot.getFeasibleNodes();
        int iter = 1; 


        /* This is where the primary algorithm starts 
         * While there are still feasible nodes for the robot to visit, 
         * The node with the highest PCR ratio is found, the robot goes there
         * Prizes for nodes associated with the greatest PCR node are updated 
         * the feasible nodes variable is updated and the next iteration occurs 
         */ 
        long initialTime = System.currentTimeMillis();
        while(feasibleNodes.size() != 0){
            robot.findBestPCR();
            System.out.println("\nIteration " + iter + "\n");
            System.out.println(robot);
            System.out.println("\nFeasible Nodes:");
            for(Node node : feasibleNodes){
                System.out.println(node); 
            }
            iter++; 
            System.out.println("\nRobot goes to " + robot.getGreatestNode());
            robot.moveRobotToNode(robot.getGreatestNode());
            graph.updatePrizes(robot.getGreatestNode().getNetwork());
            robot.setFeasibleNodes();
            feasibleNodes = robot.getFeasibleNodes();
        }
         
        // Printing at end of algorithm to show no more feasible nodes
        System.out.println("End of Algorithm, total of " + iter + " iterations ");
        System.out.println(robot);
        System.out.println("\nFeasible Nodes:");
        for(Node node : feasibleNodes){
            if(feasibleNodes.size() == 0){
                System.out.println("No more feasible nodes");
            }
            System.out.println(node);
        }
        robot.returnHome();

        // Getting & printing algorithm time 
        long end = System.currentTimeMillis() - initialTime; 
        System.out.println("Algorithm took " + end + " milliseconds");


        List<Node> route = robot.getRoute();

        if(inputtingNetwork == 0){
            Visualization visual = new Visualization(autoSetup.getNodeList(), autoSetup.getWidth(), autoSetup.getLength(), route, transmissionRange);
            visual.run(); 
        }
        if(inputtingNetwork == 1){
            Visualization visual = new Visualization( immutableNodeList, 1700, 1700, route, transmissionRange);
            visual.run();
        }
     }
}
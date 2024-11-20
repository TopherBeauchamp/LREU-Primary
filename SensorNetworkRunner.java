import java.io.FileNotFoundException;
import java.util.Scanner; 
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

        
        if(inputtingNetwork == 0){
            // Generating network logic 
            autoSetup.setVariables();
            graph = autoSetup.createNetwork();
            robot = autoSetup.createRobot();
        } else if (inputtingNetwork == 1) {
            // Network based on file logic 
            System.out.println("Enter the transmission range:");
            int transmissionRange = scan.nextInt();
            System.out.println("Enter the filename:");
            String filename = scan.next();
            

            /* *********************** IMPORTANT **************************
             * The path below must be altered in order for this to work properly 
             * in your enviornment. I'm currently working on incorperating a .env 
             * file to make this process easier 
             */
            String fullFilePath = "C:\\Users\\tophe\\OneDrive\\Desktop\\LREU-Final\\Networks\\" + filename;
            try {
                InputNetwork inputNetwork = new InputNetwork(fullFilePath, transmissionRange);
                graph = inputNetwork.getGraph();
                nodeList = inputNetwork.getNodeList();
                for(Node node : nodeList){
                    System.out.println(node);
                }
                // Battery and energy coefficient need to be inputted 
                System.out.println("Enter the amount of battery attributed to robot in watts:");
                double battery = scan.nextDouble();
                System.out.println("Enter the energy coefficient of the robot:");
                double energyCoefficient = scan.nextDouble();
                
                robot = new Robot(battery, energyCoefficient, nodeList);
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
        System.out.println("Algorithm begins at: " + System.currentTimeMillis());
        while(feasibleNodes.size() != 0){
            robot.findBestPCR();
            System.out.println("\nIteration " + iter + "\n");
            System.out.println(robot);
            System.out.println("\nFeasible Nodes:");
            for(Node node : feasibleNodes){
                System.out.println(node);
            }
            iter++; 
            robot.moveRobotToNode(robot.getGreatestNode());
            graph.updatePrizes(robot.getGreatestNode().getNetwork());
            robot.setFeasibleNodes();
            feasibleNodes = robot.getFeasibleNodes();
        }
        
        System.out.println(robot);
        System.out.println("\nFeasible Nodes:");
        for(Node node : feasibleNodes){
            System.out.println(node);
        }

        robot.returnHome();
        System.out.println("Algorithm ends at: " + System.currentTimeMillis());
     }
}
import java.util.Scanner; 
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class SensorNetworkRunner {
    
    public static void main(String[] args){ 
        Scanner scan = new Scanner(System.in);
        int numNodes, width, length, transmissionRange, minPackets, maxPackets; 
        double battery, energyCoefficient;

        //Getting user input 
        System.out.println("Please enter width of sensor network:");
        width = scan.nextInt();
        System.out.println("Please enter length of sensor network:");
        length = scan.nextInt();
        System.out.println("Please enter number of nodes in sensor network:");
        numNodes = scan.nextInt();
        System.out.println("Please enter transmission range:");
        transmissionRange = scan.nextInt();
        System.out.println("Please enter maximum number of data packets per node:");
        maxPackets = scan.nextInt();
        System.out.println("Please enter minimum number of data packets per node:");
        minPackets= scan.nextInt();
        System.out.println("Please enter the amount of battery attributed to robot in watts");
        battery = scan.nextDouble();
        System.out.println("Please enter the energy coefficient of the robot");
        energyCoefficient = scan.nextDouble();
        scan.close(); 

        ListGraph graph = new ListGraph(numNodes);

        //Initializing nodes 
        List<Node> nodeList = new ArrayList<Node>(numNodes);
        for(int i = 1; i <= numNodes; i++){ 
            Node newNode = new Node(i, width, length, minPackets, maxPackets);
            nodeList.add(newNode);

        }


        //If a pair of nodes are in distance of each other, add them to the graph
        for (int i = 0; i < numNodes; i++) {
            for (int j = i + 1; j < numNodes; j++) {
                if (nodeList.get(i).getDistance(nodeList.get(j)) <= transmissionRange) {
                    graph.addEdge(nodeList.get(i), nodeList.get(j));
                }
            }
        }

        for(int i = 1; i <= numNodes; i++){
            int prize = graph.calculatePrize(i, true);
            Node node = Node.getNodeById(i); 
            node.setPrize(prize);
        }

        Robot robot = new Robot(battery, energyCoefficient, nodeList);
        robot.setFeasibleNodes();
        List<Node> feasibleNodes = robot.getFeasibleNodes();
        List<Node> unvisitedNodes = robot.getUnvisitedNodes();
        int iter = 1; 

        while(feasibleNodes.size() != 0 && unvisitedNodes.size() != 0){
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
            unvisitedNodes = robot.getUnvisitedNodes();
        }
        
        System.out.println(robot);
        System.out.println("\nFeasible Nodes:");
            for(Node node : feasibleNodes){
                System.out.println(node);
            }

        robot.returnHome();

     }
     
}

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AutomatedSetup {
    private int numNodes = 0;
    private int width = 0;
    private int length = 0;
    private int transmissionRange = 0;
    private int minPackets = 0;
    private int maxPackets = 0;
    private double battery = 0.0;
    private double energyCoefficient = 0.0;
    List<Node> nodeList; 

    public AutomatedSetup() {}


    public void setVariables(){ 
        // Getting user input 
        Scanner scan = new Scanner(System.in);
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
        minPackets = scan.nextInt();
        System.out.println("Please enter the amount of battery attributed to robot in watts");
        battery = scan.nextDouble();
        System.out.println("Please enter the energy coefficient of the robot");
        energyCoefficient = scan.nextDouble();
        scan.close();
    }


     public ListGraph createNetwork() {  
        ListGraph graph = new ListGraph(numNodes);

        // Existing network creation logic
        nodeList = new ArrayList<>(numNodes);
        for (int i = 1; i <= numNodes; i++) { 
            Node newNode = new Node(i, width, length, minPackets, maxPackets);
            nodeList.add(newNode);
        }

        for (int i = 0; i < numNodes; i++) {
            for (int j = i + 1; j < numNodes; j++) {
                if (nodeList.get(i).getDistance(nodeList.get(j)) <= transmissionRange) {
                    graph.addEdge(nodeList.get(i), nodeList.get(j));
                }
            }
        }

        for (int i = 1; i <= numNodes; i++) {
            int prize = graph.calculatePrize(i, true);
            Node node = Node.getNodeById(i); 
            node.setPrize(prize);
        }

        return graph; 
    }


    public Robot createRobot(){
        Robot robot = new Robot(battery, energyCoefficient, nodeList);
        robot.setFeasibleNodes();
        return robot; 
    }
    public List<Node> getNodeList(){
        return nodeList; 
    }

    public int getWidth(){
        return width;
    }

    public int getLength(){ 
        return length; 
    }
}

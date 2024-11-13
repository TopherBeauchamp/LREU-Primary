import java.util.ArrayList;
import java.util.List;
public class Robot {
    private int x = 0; 
    private int y = 0; 
    private int totalPackets; 
    private double battery; 
    private double energyCoefficient; 
    private List<Node> unvisitedNodes = new ArrayList<>(); 
    private List<Node> feasibleNodes = new ArrayList<>();
    private List<String> route = new ArrayList<>(); 
    private Node greatestPCRNode = null; 


    Robot(double battery, double energyCoefficient, List<Node> nodeList){
        this.battery = battery; 
        this.energyCoefficient = energyCoefficient;
        route.add("Inital Depot");
        unvisitedNodes = nodeList;
    }
    
    public double distancefromRobot(Node node){
        int dx = this.x - node.getX();
        int dy = this.y - node.getY();
        return Math.sqrt(dx*dx + dy*dy); 
    }


    public void moveRobotToNode(Node node){
        battery -= this.distancefromRobot(node) * energyCoefficient; 
        x = node.getX();
        y = node.getY(); 
        totalPackets += node.getPrize(); 
        unvisitedNodes.remove(node);
        route.add("Node " + node.getId());
        node.drainNetwork();
    }

    public double getBattery(){
        return battery;
    }

    public void setFeasibleNodes(){
        feasibleNodes.clear(); 
        greatestPCRNode = null;
        double energyToDepot; 
        double energyToNode; 
        for(Node node : unvisitedNodes){
            energyToDepot = Math.sqrt(node.getX()*node.getX() + node.getY() * node.getY()) * energyCoefficient;
            energyToNode = this.distancefromRobot(node) * energyCoefficient;
            if(battery > energyToDepot + energyToNode && node.getPrize() != 0){
                feasibleNodes.add(node);
            }
        }
    }

    public void returnHome(){ 
        battery -= Math.sqrt(this.x * this.x + this.y * this.y) * energyCoefficient;
        this.x = 0; 
        this.y = 0; 
        route.add("Initial Depot");
        System.out.println(this);
    }

    public List<Node> getFeasibleNodes(){
        return feasibleNodes;
    }
    
    public List<String> getRoute(){
        return route; 
    }

    public void findBestPCR(){
        for(Node node : feasibleNodes){
            int prize = node.getPrize(); 
            double cost = this.distancefromRobot(node) * energyCoefficient; 
            double PCR = prize/cost; 
            node.setPCR(PCR);
            if(greatestPCRNode == null || node.getPCR() > greatestPCRNode.getPCR()){
                greatestPCRNode = node; 
            }
        }
    }

    public Node getGreatestNode(){
        return greatestPCRNode;
    }

    public List<Node> getUnvisitedNodes(){
        return unvisitedNodes;
    }

    public int getX(){ 
        return x;
    }

    public int getY(){ 
        return y; 
    }

    public String toString(){ 
        return String.format("Robot Position: (%d, %d) \nLeftover battery: %f  Total Packets: %d Route: %s", this.x, this.y, this.battery, this.totalPackets, this.route);
    }
}

import java.util.ArrayList;
import java.util.List;
public class Robot {
    private int x = 0; 
    private int y = 0; 
    private int totalPackets; 
    private int visitedPackets; 
    private int coveredPackets; 
    private double battery; 
    private List<Node> unvisitedNodes = new ArrayList<>(); 
    private List<Node> feasibleNodes = new ArrayList<>();
    private List<Node> route = new ArrayList<>(); 
    private Node greatestPCRNode = null; 
    private Node initialDepot; 
    private double totalDistance; 


    Robot(double battery, List<Node> nodeList){
        this.battery = battery; 
        initialDepot = new Node(); 
        route.add(initialDepot);
        unvisitedNodes = nodeList;
    }
    
    public double distanceFromRobot(Node node){
        int dx = this.x - node.getX();
        int dy = this.y - node.getY();
        return Math.sqrt(dx*dx + dy*dy); 
    }

    public void moveRobotToNode(Node node){
        totalDistance += this.distanceFromRobot(node); 
        battery -= this.distanceFromRobot(node)/36; 
        x = node.getX();
        y = node.getY(); 
        visitedPackets += node.getPackets();
        coveredPackets += node.getPrize() - node.getPackets(); 
        totalPackets += node.getPrize(); 
        unvisitedNodes.remove(node);
        route.add(node);
        node.drainNetwork();
    }

    public void setFeasibleNodes(){
        feasibleNodes.clear(); 
        greatestPCRNode = null;
        double energyToDepot; 
        double energyToNode; 
        for(Node node : unvisitedNodes){
            energyToDepot = Math.sqrt(node.getX()*node.getX() + node.getY() * node.getY())/36;
            energyToNode = this.distanceFromRobot(node)/36;
            if(battery > energyToDepot + energyToNode && node.getPrize() != 0){
                feasibleNodes.add(node);
            }
        }
    }


    public void returnHome(){ 
        totalDistance += Math.sqrt(this.x * this.x + this.y * this.y);
        battery -= Math.sqrt(this.x * this.x + this.y * this.y)/36;
        this.x = 0; 
        this.y = 0; 
        route.add(initialDepot);
        System.out.println(this);

        String routeStr = "";
        for(Node node : route){
            if(node.getId() == 0){
                routeStr += "Initial Depot -> "; 
            }
            else{
                routeStr += String.format("Node #%d -> ", node.getId());
            }
        }
        System.out.println("route: " +routeStr);
        System.out.println("Total Distance: " + totalDistance);
    }

    public List<Node> getFeasibleNodes(){
        return feasibleNodes;
    }
    
    public List<Node> getRoute(){
        return route; 
    }

    public void findBestPCR(){
        for(Node node : feasibleNodes){
            int prize = node.getPrize(); 
            double cost = this.distanceFromRobot(node); 
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

    public String toString(){ 
        return String.format("Robot Position: (%d, %d) \nLeftover battery: %f (%f meters)  \nTotal Packets: %d" +
         "\nPackets from Visiting: %d \nPackets from Covering: %d", this.x, this.y, this.battery, this.battery * 36, this.totalPackets, this.visitedPackets, this.coveredPackets);
   
   
        }


public double getBattery() {
    return battery;
}

public double getTotalDistance() {
    return totalDistance;
}

public int getTotalPackets() {
    return totalPackets;
}
}

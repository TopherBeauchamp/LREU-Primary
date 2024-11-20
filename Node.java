import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node {
    private int x; 
    private int y;
    private int id;
    private int numPackets; 
    private int minPackets; 
    private int maxPackets;     
    private static Map<Integer, Node> nodeRegistry= new HashMap<>(); 
    private static Random rand = new Random();  
    private int prize; 
    private List<Node> network = new ArrayList<>(); 
    private double PCR = 0.0; 
    
    // Constructor that creates a node with random position
    public Node(int id, int maxWidth, int maxLength, int minPackets, int maxPackets) {
        this.id = id;
        this.x = rand.nextInt(maxWidth + 1);  // random number from 0 to maxWidth-1
        this.y = rand.nextInt(maxLength + 1); // random number from 0 to maxLength-1
        this.numPackets = rand.nextInt(maxPackets - minPackets +1) + minPackets; 
        registerNode(this);
    }

    public Node(int id, int x, int y, int packets){
        this.id = id; 
        this.x = x; 
        this.y = y; 
        this.numPackets = packets; 
        registerNode(this);
    }
    
    public void addToNetwork(Node neighbor){
        this.network.add(neighbor);
    }

    public void drainNetwork(){
        this.drainPackets();
        for(Node node : this.network){
            node.drainPackets();
        }
        this.prize = 0;
    }

    public void setPrize(int prize){
        this.prize = prize;
    }

    public List<Node> getNetwork(){
        return network; 
    }
    public int getPrize(){
        return prize; 
    }

    public void setPCR(double PCR){ 
        this.PCR = PCR; 
    }

    public double getPCR(){
        return PCR;
    }

    public int getX(){
        return x; 
    }

    public int getY(){
        return y; 
    }

    public int getId(){ 
        return id; 
    }

    public int getPackets(){ 
        return numPackets;
    }

    public void drainPackets() {
        numPackets = 0; 
    }

    public static Node getNodeById(int id){ 
        return nodeRegistry.get(id);
    }

    @Override
    public String toString() {
        return String.format("Node #%d (%d,%d) %d packets, %d prize, & %f PCR", this.getId(), this.getX(), this.getY(), this.numPackets, this.getPrize(), this.getPCR());
    }

    public static void registerNode(Node node){
        nodeRegistry.put(node.getId(), node);
    }

    public double getDistance(Node other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx*dx + dy*dy);  
    }
}
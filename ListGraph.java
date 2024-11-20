import java.util.List;
import java.util.ArrayList;


public class ListGraph{
    /*
     * This adjacency list graph representation stores a list of edges 
     * for each node created. The edge represents a connection between
     * one node and another. Since List variables start at 0 and our nodeId's 
     * start at 1, a lot of instantiation in the adjacency list is done by subtracting 
     * a node's ID value by 1, & getting values from adj list is done by vice versa 
    */
    private List<List<Edge>> adjList;

    ListGraph(int numVertices) {
        adjList = new ArrayList<>(numVertices);
        for (int i=0; i<numVertices; i++)
            adjList.add(new ArrayList<>());
    }

    public void addEdge(Node sourceNode, Node connectedNode) {
        adjList.get(sourceNode.getId()-1).add(new Edge(sourceNode.getId(), connectedNode.getId(), sourceNode.getPackets(), connectedNode.getDistance(sourceNode)));
        adjList.get(connectedNode.getId()-1).add(new Edge(connectedNode.getId(), sourceNode.getId(), connectedNode.getPackets(), sourceNode.getDistance(connectedNode)));
    }

    public List<List<Edge>> getAdjList(){ 
        return adjList; 
    }
    

    /*
     * This calculatePrize method iterates through a given nodes's edges 
     * that are associated with it through the adj list variable. There's 
     * also a boolean parameter, that prevents nodes from repeatedly being 
     * added to the List<Node> network variable. This is useful because 
     * initially we do want this method to create a network from the nodes 
     * connected to the given node 
     */
    public int calculatePrize(int nodeId, boolean initial){
        List<Edge> edgesOfSourceNode = adjList.get(nodeId-1);
        Node sourceNode = Node.getNodeById(nodeId);
        int initialPrize = sourceNode.getPackets(); 
        for(int i = 0; i < edgesOfSourceNode.size(); i++){
            Node connectedNode = Node.getNodeById(edgesOfSourceNode.get(i).getDestination());
            initialPrize += connectedNode.getPackets();
            if(initial){
                sourceNode.addToNetwork(connectedNode);
            }
        }
        return initialPrize; 
    }


    /* 
     * This method 
     */
    public void updatePrizes(List<Node> network){
        for(Node node : network){
            node.setPrize(this.calculatePrize(node.getId(), false));
        }
        int nodeID = 0; 
        for(List<Edge> edgeList : this.getAdjList()){
            nodeID++; 
            for(Edge edge : edgeList){
                Node node = Node.getNodeById(nodeID);
                Node neighbor = Node.getNodeById(edge.getDestination());
                if(network.contains(neighbor)){
                    node.setPrize(this.calculatePrize(nodeID, false));
                }
            }
        }
    }

    
} 
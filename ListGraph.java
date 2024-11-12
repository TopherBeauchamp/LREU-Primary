import java.util.List;
import java.util.ArrayList;

//Adjacency list class from https://www.algotree.org/algorithms/adjacency_list/graph_as_adjacency_list_java/

public class ListGraph{

    private int numVertices;
    private List<List<Edge>> adjList;

    ListGraph(int numVertices) {
        this.numVertices = numVertices;
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
    
    public int calculateInitialPrize(int nodeId){
        List<Edge> edgesOfSourceNode = adjList.get(nodeId-1);
        Node sourceNode = Node.getNodeById(nodeId);
        int initialPrize = sourceNode.getPackets(); 
        for(int i = 0; i < edgesOfSourceNode.size(); i++){
            Node connectedNode = Node.getNodeById(edgesOfSourceNode.get(i).getDestination());
            initialPrize += connectedNode.getPackets();
            sourceNode.addToNetwork(connectedNode);
        }
        return initialPrize; 
    }
} 
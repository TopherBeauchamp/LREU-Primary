import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class InputNetwork {
    private List<Node> nodeList;
    private ListGraph graph;
    private int transmissionRange;

    // This constructor takes in a transmission range & a network file and runs the file through a fileReader 
    public InputNetwork(String filename, int transmissionRange) throws FileNotFoundException {
        this.transmissionRange = transmissionRange;
        this.nodeList = new ArrayList<>();
        readNetworkFromFile(filename);
        createGraph();
    }

    // This is where the file gets read; splitting each line into four parts, nodeID, x, y, and packets 
    private void readNetworkFromFile(String filename) throws FileNotFoundException {
        Scanner fileScanner = new Scanner(new File(filename));
        
        while (fileScanner.hasNextLine()) {
            String[] parts = fileScanner.nextLine().trim().split("\\s+");
            if (parts.length == 4) {
                int id = Integer.parseInt(parts[0]);
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int packets = Integer.parseInt(parts[3]);
                
                // Create a custom node with predefined coordinates and packets
                Node node = new Node(id, x, y, packets);
                nodeList.add(node);
            }
        }
        fileScanner.close();
    }

    private void createGraph() {
        graph = new ListGraph(nodeList.size());

        // Add edges based on transmission range
        for (int i = 0; i < nodeList.size(); i++) {
            for (int j = i + 1; j < nodeList.size(); j++) {
                if (nodeList.get(i).getDistance(nodeList.get(j)) <= transmissionRange) {
                    graph.addEdge(nodeList.get(i), nodeList.get(j));
                }
            }
        }

        // Calculate prizes for nodes
        for (Node node : nodeList) {
            int prize = graph.calculatePrize(node.getId(), true);
            node.setPrize(prize);
        }
    }

    // Getter Methods 
    public ListGraph getGraph() {
        return graph;
    }

    public List<Node> getNodeList() {
        return nodeList;
    }
}
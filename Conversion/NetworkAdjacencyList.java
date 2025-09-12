import java.io.*;
import java.util.*;

public class NetworkAdjacencyList {
    public static void main(String[] args) {
        String filename = "Conversion\\N3.txt";
        System.out.println(filename);
        List<Node> nodes = readNodesFromFile(filename);
        double[][] costMatrix = computeAdjacencyMatrix(nodes);
        int[] packets = extractPackets(nodes);
        printMatrix(costMatrix);
        printPackets(packets);
    }

    static class Node {
        int id;
        double x, y;
        int packets;

        Node(int id, double x, double y, int packets) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.packets = packets;
        }
    }

    private static List<Node> readNodesFromFile(String filename) {
        List<Node> nodes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 4) {
                    int id = Integer.parseInt(parts[0]);
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    int packets = Integer.parseInt(parts[3]);
                    nodes.add(new Node(id, x, y, packets));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nodes;
    }

    private static double[][] computeAdjacencyMatrix(List<Node> nodes) {
        int n = nodes.size();
        double[][] cost = new double[n][n];
        final double INF = 9999.99;
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    cost[i][j] = INF;
                } else {
                    cost[i][j] = euclideanDistance(nodes.get(i), nodes.get(j));
                }
            }
        }
        return cost;
    }

    private static double euclideanDistance(Node a, Node b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    private static void printMatrix(double[][] matrix) {
        System.out.println("Cost = [");
        for (int i = 0; i < matrix.length; i++) {
            System.out.print("[");
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.printf("%.2f", matrix[i][j]);
                if (j < matrix[i].length - 1) System.out.print(", ");
            }
            System.out.println("]" + (i < matrix.length - 1 ? "," : ""));
        }
        System.out.println("];");
    }

    private static int[] extractPackets(List<Node> nodes) {
        int[] packets = new int[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            packets[i] = nodes.get(i).packets;
        }
        return packets;
    }

    private static void printPackets(int[] packets) {
        System.out.print("p = [");
        for (int i = 0; i < packets.length; i++) {
            System.out.print(packets[i]);
            if (i < packets.length - 1) System.out.print(", ");
        }
        System.out.println("];\n");
    }
}

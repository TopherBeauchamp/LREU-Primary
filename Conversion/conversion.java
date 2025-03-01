import java.io.*;
import java.util.*;

public class NetworkAdjacencyList {
    public static void main(String[] args) {
        String filename = "N1.txt";
        List<Node> nodes = readNodesFromFile(filename);
        double[][] costMatrix = computeAdjacencyMatrix(nodes);
        printMatrix(costMatrix);
    }

    static class Node {
        int id;
        double x, y;

        Node(int id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private static List<Node> readNodesFromFile(String filename) {
        List<Node> nodes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 3) {
                    int id = Integer.parseInt(parts[0]);
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    nodes.add(new Node(id, x, y));
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
}

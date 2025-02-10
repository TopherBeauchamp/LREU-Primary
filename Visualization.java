/*
 * These imports are from the java.awt library, which deals with 
 * the individual graphics that will be used to display individual
 * sensor nodes and their respective connections 
 */
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D; 
import java.awt.Point; // represents a location (x,y) in a 2D space 
import java.awt.RenderingHints; // Controls rendering quality
import java.awt.Stroke;  // Outlines of shapes
import java.awt.geom.Ellipse2D; //Circles 

//Data structure imports
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


// These imports deal with the outline of the graph 
import javax.swing.JFrame; // Create a window to hold all components
import javax.swing.JPanel; // Container for holding and managing components

// These deal with panning and zooming 
import java.awt.event.MouseWheelEvent; 
import java.awt.event.MouseWheelListener;

public class Visualization extends JPanel implements Runnable {
    private List<Node> nodes;
    private double graphWidth;
    private double graphHeight;
    private int ovalSize = 6;
    private double zoomFactor = 1.0;

    public Visualization(List<Node> nodes, double width, double height) {
        this.nodes = nodes;
        graphWidth = width;
        graphHeight = height;

        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getWheelRotation() < 0) {
                    zoomFactor *= 1.1;
                } else {
                    zoomFactor /= 1.1;
                }
                repaint();
            }
        });
        invalidate();
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Normalize node coordinates
        double xMin = nodes.stream().mapToDouble(Node::getX).min().orElse(0);
        double xMax = nodes.stream().mapToDouble(Node::getX).max().orElse(1);
        double yMin = nodes.stream().mapToDouble(Node::getY).min().orElse(0);
        double yMax = nodes.stream().mapToDouble(Node::getY).max().orElse(1);

        double padding = 50;
        double newXMin = padding;
        double newXMax = getWidth() - padding;
        double newYMin = padding;
        double newYMax = getHeight() - padding;

        // Create graph points
        List<Point> graphPoints = new ArrayList<>();
        for (Node node : nodes) {
            double normX = (node.getX() - xMin) / (xMax - xMin);
            double normY = (node.getY() - yMin) / (yMax - yMin);
            
            double x1 = newXMin + normX * (newXMax - newXMin) * zoomFactor;
            double y1 = newYMax - normY * (newYMax - newYMin) * zoomFactor;
            
            graphPoints.add(new Point((int) x1, (int) y1));
        }

        // Background
        g2.setColor(Color.white);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Draw nodes
        g2.setColor(Color.black);
        for (Point p : graphPoints) {
            Ellipse2D.Double shape = new Ellipse2D.Double(p.x - ovalSize, p.y - ovalSize, ovalSize * 2, ovalSize * 2);
            g2.fill(shape);
        }

        // Draw node IDs and packet counts
        g2.setColor(Color.red);
        FontMetrics metrics = g2.getFontMetrics();
        Font originalFont = g2.getFont();
        Font largerFont = originalFont.deriveFont(originalFont.getSize() + 20f);
        g2.setFont(largerFont);

        int textHeight = metrics.getHeight();  // Get height of the text
        int textPadding = 5;  // Adjust spacing between text and node
        for (int i = 0; i < nodes.size(); i++) {
            Node currentNode = nodes.get(i);
            Point currentPoint = graphPoints.get(i);
            
            // Draw node ID (above the node)
            String nodeId = String.valueOf(currentNode.getId());
            int nodeIdWidth = metrics.stringWidth(nodeId);
            
            double x = currentPoint.x - (nodeIdWidth / 2.0);
            double y = currentPoint.y - textHeight - textPadding * 3;
            
            g2.setColor(Color.red);
            g2.setFont(largerFont.deriveFont(Font.BOLD));
            g2.drawString(nodeId, (int) x, (int) y);
            
            // Draw packet count (right next to node ID)
            String packetCount = String.format("(%d)", currentNode.getPackets());
            
            double xData = x + nodeIdWidth + (textPadding * 5); // Add extra spacing
            double yData = y; // Align with node ID
        
            g2.setColor(Color.darkGray);
            g2.drawString(packetCount, (int) xData, (int) yData);
            
            // Debugging output to check positions
            System.out.printf("Node %d -> Pos: (%.1f, %.1f), Packet Pos: (%.1f, %.1f)%n",
                              currentNode.getId(), x, y, xData, yData);
        }
        g2.setStroke(new BasicStroke(2f));
    }

    public void run() {
        JFrame frame = new JFrame("Sensor Network Graph");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(this);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
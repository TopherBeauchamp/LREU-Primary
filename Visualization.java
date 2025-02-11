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
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;

public class Visualization extends JPanel implements Runnable {
    private List<Node> nodes;
    private double graphWidth;
    private double graphHeight;
    private int ovalSize = 6;
    private double zoomFactor = .8;
    private List<Node> route; 
    private int transmission;
    private static final int PADDING = 50; 


    private int xOffset = 0, yOffset = 0;
    private int lastMouseX, lastMouseY;

    public Visualization(List<Node> nodes, double width, double height, List<Node> route, int transmission) {
        this.nodes = nodes;
        graphWidth = width;
        graphHeight = height;
        this.route = route; 
        this.transmission = transmission;

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
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;
                xOffset += dx;
                yOffset += dy;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                repaint();
            }
        });

        invalidate();
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Ensure the entire background is white
    g2.setColor(Color.white);
    g2.fillRect(0, 0, getWidth(), getHeight());
    
        // Apply zoom and translation
        g2.translate(getWidth() / 2 + xOffset, getHeight() / 2 + yOffset); 
        g2.scale(zoomFactor, zoomFactor);  
        g2.translate(-getWidth() / 2, -getHeight() / 2);
    
        // Background
        g2.setColor(Color.white);
        g2.fillRect(0, 0, getWidth(), getHeight());
    
        // Draw nodes
        g2.setColor(Color.black);
        for (Node node : nodes) {
            int x = (int) (node.getX() * zoomFactor);
            int y = (int) ((getHeight() - node.getY()) * zoomFactor);  // Flip Y-axis with zoom
            Ellipse2D.Double shape = new Ellipse2D.Double(x - ovalSize, y - ovalSize, ovalSize * 2, ovalSize * 2);
            g2.fill(shape);
        }
    
        // Draw transmission ranges
        g2.setColor(new Color(0, 0, 255, 50)); // Transparent blue
        for (Node node : nodes) {
            if (node.getId() == 0) continue;
            int x = (int) (node.getX() * zoomFactor);
            int y = (int) ((getHeight() - node.getY()) * zoomFactor);
            int radius = (int) (transmission * zoomFactor);
            g2.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
        }
    
        // Draw node IDs and packet counts
        g2.setColor(Color.red);
        FontMetrics metrics = g2.getFontMetrics();
        Font originalFont = g2.getFont();
        Font largerFont = originalFont.deriveFont(originalFont.getSize() + 15f);
        g2.setFont(largerFont);
    
        for (Node node : nodes) {
            int x = (int) (node.getX() * zoomFactor);
            int y = (int) ((getHeight() - node.getY()) * zoomFactor);
    
            String nodeId = String.valueOf(node.getId());
            int nodeIdWidth = metrics.stringWidth(nodeId);
            
            g2.setColor(Color.red);
            g2.drawString(nodeId, x - (nodeIdWidth / 2), y - 10);
    
            String packetCount = String.format("(%d)", node.getPackets());
            g2.setColor(Color.darkGray);
            g2.drawString(packetCount, x + (nodeIdWidth / 2) + 16, y - 10);
        }
    
        // Draw the robot's route
        if (route != null && route.size() > 1) {
            g2.setColor(Color.blue);
            g2.setStroke(new BasicStroke(2));
    
            for (int i = 0; i < route.size() - 1; i++) {
                Node current = route.get(i);
                Node next = route.get(i + 1);
                g2.drawLine((int) (current.getX() * zoomFactor), (int) ((getHeight() - current.getY()) * zoomFactor), 
                            (int) (next.getX() * zoomFactor), (int) ((getHeight() - next.getY()) * zoomFactor));
            }
        }
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
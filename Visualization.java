/*
 * These imports are from the java.awt library, which deals with 
 * the individual graphics that will be used to display individual
 * sensor nodes and their respective connections 
 */
import java.awt.BasicStroke;
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
    private int scaling = 25;
    private int ovalSize = 6;

    public Visualization(List<Node> nodes, double width, double height){
        this.nodes = nodes; 
        graphWidth = width; 
        graphHeight = height; 

    
        invalidate(); // Marks component for needing updates 
        repaint();  // calls paintComponent, should never straight up call paintComponent 
    }

    // Protected because that's the visibility of the inherited method 
    protected void paintComponent(Graphics g){
        super.paintComponent(g); // Calling paintComponent method of superclass, which in this case is JPanel 
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Enables Antialiasing 

        // Scaling that will be applied to the graph in order to condense it 
        double xScale = ((getWidth() - 3 * scaling) / (graphWidth));
        double yScale = ((getHeight() - 3 * scaling) / (graphHeight));

        // Creating points on the graph for each node 
        List<Point> graphPoints = new ArrayList<Point>();
        for (Node node : nodes) {
            double x1 = (node.getX() * (xScale) + (2 * scaling));
            double y1 = ((graphHeight - node.getY()) * yScale + scaling);
            Point point = new Point();
            point.setLocation(x1, y1);
            graphPoints.add(point);
        }

        // Setting background to white 
        g2.setColor(Color.white);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Draw ovals in blue
        g2.setColor(Color.black);
        for (int i = 0; i < graphPoints.size(); i++) {
            double x = graphPoints.get(i).x - ovalSize / 2;
            double y = graphPoints.get(i).y - ovalSize / 2;
            double ovalW = ovalSize*4;
            double ovalH = ovalSize*4;
            Ellipse2D.Double shape = new Ellipse2D.Double(x, y, ovalW, ovalH);
            g2.fill(shape);  
        }

        // Draw node IDs in red with increased spacing
        g2.setColor(Color.red);
        FontMetrics metrics = g2.getFontMetrics();
        int textHeight = metrics.getHeight();
        int textPadding = 30; // Adjust space between id and node 
        Font originalFont = g2.getFont();
        Font largerFont = originalFont.deriveFont(originalFont.getSize() + 20f); // adjust for font size
        g2.setFont(largerFont);

        for (int i = 0; i < graphPoints.size(); i++) {
            double x = graphPoints.get(i).x - (metrics.stringWidth("" + nodes.get(i).getId()) / 2.0);
            double y = graphPoints.get(i).y + ovalSize + textHeight + textPadding;
            g2.setColor(Color.red);
            g2.setFont(largerFont.deriveFont(Font.BOLD));
            g2.drawString(String.format("%d", nodes.get(i).getId()), (int) x, (int) y);
            
            // Calculate the x-coordinate for data packets to align them with the ID
            double xData = x + metrics.stringWidth("" + nodes.get(i).getId()) + 22; // Adjust this value as needed
            
            g2.setColor(Color.darkGray);
            g2.drawString(String.format("(%d)", nodes.get(i).getPackets()), (int) xData, (int) y);
        }
        
        Stroke stroke = new BasicStroke(2f);
        g2.setStroke(stroke);


    }

    public void run(){ 
        JFrame frame = new JFrame("Sensor Network Graph"); //This creates the overall window that will hold all components
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

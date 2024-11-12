public class Edge {
    private int source; 
    private int destination; 
    public double distance ;

    Edge(int source, int destination, int numPackets, double distance){ 
        this.source = source; 
        this.destination = destination; 
        this.distance = distance;
    }


    public int getDestination(){
        return destination;
    }

    public double getDistance(){ 
        return distance; 
    }

    public int getSource(){ 
        return source; 
    }

    public String toString(){
        return String.format("Edge: %d source & %d dest", this.getSource(), this.getDestination());
    }
}

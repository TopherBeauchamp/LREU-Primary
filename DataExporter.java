    import java.io.FileWriter;
    import java.io.IOException;
    import java.util.ArrayList;
    import java.util.List;

    /**
     * This class handles exporting simulation results to CSV files
     * for later analysis in Excel.
     */
    public class DataExporter {
        
        // Represents a single run of the algorithm
        public static class SimulationResult {
            private String networkName;
            private int dataPackets;
            private double distanceTraveled;
            private long computationalTime;
            private double batteryLevel;
            
            public SimulationResult(String networkName, int dataPackets, double distanceTraveled, 
                                    long computationalTime, double batteryLevel) {
                this.networkName = networkName;
                this.dataPackets = dataPackets;
                this.distanceTraveled = distanceTraveled;
                this.computationalTime = computationalTime;
                this.batteryLevel = batteryLevel;
            }
            
            public String getNetworkName() { return networkName; }
            public int getDataPackets() { return dataPackets; }
            public double getDistanceTraveled() { return distanceTraveled; }
            public long getComputationalTime() { return computationalTime; }
            public double getBatteryLevel() { return batteryLevel; }
        }
        
        // Groups simulation results by battery level
        public static class BatteryGroup {
            private double batteryLevel;
            private List<SimulationResult> results;
            private double avgDataPackets;
            private double avgDistance;
            private double avgCompTime;
            private double stdDevDataPackets;
            private double stdDevDistance;
            private double confidenceDataPackets;
            private double confidenceDistance;
            private double percentChangeDataPackets;
            private double percentChangeDistance;
            private double percentChangeCompTime;
            
            public BatteryGroup(double batteryLevel) {
                this.batteryLevel = batteryLevel;
                this.results = new ArrayList<>();
            }
            
            public void addResult(SimulationResult result) {
                results.add(result);
            }
            
            public void calculateStatistics() {
                // Calculate averages
                avgDataPackets = results.stream().mapToInt(SimulationResult::getDataPackets).average().orElse(0);
                avgDistance = results.stream().mapToDouble(SimulationResult::getDistanceTraveled).average().orElse(0);
                avgCompTime = results.stream().mapToLong(SimulationResult::getComputationalTime).average().orElse(0);
                
                // Calculate standard deviations
                stdDevDataPackets = calculateStdDev(results.stream().mapToDouble(r -> r.getDataPackets()).toArray(), avgDataPackets);
                stdDevDistance = calculateStdDev(results.stream().mapToDouble(SimulationResult::getDistanceTraveled).toArray(), avgDistance);
                
                // Calculate 95% confidence interval (using t-distribution with n-1 degrees of freedom)
                // For simplicity, using 1.96 for large sample, adjust for small samples
                double tValue = 1.96; // For large samples
                int n = results.size();
                if (n > 1) {
                    confidenceDataPackets = tValue * (stdDevDataPackets / Math.sqrt(n));
                    confidenceDistance = tValue * (stdDevDistance / Math.sqrt(n));
                }
            }
            
            private double calculateStdDev(double[] values, double mean) {
                double sum = 0;
                for (double value : values) {
                    sum += Math.pow(value - mean, 2);
                }
                return Math.sqrt(sum / Math.max(1, values.length - 1));
            }
            
            public void setPercentChanges(double prevDataPackets, double prevDistance, double prevCompTime) {
                if (prevDataPackets > 0) {
                    percentChangeDataPackets = ((avgDataPackets - prevDataPackets) / prevDataPackets) * 100;
                }
                if (prevDistance > 0) {
                    percentChangeDistance = ((avgDistance - prevDistance) / prevDistance) * 100;
                }
                if (prevCompTime > 0) {
                    percentChangeCompTime = ((avgCompTime - prevCompTime) / prevCompTime) * 100;
                }
            }
            
            public double getBatteryLevel() { return batteryLevel; }
            public List<SimulationResult> getResults() { return results; }
            public double getAvgDataPackets() { return avgDataPackets; }
            public double getAvgDistance() { return avgDistance; }
            public double getAvgCompTime() { return avgCompTime; }
            public double getStdDevDataPackets() { return stdDevDataPackets; }
            public double getStdDevDistance() { return stdDevDistance; }
            public double getConfidenceDataPackets() { return confidenceDataPackets; }
            public double getConfidenceDistance() { return confidenceDistance; }
            public double getPercentChangeDataPackets() { return percentChangeDataPackets; }
            public double getPercentChangeDistance() { return percentChangeDistance; }
            public double getPercentChangeCompTime() { return percentChangeCompTime; }
        }
        
        private List<BatteryGroup> batteryGroups;
        
        public DataExporter() {
            batteryGroups = new ArrayList<>();
        }
        
        public void addResult(SimulationResult result) {
            // Find or create the appropriate battery group
            BatteryGroup group = batteryGroups.stream()
                    .filter(g -> g.getBatteryLevel() == result.getBatteryLevel())
                    .findFirst()
                    .orElse(null);
            
            if (group == null) {
                group = new BatteryGroup(result.getBatteryLevel());
                batteryGroups.add(group);
            }
            
            group.addResult(result);
        }
        
        public void calculateAllStatistics() {
            // Sort battery groups by battery level
            batteryGroups.sort((g1, g2) -> Double.compare(g1.getBatteryLevel(), g2.getBatteryLevel()));
            
            // Calculate statistics for each group
            BatteryGroup prevGroup = null;
            for (BatteryGroup group : batteryGroups) {
                group.calculateStatistics();
                
                if (prevGroup != null) {
                    group.setPercentChanges(
                        prevGroup.getAvgDataPackets(),
                        prevGroup.getAvgDistance(), 
                        prevGroup.getAvgCompTime()
                    );
                }
                
                prevGroup = group;
            }
        }
        
        public void exportToCSV(String filename) throws IOException {
            try (FileWriter writer = new FileWriter(filename)) {
                // Write headers
                writer.write("PCA Algorithm,Data Packets,Distance Traveled,Computational Time\n");
                
                // Process each battery group
                for (BatteryGroup group : batteryGroups) {
                    // Write battery level header
                    writer.write(String.format("%.0fWh\n", group.getBatteryLevel()));
                    
                    // Write individual network results
                    for (int i = 0; i < group.getResults().size(); i++) {
                        SimulationResult result = group.getResults().get(i);
                        writer.write(String.format("Network %d,%d,%.2f,%d\n", 
                            i + 1, 
                            result.getDataPackets(),
                            result.getDistanceTraveled(),
                            result.getComputationalTime()
                        ));
                    }
                    
                    // Write statistics
                    writer.write(String.format("Stan Dev,%.8f,%.8f\n", 
                        group.getStdDevDataPackets(), 
                        group.getStdDevDistance()
                    ));
                    
                    writer.write(String.format("Confidence,%.8f,%.8f\n", 
                        group.getConfidenceDataPackets(), 
                        group.getConfidenceDistance()
                    ));
                    
                    writer.write(String.format("Average,%.2f,%.2f,%d\n", 
                        group.getAvgDataPackets(),
                        group.getAvgDistance(),
                        (long)group.getAvgCompTime()
                    ));
                    
                    writer.write(String.format("Pecent Change,%.8f,%.8f,%.8f\n\n", 
                        group.getPercentChangeDataPackets(),
                        group.getPercentChangeDistance(),
                        group.getPercentChangeCompTime()
                    ));
                }
            }
        }
    }

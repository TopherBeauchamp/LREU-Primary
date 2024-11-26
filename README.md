# Primary LREU Project
In this project, I represent a robot traversing through a network of data sensor nodes using an adjacency list, and by implementing a custom greedy algorithm, the robot is able to maximize 
profit (data packets from nodes) and minimize cost (distance travelled/energy depleted).  

# Purpose: 
The purpose of this project is to represent Professor Bin Tang's battery-constrained covering salesman problem that we plan to incorperate into a research paper. 


# To Do 
1. Utilize a graphics library to visualize data created by this project in order to create graphs & charts to be used in a research paper
2. Create tests to ensure that data created is accurate


# Usage 
If you wish to use this code you can clone it by entering the following command into a terminal:
```
git clone https://github.com/TopherBeauchamp/LREU-Final
```
Below compiling, please change the path given in the sensorNetworkRunner.java file @ line 39 to the path where your network files are stored.
This is where the change must be implemented: 
```
  /* *********************** IMPORTANT **************************
   * The path below must be altered in order for this to work properly 
   * in your enviornment. I'm currently working on incorperating a .env 
   * file to make this process easier 
   */
   String fullFilePath = "C:\\Users\\tophe\\OneDrive\\Desktop\\LREU-Final\\Networks\\" + filename;
```
The code can be compiled in a terminal with the following commands:

### Compile Java Files: 
```
javac *.java
```
### Run main method: 
```
java SensorNetworkRunner
```

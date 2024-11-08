import java.util.HashMap;
import java.util.Map;

public class NodeRegistry {
    private static Map<Integer, Node> map = new HashMap<>(); 

    public static void registerNode(Node node){
        map.put(node.getId(), node);
    }

    public static Node getNodeById(int id){ 
        return map.get(id);
    }
}

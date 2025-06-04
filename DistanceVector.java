import java.util.*;

/**
 * Represents a neighboring node in the network graph with its associated cost.
 * Used to store adjacency information for each node.
 */
class Neighbor {
    private String name;
    private int cost;

    public Neighbor(String name, int cost) {
        this.name = name;
        this.cost = cost;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }
}

/**
 * Represents a network graph using an adjacency list structure.
 * Supports adding nodes, adding edges with weights, and removing edges.
 */
class Graph {
    private final Map<String, List<Neighbor>> adjList = new HashMap<>();

    public Map<String, List<Neighbor>> getAdjList() {
        return adjList;
    }

    public void addNode(String node) {
        adjList.putIfAbsent(node, new ArrayList<>());
    }

    public void addEdge(String u, String v, int weight) {
        if (weight >= 0) {
            // Add bidirectional edge with positive weight
            adjList.computeIfAbsent(u, k -> new ArrayList<>())
                   .add(new Neighbor(v, weight));
            adjList.computeIfAbsent(v, k -> new ArrayList<>())
                   .add(new Neighbor(u, weight));
        } else {
            // Negative weight indicates edge removal
            updateEdge(u, v);
        }
    }

    public void updateEdge(String u, String v) {
        // Remove v from u's neighbor list
        List<Neighbor> listU = adjList.get(u);
        if (listU != null) {
            listU.removeIf(neighbor -> neighbor.getName().equals(v));
        }
        
        // Remove u from v's neighbor list
        List<Neighbor> listV = adjList.get(v);
        if (listV != null) {
            listV.removeIf(neighbor -> neighbor.getName().equals(u));
        }
    }
}

/**
 * Represents a distance table entry containing distances to a destination
 * via different intermediate nodes (next hops).
 */
class DistanceList {
    private final Map<String, String> distances = new HashMap<>();

    public Map<String, String> getDistances() {
        return distances;
    }
}

/**
 * Implementation of the Distance Vector Routing Algorithm.
 * Simulates distributed routing where each node maintains distance tables
 * and exchanges information with neighbors to compute shortest paths.
 */
public class DistanceVector {
    /** Global tick counter to track algorithm iterations */
    private static int tick = 0;

    /**
     * Copies a 2D matrix of Neighbor objects from source to destination.
     * 
     * @param dest the destination matrix
     * @param src the source matrix to copy from
     */
    private static void copyMatrix(Neighbor[][] dest, Neighbor[][] src) {
        int n = src.length;
        for (int i = 0; i < n; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, n);
        }
    }

    /**
     * Finds the best next hop (minimum cost path) from source to destination
     * based on the distance table entries.
     * 
     * @param table the distance table containing all routing information
     * @param from the source node
     * @param to the destination node
     * @param nodeIndex mapping from node names to array indices
     * @return the best neighbor to route through, or null if unreachable
     */
    private static Neighbor findMinHop(DistanceList[][] table, String from, String to,
                                      Map<String, Integer> nodeIndex) {
        DistanceList dl = table[nodeIndex.get(from)][nodeIndex.get(to)];
        int bestCost = Integer.MAX_VALUE;
        Neighbor bestNeighbor = new Neighbor("", bestCost);
        int countInf = 0;

        // Iterate through all possible next hops to find the minimum cost path
        for (Map.Entry<String, String> entry : dl.getDistances().entrySet()) {
            String via = entry.getKey();
            String costStr = entry.getValue();
            if ("INF".equals(costStr)) {
                countInf++;
            } else {
                int cost = Integer.parseInt(costStr);
                // Choose minimum cost, break ties by lexicographic order
                if (cost < bestCost ||
                    (cost == bestCost && via.compareTo(bestNeighbor.getName()) < 0)) {
                    bestCost = cost;
                    bestNeighbor.setName(via);
                    bestNeighbor.setCost(cost);
                }
            }
        }

        // Return null if all paths are infinite (unreachable)
        if (countInf == dl.getDistances().size()) {
            return null;
        }
        return bestNeighbor;
    }

    /**
     * Initializes the distance tables and minimum cost matrices with direct neighbor costs.
     * 
     * @param neighborCosts 2D array storing direct costs between adjacent nodes
     * @param minCost 2D array storing best known paths between all node pairs
     * @param nodeIndex mapping from node names to array indices
     * @param graph the adjacency list representation of the network
     */
    private static void initializeTables(int[][] neighborCosts, Neighbor[][] minCost,
                                         Map<String, Integer> nodeIndex, Map<String, List<Neighbor>> graph) {
        // Initialize diagonal (cost to self is 0)
        for (String node : nodeIndex.keySet()) {
            int idx = nodeIndex.get(node);
            minCost[idx][idx] = new Neighbor(node, 0);
        }

        // Initialize direct neighbor costs
        for (Map.Entry<String, List<Neighbor>> entry : graph.entrySet()) {
            String u = entry.getKey();
            int ui = nodeIndex.get(u);
            for (Neighbor neigh : entry.getValue()) {
                String v = neigh.getName();
                int vi = nodeIndex.get(v);
                neighborCosts[ui][vi] = neigh.getCost();
            }
        }
    }

    /**
     * Builds a mapping from node names to array indices for matrix operations.
     * Nodes are sorted alphabetically to ensure consistent ordering.
     * 
     * @param graph the network graph
     * @return mapping from node names to indices
     */
    private static Map<String, Integer> buildIndexMap(Graph graph) {
        List<String> nodes = new ArrayList<>(graph.getAdjList().keySet());
        Collections.sort(nodes); // Ensure consistent alphabetical ordering
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            indexMap.put(nodes.get(i), i);
        }
        return indexMap;
    }

    /**
     * Utility method to print text with fixed-width padding for table formatting.
     * 
     * @param text the text to print
     * @param width the total width to pad to
     */
    private static void pad(String text, int width) {
        System.out.print(text);
        for (int i = text.length(); i < width; i++) {
            System.out.print(" ");
        }
    }

    /**
     * Prints the distance tables for all nodes at the current tick.
     * Shows the cost to reach each destination via each possible next hop.
     * 
     * @param nodes set of all nodes in the network
     * @param table the distance table containing routing information
     * @param nodeIndex mapping from node names to array indices
     */
    private static void printDistanceTables(Set<String> nodes, DistanceList[][] table,
                                            Map<String, Integer> nodeIndex) {
        for (String src : nodes) {
            System.out.println("Distance Table of router " + src + " at t=" + tick + ":");
            
            // Print header row with destination nodes
            pad("", 5);
            for (String dest : nodes) {
                if (!dest.equals(src)) pad(dest, 5);
            }
            System.out.println();

            // Print each row showing costs via different next hops
            for (String dest : nodes) {
                if (dest.equals(src)) continue;
                pad(dest, 5);
                DistanceList dl = table[nodeIndex.get(src)][nodeIndex.get(dest)];
                for (String via : nodes) {
                    if (via.equals(src)) continue;
                    String cost = dl.getDistances().getOrDefault(via, "INF");
                    pad(cost, 5);
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    /**
     * Prints the final routing tables for all nodes.
     * Shows the next hop and total cost to reach each destination.
     * 
     * @param nodes set of all nodes in the network
     * @param minCost 2D array containing best paths between all node pairs
     * @param nodeIndex mapping from node names to array indices
     */
    private static void printRoutingTables(Set<String> nodes, Neighbor[][] minCost,
                                           Map<String, Integer> nodeIndex) {
        for (String src : nodeIndex.keySet()) {
            System.out.println("Routing Table of router " + src + ":");
            int i = nodeIndex.get(src);
            for (String dest : nodes) {
                if (dest.equals(src)) continue;
                int j = nodeIndex.get(dest);
                Neighbor via = minCost[i][j];
                String nextHop = (via == null) ? "INF" : via.getName();
                String costStr = (via == null) ? "INF" : String.valueOf(via.getCost());
                System.out.println(dest + "," + nextHop + "," + costStr);
            }
            System.out.println();
        }
    }

    /**
     * Executes the main distance vector algorithm loop.
     * Iteratively updates distance tables until convergence is reached.
     * 
     * @param neighborCosts direct costs between adjacent nodes
     * @param minCost best known paths between all node pairs
     * @param nodeIndex mapping from node names to array indices
     * @param table distance tables for all nodes
     */
    private static void runDistanceVector(int[][] neighborCosts, Neighbor[][] minCost,
                                          Map<String, Integer> nodeIndex, DistanceList[][] table) {
        Set<String> nodes = nodeIndex.keySet();
        int n = nodes.size();
        boolean changed = true;

        // Continue until no changes occur (convergence)
        while (changed) {
            Neighbor[][] newMinCost = new Neighbor[n][n];
            copyMatrix(newMinCost, minCost);
            int stableCount = 0;

            // For each source node
            for (String src : nodes) {
                int si = nodeIndex.get(src);
                // For each destination node
                for (String dest : nodes) {
                    if (dest.equals(src)) continue;
                    int di = nodeIndex.get(dest);
                    
                    // For each possible intermediate node (next hop)
                    for (String via : nodes) {
                        if (via.equals(src)) continue;
                        int vi = nodeIndex.get(via);
                        
                        // Calculate cost: src -> via + via -> dest
                        int costToVia = neighborCosts[si][vi];
                        Neighbor bestVia = minCost[vi][di];
                        int costViaDest = (bestVia == null) ? -1 : bestVia.getCost();
                        
                        // Determine new cost (INF if any segment is unreachable)
                        String newCost = (costToVia < 0 || costViaDest < 0)
                                         ? "INF"
                                         : String.valueOf(costToVia + costViaDest);

                        // Update distance table if cost has changed
                        DistanceList dl = table[si][di];
                        if (dl == null || !newCost.equals(dl.getDistances().get(via))) {
                            if (dl == null) table[si][di] = new DistanceList();
                            table[si][di].getDistances().put(via, newCost);
                            changed = true;
                        } else {
                            stableCount++;
                        }
                    }
                    // Update minimum cost path for this src-dest pair
                    newMinCost[si][di] = findMinHop(table, src, dest, nodeIndex);
                }
            }

            // Check for convergence (all entries stable)
            int expectedStable = n * (n - 1) * (n - 1);
            if (stableCount == expectedStable) {
                changed = false;
                tick--; // Adjust tick since we incremented it unnecessarily
            } else {
                // Print intermediate state if still changing
                printDistanceTables(nodes, table, nodeIndex);
            }

            tick++;
            copyMatrix(minCost, newMinCost);
        }

        // Print final routing tables
        printRoutingTables(nodes, minCost, nodeIndex);
    }

    private static void mergeState(Neighbor[][] oldMinCost, Map<String, Integer> oldIndex,
                                   Map<String, Integer> newIndex,
                                   Neighbor[][] newMinCost, DistanceList[][] newTable,
                                   DistanceList[][] oldTable) {
        // Copy existing state for nodes that exist in both old and new topologies
        for (String nodeU : oldIndex.keySet()) {
            for (String nodeV : oldIndex.keySet()) {
                int ou = oldIndex.get(nodeU);
                int ov = oldIndex.get(nodeV);
                int nu = newIndex.get(nodeU);
                int nv = newIndex.get(nodeV);
                newMinCost[nu][nv] = oldMinCost[ou][ov];
                newTable[nu][nv] = oldTable[ou][ov];
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Graph graph = new Graph();

        // Read initial node definitions
        String token = scanner.next();
        while (!"START".equals(token)) {
            graph.addNode(token);
            token = scanner.next();
        }

        // Read initial edge definitions
        token = scanner.next();
        while (!"UPDATE".equals(token)) {
            String u = token;
            String v = scanner.next();
            int w = Integer.parseInt(scanner.next());
            graph.addEdge(u, v, w);
            token = scanner.next();
        }

        // Build initial routing tables and run algorithm
        Map<String, Integer> indexMap = buildIndexMap(graph);
        int n = indexMap.size();

        DistanceList[][] distanceTable = new DistanceList[n][n];
        Neighbor[][] minCost = new Neighbor[n][n];
        int[][] neighborCosts = new int[n][n];
        
        // Initialize with -1 (indicating no direct connection)
        for (int i = 0; i < n; i++) {
            Arrays.fill(neighborCosts[i], -1);
        }

        initializeTables(neighborCosts, minCost, indexMap, graph.getAdjList());
        runDistanceVector(neighborCosts, minCost, indexMap, distanceTable);

        // Handle dynamic updates
        boolean updated = false;
        token = scanner.next();
        while (!"END".equals(token)) {
            String u = token;
            String v = scanner.next();
            int w = Integer.parseInt(scanner.next());
            graph.addEdge(u, v, w);
            updated = true;
            token = scanner.next();
        }
        scanner.close();

        // Re-run algorithm if topology was updated
        if (updated) {
            Map<String, Integer> updatedIndex = buildIndexMap(graph);
            int n2 = updatedIndex.size();

            Neighbor[][] minCost2 = new Neighbor[n2][n2];
            DistanceList[][] distanceTable2 = new DistanceList[n2][n2];
            int[][] neighborCosts2 = new int[n2][n2];
            
            // Initialize new tables
            for (int i = 0; i < n2; i++) {
                Arrays.fill(neighborCosts2[i], -1);
            }

            initializeTables(neighborCosts2, minCost2, updatedIndex, graph.getAdjList());
            // Merge previous state to avoid recomputing from scratch
            mergeState(minCost, indexMap, updatedIndex, minCost2, distanceTable2, distanceTable);
            runDistanceVector(neighborCosts2, minCost2, updatedIndex, distanceTable2);
        }
    }
}

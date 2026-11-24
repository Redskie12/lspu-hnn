
package system.com.lostfound;

import java.util.*;

public class CampusGraph {

    private final Map<String, Map<String, Integer>> graph =
            new LinkedHashMap<>();

    public CampusGraph() {

        graph.put("Siniloan", new LinkedHashMap<>());
        graph.put("Santa Cruz", new LinkedHashMap<>());
        graph.put("San Pablo", new LinkedHashMap<>());
        graph.put("Los Baños", new LinkedHashMap<>());

        // All six campus connections and distances

        addConnection("Siniloan", "Santa Cruz", 27);
        addConnection("Siniloan", "San Pablo", 63);
        addConnection("Siniloan", "Los Baños", 75);

        addConnection("Santa Cruz", "San Pablo", 40);
        addConnection("Santa Cruz", "Los Baños", 26);

        addConnection("San Pablo", "Los Baños", 24);
    }

    // ADD TWO-WAY CONNECTION

    private void addConnection(
            String campusA,
            String campusB,
            int distance) {

        graph.get(campusA).put(campusB, distance);
        graph.get(campusB).put(campusA, distance);
    }


    // BREADTH-FIRST SEARCH

    public List<String> bfs(String start) {

        List<String> result = new ArrayList<>();

        Queue<String> queue = new LinkedList<>();

        Set<String> visited = new LinkedHashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {

            String current = queue.poll();

            result.add(current);

            // BFS neighbor order
            for (String neighbor : getBFSNeighbors(current)) {

                if (!visited.contains(neighbor)) {

                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }


    // BFS NEIGHBOR ORDER

    private List<String> getBFSNeighbors(String campus) {

        switch (campus) {

            case "Siniloan":
                return Arrays.asList(
                        "Santa Cruz",
                        "Los Baños",
                        "San Pablo"
                );

            case "Santa Cruz":
                return Arrays.asList(
                        "Siniloan",
                        "San Pablo",
                        "Los Baños"
                );

            case "Los Baños":
                return Arrays.asList(
                        "Siniloan",
                        "Santa Cruz",
                        "San Pablo"
                );

            case "San Pablo":
                return Arrays.asList(
                        "Siniloan",
                        "Santa Cruz",
                        "Los Baños"
                );

            default:
                return Collections.emptyList();
        }
    }


    // DEPTH-FIRST SEARCH

    public List<String> dfs(String start) {

        List<String> result = new ArrayList<>();

        Set<String> visited = new LinkedHashSet<>();

        dfsHelper(start, visited, result);

        return result;
    }


    // DFS RECURSION

    private void dfsHelper(
            String current,
            Set<String> visited,
            List<String> result) {

        visited.add(current);

        result.add(current);

        // DFS uses its own neighbor order
        for (String neighbor : getDFSNeighbors(current)) {

            if (!visited.contains(neighbor)) {

                dfsHelper(neighbor, visited, result);
            }
        }
    }


    // DFS NEIGHBOR ORDER

    private List<String> getDFSNeighbors(String campus) {

        switch (campus) {

            case "Siniloan":
                return Arrays.asList(
                        "Santa Cruz",
                        "San Pablo",
                        "Los Baños"
                );

            case "Santa Cruz":
                return Arrays.asList(
                        "Siniloan",
                        "San Pablo",
                        "Los Baños"
                );

            case "San Pablo":
                return Arrays.asList(
                        "Siniloan",
                        "Santa Cruz",
                        "Los Baños"
                );

            case "Los Baños":
                return Arrays.asList(
                        "Siniloan",
                        "Santa Cruz",
                        "San Pablo"
                );

            default:
                return Collections.emptyList();
        }
    }


    // GET DISTANCE BETWEEN TWO CAMPUSES

    public int getDistance(
            String campusA,
            String campusB) {

        if (campusA.equals(campusB)) {
            return 0;
        }

        Map<String, Integer> neighbors = graph.get(campusA);

        if (neighbors == null ||
                !neighbors.containsKey(campusB)) {

            return -1;
        }

        return neighbors.get(campusB);
    }


    // GET ALL CONNECTED CAMPUSES

    public Map<String, Map<String, Integer>> getGraph() {

        return graph;
    }
}

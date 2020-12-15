package dev.alexnader.auto_codec.processor.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class CircuitFinder {
    public static Graph<Integer> subgraphInducedBy(Graph<Integer> graph, int s) {
        Map<Integer, Set<Integer>> subAdjacency = new HashMap<>();

        for (int v = 1; v <= graph.vertexCount(); v++) {
            if (s <= v) {
                subAdjacency.put(v, new HashSet<>());
                for (int x : graph.getNeighbors(v)) {
                    if (s <= x) {
                        subAdjacency.get(v).add(x);
                    }
                }
            }
        }

        return new Graph<>(subAdjacency);
    }

    public static <T> Set<Set<T>> circuitFindingAlgorithm(Graph<T> graph) {
        Map<T, Integer> mapping = new HashMap<>();
        Map<Integer, T> unMapping = new HashMap<>();
        {
            int i = 1;
            for (T vertex : graph.vertices()) {
                mapping.put(vertex, i);
                unMapping.put(i++, vertex);
            }
        }

        Graph<Integer> intGraph = graph.map(mapping::get);

        Set<Set<Integer>> intCircuits = intCircuitFindingAlgorithm(intGraph);

        Set<Set<T>> circuits = new HashSet<>();
        for (Set<Integer> intCircuit : intCircuits) {
            Set<T> circuit = new HashSet<>();

            for (Integer i : intCircuit) {
                circuit.add(unMapping.get(i));
            }

            circuits.add(circuit);
        }

        return circuits;
    }

    private static Set<Set<Integer>> intCircuitFindingAlgorithm(Graph<Integer> graph) {
        Set<Set<Integer>> circuits = new HashSet<>();

        Graph<Integer> A_K;
        Map<Integer, Set<Integer>> B = new HashMap<>();

        Stack<Integer> stack = new Stack<>();

        Set<Integer> blocked = new HashSet<>();

        int s = 1;

        class Circuit {
            boolean f = false;

            boolean circuit(Graph<Integer> A_K, int s, int v) {
                class Unblock {
                    void unblock(int u) {
                        blocked.remove(u);

                        List<Integer> toRemove = new ArrayList<>();
                        for (int w : B.get(v)) {
                            toRemove.add(w);
                            if (blocked.contains(w)) {
                                unblock(w);
                            }
                        }
                        for (Integer w : toRemove) {
                            B.get(u).remove(w);
                        }
                    }
                }

                stack.push(v);
                blocked.add(v);

                for (int w : A_K.getNeighbors(v)) {
                    if (w == s) {
                        Set<Integer> circuit = new HashSet<>(stack);
                        circuit.add(s);
                        circuits.add(circuit);
                        f = true;
                    } else if (!blocked.contains(w)) {
                        if (circuit(A_K, s, w)) {
                            f = true;
                        }
                    }
                }

                if (f) {
                    new Unblock().unblock(v);
                } else for (int w : A_K.getNeighbors(v)) {
                    if (!B.computeIfAbsent(w,  unused -> new HashSet<>()).contains(v)) {
                        B.get(w).add(v);
                    }
                }

                stack.remove((Integer) v);
                return f;
            }
        }

        Circuit circuit = new Circuit();

        while (s < graph.vertexCount()) {
            Graph<Integer> subgraph = subgraphInducedBy(graph, s);
            Set<Set<Integer>> components = subgraph.strongComponents();
            LeastComponent least = findLeastComponent(components);
            A_K = StrongComponentFinder.adjacencyOfComponent(subgraph, least.component);
            if (A_K.vertexCount() != 0) {
                s = least.vertex;

                for (int i : least.component) {
                    blocked.remove(i);
                    B.put(i, new HashSet<>());
                }

                circuit.circuit(A_K, s, s);
                s += 1;
            } else {
                s = graph.vertexCount();
            }
        }

        return circuits;
    }

    private static LeastComponent findLeastComponent(Set<Set<Integer>> components) {
        int min = Integer.MAX_VALUE;
        Set<Integer> minComponent = null;

        for (Set<Integer> component : components) {
            for (int i : component) {
                if (i < min) {
                    minComponent = component;
                    min = i;
                }
            }
        }

        return new LeastComponent(min, minComponent);
    }

    private static class LeastComponent {
        public final int vertex;
        public final Set<Integer> component;

        public LeastComponent(int vertex, Set<Integer> component) {
            this.vertex = vertex;
            this.component = component;
        }
    }
}

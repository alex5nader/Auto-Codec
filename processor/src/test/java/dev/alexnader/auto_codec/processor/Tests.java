package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.processor.graph.CircuitFinder;
import dev.alexnader.auto_codec.processor.graph.Graph;
import dev.alexnader.auto_codec.processor.graph.StrongComponentFinder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class Tests {
    @Test
    void subgraph() {
        Map<Integer, Set<Integer>> graph = new HashMap<>();
        graph.put(1, new HashSet<>(Arrays.asList(2, 3, 5)));
        graph.put(2, new HashSet<>(Arrays.asList(3, 4)));
        graph.put(3, new HashSet<>(Arrays.asList(5)));
        graph.put(4, new HashSet<>(Arrays.asList(1)));
        graph.put(5, new HashSet<>(Arrays.asList(4)));

        Map<Integer, Set<Integer>> subgraph = new HashMap<>();
        subgraph.put(3, new HashSet<>(Arrays.asList(5)));
        subgraph.put(4, new HashSet<>(Arrays.asList()));
        subgraph.put(5, new HashSet<>(Arrays.asList(4)));

        Assertions.assertEquals(new Graph<>(subgraph), CircuitFinder.subgraphInducedBy(new Graph<>(graph), 3));
    }

    @Test
    void strongComponents() {
        Map<String, Set<String>> adjacency = new HashMap<>();
        adjacency.put("A", new HashSet<>(Arrays.asList("B")));
        adjacency.put("B", new HashSet<>(Arrays.asList("C")));
        adjacency.put("C", new HashSet<>(Arrays.asList("A")));
        adjacency.put("D", new HashSet<>(Arrays.asList("B", "C", "E")));
        adjacency.put("E", new HashSet<>(Arrays.asList("D")));
        adjacency.put("F", new HashSet<>(Arrays.asList("E", "F")));

        Set<Set<String>> expectedComponents = new HashSet<Set<String>>() {{
            add(new HashSet<String>() {{
                add("A");
                add("B");
                add("C");
            }});
            add(new HashSet<String>() {{
                add("D");
                add("E");
            }});
            add(new HashSet<String>() {{
                add("F");
            }});
        }};

        Set<Set<String>> components = StrongComponentFinder.strongComponents(new Graph<>(adjacency));
        Assertions.assertEquals(expectedComponents, components);

        Map<String, Set<String>> abcSubgraph = new HashMap<>();
        abcSubgraph.put("A", new HashSet<>(Arrays.asList("B")));
        abcSubgraph.put("B", new HashSet<>(Arrays.asList("C")));
        abcSubgraph.put("C", new HashSet<>(Arrays.asList("A")));

        Map<String, Set<String>> deSubgraph = new HashMap<>();
        deSubgraph.put("D", new HashSet<>(Arrays.asList("E")));
        deSubgraph.put("E", new HashSet<>(Arrays.asList("D")));

        Map<String, Set<String>> fSubgraph = new HashMap<>();
        fSubgraph.put("F", new HashSet<>(Arrays.asList("F")));

        Map<Integer, Map<String, Set<String>>> expectedSubgraphs = new HashMap<>();
        expectedSubgraphs.put(3, abcSubgraph);
        expectedSubgraphs.put(2, deSubgraph);
        expectedSubgraphs.put(1, fSubgraph);

        for (Set<String> component : components) {
            Assertions.assertEquals(new Graph<>(expectedSubgraphs.get(component.size())), StrongComponentFinder.adjacencyOfComponent(new Graph<>(adjacency), component));
        }
    }

    @Test
    void circuitFinder() {
        Graph<Integer> graph;
        {
            Map<Integer, Set<Integer>> adjacency = new HashMap<>();
            adjacency.put(1, new HashSet<>(Arrays.asList(2)));
            adjacency.put(2, new HashSet<>(Arrays.asList(3)));
            adjacency.put(3, new HashSet<>(Arrays.asList(1)));
            adjacency.put(4, new HashSet<>(Arrays.asList(1)));
            adjacency.put(5, new HashSet<>(Arrays.asList(6)));
            adjacency.put(6, new HashSet<>(Arrays.asList(3, 4, 5)));
            graph = new Graph<>(adjacency);
        }

        Set<Set<Integer>> expectedCircuits = new HashSet<>(Arrays.asList(
            new HashSet<>(Arrays.asList(1, 2, 3)),
            new HashSet<>(Arrays.asList(5, 6))
        ));

        Assertions.assertEquals(expectedCircuits, graph.circuits());
    }

    @Test
    void overlappingCircuits() {
        Graph<String> graph;
        {
            Map<String, Set<String>> adjacency = new HashMap<>();
            adjacency.put("A", new HashSet<>(Arrays.asList("A", "B")));
            adjacency.put("B", new HashSet<>(Arrays.asList("C")));
            adjacency.put("C", new HashSet<>(Arrays.asList("A", "D")));
            adjacency.put("D", new HashSet<>(Arrays.asList("B")));
            graph = new Graph<>(adjacency);
        }

        Set<Set<String>> expectedCircuits = new HashSet<>(Arrays.asList(
            new HashSet<>(Arrays.asList("A")),
            new HashSet<>(Arrays.asList("A", "B", "C")),
            new HashSet<>(Arrays.asList("B", "C", "D"))
        ));

        Assertions.assertEquals(expectedCircuits, graph.circuits());
    }
}

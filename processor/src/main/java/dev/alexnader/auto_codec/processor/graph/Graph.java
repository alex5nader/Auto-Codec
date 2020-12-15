package dev.alexnader.auto_codec.processor.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class Graph<A> {
    public final Map<A, Set<A>> adjacency;

    public Graph(Set<A> vertices, Function<A, Set<A>> getNeighbors) {
        adjacency = new HashMap<>();
        for (A vertex : vertices) {
            adjacency.put(vertex, getNeighbors.apply(vertex));
        }
    }

    public int vertexCount() {
        return adjacency.size();
    }

    public Set<A> vertices() {
        return adjacency.keySet();
    }

    public Set<A> getNeighbors(A vertex) {
        return adjacency.get(vertex);
    }

    public Graph(Map<A, Set<A>> adjacency) {
        this(adjacency.keySet(), adjacency::get);
    }

    public <B> Graph<B> map(Function<A, B> f) {
        Map<A, B> mapping = new HashMap<>();
        for (A vertex : vertices()) {
            mapping.put(vertex, f.apply(vertex));
        }

        Map<B, Set<B>> mapped = new HashMap<>();

        for (A vertex : this.vertices()) {
            Set<B> neighbors = new HashSet<>();
            for (A neighbor : getNeighbors(vertex)) {
                neighbors.add(mapping.get(neighbor));
            }
            mapped.put(mapping.get(vertex), neighbors);
        }

        return new Graph<>(mapped);
    }

    public Set<Set<A>> circuits() {
        return CircuitFinder.circuitFindingAlgorithm(this);
    }

    public Set<Set<A>> strongComponents() {
        return StrongComponentFinder.strongComponents(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Graph<?> other = (Graph<?>) o;

        return adjacency.equals(other.adjacency);
    }

    @Override
    public int hashCode() {
        return adjacency.hashCode();
    }
}

package dev.alexnader.auto_codec.processor.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class StrongComponentFinder {
    public static class Info<T> {
        public T value;
        public int index;
        public int lowlink;
        public boolean onStack;

        public Info(T value) {
            this.value = value;
            index = -1;
            lowlink = -1;
            onStack = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Info<?> info = (Info<?>) o;

            return value.equals(info.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private StrongComponentFinder() {}

    public static <T> Set<Set<T>> strongComponents(Graph<T> graph) {
        class StrongConnect {
            private int index = 0;
            private final Stack<Info<T>> stack = new Stack<>();

            private final Graph<Info<T>> infoGraph = graph.map(Info::new);

            public final Set<Set<T>> components = new HashSet<>();

            private void strongConnect(Info<T> v) {
                v.index = index;
                v.lowlink = index;
                index += 1;
                stack.push(v);
                v.onStack = true;

                for (Info<T> w : infoGraph.getNeighbors(v)) {
                    if (w.index == -1) {
                        strongConnect(w);
                        v.lowlink = Math.min(v.lowlink, w.lowlink);
                    } else if (w.onStack) {
                        v.lowlink = Math.min(v.lowlink, w.index);
                    }
                }

                if (v.lowlink == v.index) {
                    Set<T> component = new HashSet<>();

                    Info<T> w;
                    do {
                        w = stack.pop();
                        w.onStack = false;
                        component.add(w.value);
                    } while (w != v);

                    components.add(component);
                }
            }
        }

        StrongConnect strongConnect = new StrongConnect();

        for (Info<T> vertex : strongConnect.infoGraph.vertices()) {
            if (vertex.index == -1) {
                strongConnect.strongConnect(vertex);
            }
        }

        return strongConnect.components;
    }

    public static <T> Graph<T> adjacencyOfComponent(Graph<T> graph, Set<T> component) {
        Map<T, Set<T>> subAdjacency = new HashMap<>();

        for (T vertex : graph.vertices()) {
            if (component.contains(vertex)) {
                Set<T> subNeighbors = new HashSet<>();

                for (T neighbor : graph.getNeighbors(vertex)) {
                    if (component.contains(neighbor)) {
                        subNeighbors.add(neighbor);
                    }
                }

                subAdjacency.put(vertex, subNeighbors);
            }
        }

        return new Graph<>(subAdjacency);
    }
}

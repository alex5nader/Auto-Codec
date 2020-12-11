package dev.alexnader.auto_codec.processor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class Tests {
    @Test
    void checkCycleDetection() {
        class SimpleVertex implements GraphUtil.Vertex<SimpleVertex> {
            private final String label;
            private final List<SimpleVertex> neighbors;

            @Override
            public List<SimpleVertex> neighbors() {
                return neighbors;
            }

            public SimpleVertex(String label) {
                this.label = label;
                neighbors = new ArrayList<>();
            }

            public void to(SimpleVertex other) {
                neighbors.add(other);
            }

            @Override
            public String toString() {
                return label;
            }
        }

        SimpleVertex a = new SimpleVertex("A");
        SimpleVertex b = new SimpleVertex("B");
        SimpleVertex c = new SimpleVertex("C");

        a.to(b);
        a.to(c);
        b.to(c);

        Assertions.assertFalse(GraphUtil.hasCycle(a));

        SimpleVertex d = new SimpleVertex("D");

        b.to(d);
        d.to(b);

        Assertions.assertTrue(GraphUtil.hasCycle(a));
    }
}

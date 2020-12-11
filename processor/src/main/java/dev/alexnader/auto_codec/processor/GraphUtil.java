package dev.alexnader.auto_codec.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphUtil {
    interface Vertex<V extends Vertex<V>> {
        List<V> neighbors();
    }

    private enum State {
        IN_PROGRESS,
        FINISHED,
    }

    public static <V extends Vertex<V>> boolean hasCycle(V start) {
        return hasCycle(start, new HashMap<>());
    }

    // https://cs.stackexchange.com/a/9681
    private static <V extends Vertex<V>> boolean hasCycle(V current, Map<V, State> states) {
        State state = states.get(current);
        if (state == State.IN_PROGRESS) {
            return true;
        } else if (state == State.FINISHED) {
            return false;
        }

        states.put(current, State.IN_PROGRESS);

        for (V neighbor : current.neighbors()) {
            if (hasCycle(neighbor, states)) {
                return true;
            }
        }

        states.put(current, State.FINISHED);

        return false;
    }
}

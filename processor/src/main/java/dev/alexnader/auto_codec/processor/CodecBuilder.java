package dev.alexnader.auto_codec.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class CodecBuilder implements GraphUtil.Vertex<CodecBuilder> {
    protected final ProcessingEnvironment processingEnvironment;

    protected CodecBuilder(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    public List<CodecBuilder> neighbors() {
        return new ArrayList<>(directDependencies());
    }

    protected abstract Set<CodecBuilder> directDependencies();

    protected abstract boolean setup();

    protected abstract String source();

    public final String build() {
        if (!setup()) {
            return null;
        }

        if (GraphUtil.hasCycle(this)) {
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Dependency cycle detected in " + this + ".");
            return null;
        }

        return source();
    }
}

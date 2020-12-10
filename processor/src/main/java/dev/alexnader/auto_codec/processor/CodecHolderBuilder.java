package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.CodecHolder;

import javax.lang.model.element.PackageElement;
import java.util.ArrayList;
import java.util.List;

public class CodecHolderBuilder {
    private final PackageElement packageElement;
    private final List<CodecBuilder> codecs = new ArrayList<>();

    public CodecHolderBuilder(PackageElement packageElement) {
        this.packageElement = packageElement;
    }

    public void addCodec(CodecBuilder codec) {
        codecs.add(codec);
    }

    public String build() {
        StringBuilder contents = new StringBuilder()
            .append("package ").append(packageElement.getQualifiedName()).append(";\n")
            .append("\n")
            .append("public class ").append(packageElement.getAnnotation(CodecHolder.class).value()).append(" {\n");

        for (CodecBuilder codec : codecs) {
            String result = codec.build();
            if (result == null) {
                return null;
            }
            contents.append(result);
        }

        contents.append("}\n");

        return contents.toString();
    }
}

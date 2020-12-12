package dev.alexnader.auto_codec.processor;

import javax.lang.model.element.PackageElement;
import java.util.ArrayList;
import java.util.List;

class CodecHolder {
    public final PackageElement packageElement;
    public final String name;
    public final List<Codec> codecs = new ArrayList<>();

    public CodecHolder(PackageElement packageElement) {
        this.packageElement = packageElement;
        name = packageElement.getAnnotation(dev.alexnader.auto_codec.CodecHolder.class).value();
    }

    public String qualifiedClass() {
        return packageElement.getQualifiedName().toString() + "." + name;
    }

    public String build() {
        StringBuilder contents = new StringBuilder()
            .append("package ").append(packageElement.getQualifiedName()).append(";\n")
            .append("\n")
            .append("public class ").append(name).append(" {\n");

        for (Codec codec : codecs) {
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

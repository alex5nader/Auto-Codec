package dev.alexnader.auto_codec.processor.holder;

import dev.alexnader.auto_codec.CodecHolder;
import dev.alexnader.auto_codec.processor.codec.GeneratedCodec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.PackageElement;
import java.util.ArrayList;
import java.util.List;

public class GeneratedHolder extends HolderRef {
    public final List<GeneratedCodec> codecs = new ArrayList<>();

    public GeneratedHolder(PackageElement packageElement) {
        super(packageElement, packageElement.getAnnotation(CodecHolder.class).value());
    }

    public String fileName() {
        return packageElement.getQualifiedName() + "." + name;
    }

    public String toSourceCode(ProcessingEnvironment processingEnv) {
        StringBuilder contents = new StringBuilder()
            .append("package ").append(packageElement.getQualifiedName()).append(";\n")
            .append("\n")
            .append("public class ").append(name).append(" {\n");

        for (GeneratedCodec codec : codecs) {
            contents.append(codec.toSourceCode(processingEnv));
        }

        contents.append("}\n");

        return contents.toString();
    }
}

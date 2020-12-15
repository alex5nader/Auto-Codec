package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.CodecHolder;
import dev.alexnader.auto_codec.codecs.Record;
import dev.alexnader.auto_codec.options.Exclude;
import dev.alexnader.auto_codec.options.Getter;
import dev.alexnader.auto_codec.options.Rename;
import dev.alexnader.auto_codec.options.Use;
import dev.alexnader.auto_codec.processor.codec.CodecRef;
import dev.alexnader.auto_codec.processor.codec.ExternalCodec;
import dev.alexnader.auto_codec.processor.codec.GeneratedCodec;
import dev.alexnader.auto_codec.processor.codec.Status;
import dev.alexnader.auto_codec.processor.codec.constructor.CodecConstructor;
import dev.alexnader.auto_codec.processor.codec.constructor.RecordConstructor;
import dev.alexnader.auto_codec.processor.graph.Graph;
import dev.alexnader.auto_codec.processor.holder.GeneratedHolder;
import dev.alexnader.auto_codec.processor.holder.HolderRef;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeMirror, CodecRef> defaultCodecs = new HashMap<>();
        {
            Types types = processingEnv.getTypeUtils();
            Elements elements = processingEnv.getElementUtils();
            HolderRef builtinHolder = new HolderRef(elements.getPackageElement("com.mojang.serialization"), "Codec");
            defaultCodecs.put(elements.getTypeElement("java.lang.Boolean").asType(), new ExternalCodec(builtinHolder, elements.getTypeElement("java.lang.Boolean").asType(), "BOOL"));
            defaultCodecs.put(elements.getTypeElement("java.lang.Byte").asType(), new ExternalCodec(builtinHolder, elements.getTypeElement("java.lang.Byte").asType(), "BYTE"));
            defaultCodecs.put(elements.getTypeElement("java.lang.Short").asType(), new ExternalCodec(builtinHolder, elements.getTypeElement("java.lang.Short").asType(), "SHORT"));
            defaultCodecs.put(elements.getTypeElement("java.lang.Integer").asType(), new ExternalCodec(builtinHolder, elements.getTypeElement("java.lang.Integer").asType(), "INT"));
            defaultCodecs.put(elements.getTypeElement("java.lang.Long").asType(), new ExternalCodec(builtinHolder, elements.getTypeElement("java.lang.Long").asType(), "LONG"));
            defaultCodecs.put(elements.getTypeElement("java.lang.Float").asType(), new ExternalCodec(builtinHolder, elements.getTypeElement("java.lang.Float").asType(), "FLOAT"));
            defaultCodecs.put(elements.getTypeElement("java.lang.Double").asType(), new ExternalCodec(builtinHolder, elements.getTypeElement("java.lang.Double").asType(), "DOUBLE"));
            defaultCodecs.put(elements.getTypeElement("java.lang.String").asType(), new ExternalCodec(builtinHolder, elements.getTypeElement("java.lang.String").asType(), "STRING"));
            defaultCodecs.put(types.getPrimitiveType(TypeKind.BOOLEAN), new ExternalCodec(builtinHolder, types.getPrimitiveType(TypeKind.BOOLEAN), "BOOL"));
            defaultCodecs.put(types.getPrimitiveType(TypeKind.BYTE), new ExternalCodec(builtinHolder, types.getPrimitiveType(TypeKind.BYTE), "BYTE"));
            defaultCodecs.put(types.getPrimitiveType(TypeKind.SHORT), new ExternalCodec(builtinHolder, types.getPrimitiveType(TypeKind.SHORT), "SHORT"));
            defaultCodecs.put(types.getPrimitiveType(TypeKind.INT), new ExternalCodec(builtinHolder, types.getPrimitiveType(TypeKind.INT), "INT"));
            defaultCodecs.put(types.getPrimitiveType(TypeKind.LONG), new ExternalCodec(builtinHolder, types.getPrimitiveType(TypeKind.LONG), "LONG"));
            defaultCodecs.put(types.getPrimitiveType(TypeKind.FLOAT), new ExternalCodec(builtinHolder, types.getPrimitiveType(TypeKind.FLOAT), "FLOAT"));
            defaultCodecs.put(types.getPrimitiveType(TypeKind.DOUBLE), new ExternalCodec(builtinHolder, types.getPrimitiveType(TypeKind.DOUBLE), "DOUBLE"));
        }

        Map<TypeMirror, Status> status = new HashMap<>();
        for (TypeMirror builtinType : defaultCodecs.keySet()) {
            status.put(builtinType, Status.SUCCESSFUL);
        }

        Map<PackageElement, GeneratedHolder> holders = new HashMap<>();

        Set<TypeElement> toProcess = new HashSet<>();
        toProcess.addAll(ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Record.class)));
        //TODO add more `addAll` for other codec types

        Map<TypeMirror, CodecConstructor<?>> constructors = new HashMap<>();
        {
            Set<CodecConstructor<?>> possibleConstructors = new HashSet<>(Arrays.asList(
                new RecordConstructor(processingEnv)
            ));

            for (TypeElement current : toProcess) {
                for (CodecConstructor<?> constructor : possibleConstructors) {
                    if (constructor.supports(current.asType())) {
                        constructors.put(current.asType(), constructor);
                    }
                }
            }
        }

        for (TypeElement current : toProcess) {
            create(constructors, status, defaultCodecs, holders, current.asType());
        }

        if (status.containsValue(Status.CYCLIC_DEPENDENCY)) {
            Set<TypeMirror> types = new HashSet<>();
            for (TypeElement typeElement : toProcess) {
                types.add(typeElement.asType());
                types.addAll(constructors.get(typeElement.asType()).dependencies(typeElement.asType()));
            }
            Graph<TypeMirror> dependencyGraph = new Graph<>(types, type -> {
                if (constructors.containsKey(type)) {
                    return constructors.get(type).dependencies(type);
                } else {
                    return Collections.emptySet();
                }
            });
            for (Set<TypeMirror> cycle : dependencyGraph.circuits()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Dependency cycle: " + cycle);
            }
        }

        for (GeneratedHolder holder : holders.values()) {
            try {
                JavaFileObject file = processingEnv.getFiler().createSourceFile(holder.fileName());

                try (Writer writer = file.openWriter()) {
                    writer.write(holder.toSourceCode(processingEnv));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private void create(Map<TypeMirror, CodecConstructor<?>> constructors, Map<TypeMirror, Status> status, Map<TypeMirror, CodecRef> defaultCodecs, Map<PackageElement, GeneratedHolder> holders, TypeMirror current) {
        switch (status.computeIfAbsent(current, unused -> Status.NOT_STARTED)) {
        case INVALID:
        case CYCLIC_DEPENDENCY:
        case UNSUPPORTED:
        case SUCCESSFUL:
            return;
        case CREATING_DEPENDENCIES:
            status.put(current, Status.CYCLIC_DEPENDENCY);
            return;
        case NOT_STARTED:
            break;
        }

        CodecConstructor<?> constructor = constructors.get(current);
        if (constructor == null) {
            // todo move this special case somewhere nicer?
            if (processingEnv.getTypeUtils().asElement(current).getAnnotation(Use.class) == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, current + " is an unknown type. Try adding @Use if it's an external type.");
                status.put(current, Status.UNSUPPORTED);
            }
            return;
        }

        status.put(current, Status.CREATING_DEPENDENCIES);

        for (TypeMirror dependency : constructor.dependencies(current)) {
            create(constructors, status, defaultCodecs, holders, dependency);
            Status dependencyStatus = status.get(dependency);
            if (dependencyStatus != Status.SUCCESSFUL) {
                status.put(current, dependencyStatus);
                return;
            }
        }

        PackageElement holderPackageElement = findHolderPackageElement(current);
        GeneratedHolder holder = holders.computeIfAbsent(holderPackageElement, GeneratedHolder::new);

        @Nullable GeneratedCodec codec = constructor.construct(holder, current, defaultCodecs);
        if (codec != null) {
            defaultCodecs.put(current, codec);

            holder.codecs.add(codec);

            status.put(current, Status.SUCCESSFUL);
        } else {
            status.put(current, Status.INVALID);
        }
    }

    public @Nullable PackageElement findHolderPackageElement(TypeMirror type) {
        Element element = processingEnv.getTypeUtils().asElement(type);
        if (element == null) {
            return null;
        }

        String[] parts = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString().split("\\.");

        for (int length = parts.length; length > 0; length--) {
            StringBuilder packageName = new StringBuilder();
            for (int i = 0; i < length; i++) {
                packageName.append(parts[i]);
                if (i != length - 1) {
                    packageName.append(".");
                }
            }

            PackageElement packageElement = processingEnv.getElementUtils().getPackageElement(packageName);
            if (packageElement.getAnnotation(CodecHolder.class) != null) {
                return packageElement;
            }
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "No parent package of " + processingEnv.getElementUtils().getPackageOf(element).getQualifiedName() + " is annotated with @CodecHolder.");
        return null;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Stream.of(Record.class, Exclude.class, Getter.class, Rename.class, Use.class, CodecHolder.class).map(Class::getName).collect(Collectors.toSet());
    }
}

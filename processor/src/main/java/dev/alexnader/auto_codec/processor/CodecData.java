//package dev.alexnader.auto_codec.processor;
//
//import dev.alexnader.auto_codec.CodecHolder;
//import dev.alexnader.auto_codec.codecs.Record;
//
//import javax.lang.model.element.PackageElement;
//import javax.lang.model.element.TypeElement;
//import java.util.List;
//
//class CodecData implements GraphUtil.Vertex<CodecData> {
//    public final PackageElement holderPackage;
//    public final TypeElement typeElement;
//    private final List<CodecData> dependencies;
//
//    public CodecData(PackageElement holderPackage, TypeElement typeElement, List<CodecData> dependencies) {
//        this.holderPackage = holderPackage;
//        this.typeElement = typeElement;
//        this.dependencies = dependencies;
//    }
//
//    public String codecName() {
//        return Processor.getCodecName(typeElement, typeElement.getAnnotation(Record.class).value());
//    }
//
//    public String codecField() {
//        return holderPackage.getQualifiedName().toString() + "." + holderPackage.getAnnotation(CodecHolder.class).value() + "." + codecName();
//    }
//
//    public boolean holderCanSeePackagePrivateFields() {
//        return ((PackageElement) typeElement.getEnclosingElement()).equals(holderPackage);
//    }
//
//    @Override
//    public List<CodecData> neighbors() {
//        return dependencies;
//    }
//}

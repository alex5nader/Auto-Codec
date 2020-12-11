package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.CodecHolder;
import dev.alexnader.auto_codec.codecs.Record;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

public class RecordData {

    public final PackageElement holderPackage;
    public final TypeElement typeElement;

    public RecordData(PackageElement holderPackage, TypeElement typeElement) {
        this.holderPackage = holderPackage;
        this.typeElement = typeElement;
    }

    public String codecName() {
        return Processor.getCodecName(typeElement, typeElement.getAnnotation(Record.class).value());
    }

    public String codecField() {
        return holderPackage.getQualifiedName().toString() + "." + holderPackage.getAnnotation(CodecHolder.class).value() + "." + codecName();
    }

    public boolean holderCanSeePackagePrivateFields() {
        return ((PackageElement) typeElement.getEnclosingElement()).equals(holderPackage);
    }
}

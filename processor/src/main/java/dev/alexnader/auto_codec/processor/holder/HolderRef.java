package dev.alexnader.auto_codec.processor.holder;

import javax.lang.model.element.PackageElement;

public class HolderRef {
    public final PackageElement packageElement;
    public final String name;

    public HolderRef(PackageElement packageElement, String holderName) {
        this.packageElement = packageElement;
        name = holderName;
    }

    public String qualifiedClassName() {
        return packageElement.getQualifiedName() + "." + name;
    }
}

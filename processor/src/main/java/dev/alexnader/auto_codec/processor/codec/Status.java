package dev.alexnader.auto_codec.processor.codec;

public enum Status {
    NOT_STARTED,
    CREATING_DEPENDENCIES,
    SUCCESSFUL,

    UNSUPPORTED,
    INVALID,
    CYCLIC_DEPENDENCY,
}

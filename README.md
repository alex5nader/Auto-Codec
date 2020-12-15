# Auto Codec

Auto Codec is an annotation processor that generates DFU codecs.

## Installation

I don't have a maven repository, so if you want to use Auto Codec, clone the repo and
run the task `publishProcessorPublicationToMavenLocal`. Then, add this to your build.gradle:

```gradle
repositories {
    mavenLocal()
}

dependencies {
    compileOnly("dev.alexnader:auto_codec:<VERSION GOES HERE>")
    annotationProcessor("dev.alexnader:auto_codec:<VERSION GOES HERE>")
}
```

Important note: do **NOT** use `modCompileOnly`. Auto Codec is not published as a fabric mod, and does not need
to be remapped.

The version can be found in [`gradle.properties`](gradle.properties), under `mod_version`.

## Usage

An example mod using Auto Codec can be found in the [`testmod`](testmod) gradle project.

### Codec Holders

The first step when using Auto Codec is to create a Codec Holder. This is a class generated
by Auto Codec that will have fields for the generated codecs.

Create a `package-info.java` in the package you want to generate the holder in, and add the
`@CodecHolder` annotation to it. The annotation's value determines the name of the codec
holder.

The following `package-info.java` will generate the class
`dev.alexnader.auto_codec_test.Codecs`.

```java
@CodecHolder("Codecs")
package dev.alexnader.auto_codec_test;

import dev.alexnader.auto_codec.CodecHolder;
```

### Record Codecs

Auto Codec can generate codecs from any subpackage of the holder package. For example,
the class `dev.alexnader.auto_codec_test.record.A` will be placed into
`dev.alexnader.auto_codec_test.Codecs`.

To generate a record codec for a class, annotate the class with `@Record`.

A constructor is required for Auto Codec to work. It must have a parameters corresponding
to all fields of the class (except for excluded fields).

#### Options

By default, a record codec's name will be the same as the class's name, converted to
SCREAMING_SNAKE_CASE. Provide a value for `@Record` to change the name.

By default, Auto Codec will try to include every field in the codec. Fields can be
excluded with `@Exclude`.

If a field is not accessible from the codec holder, a getter
must be specified with `@Getter`. Provide the name of an instance method in the
`@Record`-annotated class as the value.

If a field is not one of the default codec types, or of another type that Auto Codec
generates a codec for (in the same project), it must be annotated with `@Use`. The
value should be a static field containing the codec to use for that field, and it must
be accessible from the codec holder package.

Default codec types:
- `java.lang.Boolean`
- `boolean`
- `java.lang.Byte`
- `byte`
- `java.lang.Short`
- `short`
- `java.lang.Integer`
- `int`
- `java.lang.Long`
- `long`
- `java.lang.Float`
- `float`
- `java.lang.Double`
- `double`
- `java.lang.String`

package dev.alexnader.auto_codec_test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.profiler.Profiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SampleClassDataListener implements SimpleResourceReloadListener<Collection<Identifier>> {
    @Override
    public CompletableFuture<Collection<Identifier>> load(ResourceManager resourceManager, Profiler profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            return resourceManager.findResources("auto_codec_test/sample_class", s -> s.endsWith(".json"));
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Collection<Identifier> identifiers, ResourceManager resourceManager, Profiler profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            for (Identifier sampleClassId : identifiers) {
                try {
                    JsonElement element = new Gson().fromJson(new BufferedReader(new InputStreamReader(resourceManager.getResource(sampleClassId).getInputStream())), JsonElement.class);

                    DataResult<Pair<SampleClass, JsonElement>> result = Codecs.SAMPLE_CLASS.decode(JsonOps.INSTANCE, element);

                    result.get().map(
                        pair -> {
                            System.out.println("Deserialized `" + sampleClassId + "`: " + pair.getFirst());
                            return Unit.INSTANCE;
                        },
                        partial -> {
                            System.err.println("Error in `" + sampleClassId + "`: " + partial.message());
                            return Unit.INSTANCE;
                        }
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, executor);
    }

    private final Identifier id = new Identifier("auto_codec_test", "data/test");

    @Override
    public Identifier getFabricId() {
        return id;
    }
}
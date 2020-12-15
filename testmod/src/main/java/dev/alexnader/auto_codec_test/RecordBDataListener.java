package dev.alexnader.auto_codec_test;

import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class RecordBDataListener implements SimpleResourceReloadListener<Collection<Identifier>> {
    private final Identifier id = new Identifier("auto_codec_test", "data/test");

    @Override
    public Identifier getFabricId() {
        return id;
    }

    @Override
    public CompletableFuture<Collection<Identifier>> load(ResourceManager resourceManager, Profiler profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            //noinspection CodeBlock2Expr
            return resourceManager.findResources("auto_codec_test/class_b", s -> s.endsWith(".json"));
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Collection<Identifier> identifiers, ResourceManager resourceManager, Profiler profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
//            for (Identifier sampleClassId : identifiers) {
//                try {
//                    JsonElement element = new Gson().fromJson(new BufferedReader(new InputStreamReader(resourceManager.getResource(sampleClassId).getInputStream())), JsonElement.class);
//
//                    DataResult<Pair<RecordB, JsonElement>> result = Codecs.SUPER_COOL_CLASS_B_CODEC_WITH_CUSTOM_NAME.decode(JsonOps.INSTANCE, element);
//
//                    result.get().map(
//                        pair -> {
//                            System.out.println("Deserialized `" + sampleClassId + "`: " + pair.getFirst());
//                            return Unit.INSTANCE;
//                        },
//                        partial -> {
//                            System.err.println("Error in `" + sampleClassId + "`: " + partial.message());
//                            return Unit.INSTANCE;
//                        }
//                    );
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }, executor);
    }
}

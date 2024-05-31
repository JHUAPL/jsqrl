package edu.jhuapl.ses.jsqrl.impl.gson;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Preconditions;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.MetadataManager;
import edu.jhuapl.ses.jsqrl.api.Serializer;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.FixedMetadata;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;
import edu.jhuapl.ses.jsqrl.impl.gson.GsonSerializer;

public class Serializers {
  private static final Serializer INSTANCE = GsonSerializer.of();

  public static Serializer getDefault() {
    return INSTANCE;
  }

  public static Serializer of() {
    return GsonSerializer.of();
  }

  public static void serialize(String metadataId, MetadataManager manager, File file)
      throws IOException {
    Preconditions.checkNotNull(metadataId);
    Preconditions.checkNotNull(manager);
    Preconditions.checkNotNull(file);

    Serializer serializer = of();
    serializer.register(Key.of(metadataId), manager);
    serializer.save(file);
  }

  public static void serialize(String metadataId, Metadata metadata, File file) throws IOException {
    Preconditions.checkNotNull(metadataId);
    Preconditions.checkNotNull(metadata);
    Preconditions.checkNotNull(file);

    serialize(metadataId, new MetadataManager() {

      @Override
      public Metadata store() {
        return metadata;
      }

      @Override
      public void retrieve(@SuppressWarnings("unused") Metadata source) {
        throw new UnsupportedOperationException();
      }

    }, file);
  }

  public static void deserialize(File file, String metadataId, MetadataManager manager)
      throws IOException {
    Preconditions.checkNotNull(file);
    Preconditions.checkNotNull(metadataId);
    Preconditions.checkNotNull(manager);

    Serializer serializer = of();
    Key<Metadata> key = Key.of(metadataId);
    serializer.register(key, manager);
    serializer.load(file);
    serializer.deregister(key);
  }

  public static FixedMetadata deserialize(File file, String metadataId) throws IOException {
    Preconditions.checkNotNull(file);
    Preconditions.checkNotNull(metadataId);

    class DirectMetadataManager implements MetadataManager {
      final SettableMetadata metadata = SettableMetadata.of(Version.of(0, 1));

      @Override
      public Metadata store() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void retrieve(Metadata source) {
        for (Key<?> key : source.getKeys()) {
          put(key, source.get(key), metadata);
        }
      }

    }

    DirectMetadataManager manager = new DirectMetadataManager();
    deserialize(file, metadataId, manager);
    return FixedMetadata.of(manager.metadata);
  }

  @SuppressWarnings("unchecked")
  private static <T> void put(Key<?> key, T object, SettableMetadata metadata) {
    metadata.put((Key<T>) key, object);
  }
}

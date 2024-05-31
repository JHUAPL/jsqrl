package edu.jhuapl.ses.jsqrl.api;

import java.io.File;
import java.io.IOException;

public interface Serializer {
  /**
   * Return the version of the serializer/stored file format itself.
   * 
   * @return the version.
   */
  Version getVersion();

  /**
   * Register the provided manager to manage Metadata objects associated with the provided key.
   * 
   * Managers are called in the order in which they were originally added. Call this method the very
   * first time a method is invoked that uses a Metadata object to save and/or restore the state of
   * an object. This may or may not be within a constructor.
   * 
   * This is so that program execution will more-or-less preserve the natural order of operations
   * affecting objects whose metadata is serialized/deserialized.
   * 
   * @param key the key identifying the Metadata objects this manager manages
   * @param manager the manager for Metadata objects associated with the key
   * @throws IllegalStateException if method is called more than once with the same key
   */
  void register(Key<? extends Metadata> key, MetadataManager manager);

  void deregister(Key<? extends Metadata> key);

  void load(File file) throws IOException;

  void save(File file) throws IOException;

}

package edu.jhuapl.ses.jsqrl.impl;

import java.util.Collection;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.Version;

public final class EmptyMetadata implements Metadata {
  private static final EmptyMetadata INSTANCE = new EmptyMetadata();

  public static EmptyMetadata instance() {
    return INSTANCE;
  }

  @Override
  public Version getVersion() {
    throw new UnsupportedOperationException("Empty metadata has no version");
  }

  @Override
  public Collection<Key<?>> getKeys() {
    throw new UnsupportedOperationException("Empty metadata has no keys");
  }

  @Override
  public boolean hasKey(Key<?> key) {
    throw new UnsupportedOperationException("Empty metadata does not have key " + key);
  }

  @Override
  public <V> V get(Key<V> key) {
    throw new UnsupportedOperationException("Empty metadata has no value for key " + key);
  }

  @Override
  public Metadata copy() {
    return this;
  }

  @Override
  public String toString() {
    return "Empty metadata";
  }

}

package edu.jhuapl.ses.jsqrl.impl;

import java.lang.reflect.Array;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.Version;

/**
 * Base implementation that assumes/requires all metadata keys to be stored in a standard
 * {@link java.util.Map}. Implementations are provided for all {@link Metadata} methods except copy.
 * An additional protected abtract method, getMap() is provided so that subclasses may provide the
 * map used by this impementation. To ensure invariants are preserved in sublasses, all methods that
 * rely on this implementation's contract are final.
 */
public abstract class AbstractMetadata implements Metadata {
  /**
   * Object used to represent null. By proxying null with this object, it is possible to use any
   * {@link java.util.Map} implementation for key-value pairs.
   */
  private static final Object NULL_OBJECT = new Object() {
    @Override
    public String toString() {
      // Deliberately capitalizing this so that the astute debugger has a chance of
      // noticing that this object is not actually a null pointer.
      return "NULL";
    }
  };

  private final Version version;

  protected AbstractMetadata(Version version) {
    Preconditions.checkNotNull(version);
    this.version = version;
  }

  /**
   * Provide an immutable copy of the map of keys to values. Because the returned map does not
   * support nulls for keys or values, implementations must use the object returned by getNullObject
   * to represent all null values.
   * 
   * @return the map of keys to values
   */
  public abstract ImmutableMap<Key<?>, Object> getMap();

  @Override
  public final Version getVersion() {
    return version;
  }

  @Override
  public final boolean hasKey(Key<?> key) {
    Preconditions.checkNotNull(key);
    return getMap().containsKey(key);
  }

  @Override
  public final <V> V get(Key<V> key) {
    Preconditions.checkNotNull(key);
    Object object = getMap().get(key);
    if (object == null) {
      throw new IllegalArgumentException("FixedMetadata does not contain key " + key);
    }
    if (getNullObject() == object) {
      return null;
    }
    @SuppressWarnings("unchecked")
    V result = (V) object;
    return result;
  }

  @Override
  public abstract AbstractMetadata copy();

  @Override
  public final int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + getVersion().hashCode();
    result = prime * result + getMap().hashCode();
    return result;
  }

  @Override
  public final boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof AbstractMetadata) {
      AbstractMetadata that = (AbstractMetadata) other;
      if (!this.getVersion().equals(that.getVersion())) {
        return false;
      }

      ImmutableMap<Key<?>, Object> thisMap = this.getMap();
      ImmutableMap<Key<?>, Object> thatMap = that.getMap();
      for (Entry<Key<?>, Object> entry : thisMap.entrySet()) {
        Key<?> key = entry.getKey();
        if (!thatMap.containsKey(key)) {
          return false;
        }

        Object thisValue = entry.getValue();
        Object thatValue = thatMap.get(key);
        try {
          int thisLength = Array.getLength(thisValue);
          int thatLength = Array.getLength(thatValue);
          if (thisLength != thatLength) {
            return false;
          }
          for (int index = 0; index < thisLength; ++index) {
            if (!Array.get(thisValue, index).equals(Array.get(thatValue, index))) {
              return false;
            }
          }
        } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
          // One or both items are not arrays, so just fall back on native equals method.
          if (!thisValue.equals(thatValue)) {
            return false;
          }
        }
      }

      return true;
    }

    return false;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("(Metadata) version ");
    builder.append(getVersion());
    for (Key<?> key : getKeys()) {
      builder.append("\n");
      builder.append(key + " = " + get(key));
    }
    return builder.toString();
  }

  protected static Object getNullObject() {
    return NULL_OBJECT;
  }

}

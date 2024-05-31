package edu.jhuapl.ses.jsqrl.impl;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.ProvidesGenericObjectFromMetadata;
import edu.jhuapl.ses.jsqrl.api.ProvidesMetadataFromGenericObject;
import edu.jhuapl.ses.jsqrl.impl.Utilities;

/**
 * A collection of {@link ProvidesGenericObjectFromMetadata}s that may be stored or retrieved using
 * an associated {@link Key}. Keys must be unique within one InstanceGetter.
 * 
 * @param <T> the object type that can be gotten from the Metadata
 */
public final class InstanceGetter {

  /**
   * Return a reference to the standard/global InstanceGetter. Most applications will use this so
   * that any code may gain access to the various types of available
   * {@link ProvidesGenericObjectFromMetadata} objects.
   * 
   * @return the instance
   */
  public static InstanceGetter defaultInstanceGetter() {
    return DEFAULT_INSTANCE_GETTER;
  }

  private static final InstanceGetter DEFAULT_INSTANCE_GETTER = new InstanceGetter();

  private final SortedMap<Key<?>, ProvidesGenericObjectFromMetadata<?>> fromMetadataMap;
  private final Map<Class<?>, ProvidesMetadataFromGenericObject<?>> toMetadataMap;
  private final BiMap<Class<?>, Key<?>> keyMap;
  private final List<Class<?>> abstractTypes;
  private final List<Class<?>> interfaceTypes;

  /**
   * Create a new InstanceGetter. In general, it's best to use the standard/global InstanceGetter
   * supplied by the defaultInstanceGetter method. However, this constructor is public in case it
   * ever becomes necessary to stand up an independent one (say to override how objects of a
   * particular class are proxied by default).
   */
  public InstanceGetter() {
    this.fromMetadataMap = new TreeMap<>();
    this.toMetadataMap = new HashMap<>();
    this.keyMap = HashBiMap.create();
    this.abstractTypes = new ArrayList<>();
    this.interfaceTypes = new ArrayList<>();
  }

  public boolean isProvidableFromMetadata(Key<?> proxyTypeKey) {
    Preconditions.checkNotNull(proxyTypeKey);

    return fromMetadataMap.containsKey(proxyTypeKey);
  }

  /**
   * Get the {@link ProvidesGenericObjectFromMetadata} object that matches the supplied key,
   * provided the InstanceGetter has access to one.
   * 
   * @param proxyTypeKey the key uniquely identifying the type of the object to be handled by the
   *        returned {@link ProvidesGenericObjectFromMetadata}
   * @return the helper object that may be used to create/get objects from {@link Metadata}
   * @throws IllegalArgumentException if this InstanceGetter does not have a
   *         {@link ProvidesGenericObjectFromMetadata} object for the supplied key
   * @throws ClassCastException if the supplied key matches a
   *         {@link ProvidesGenericObjectFromMetadata} object managed by this InstanceGetter, but
   *         the key has the wrong type.
   */
  public <T> ProvidesGenericObjectFromMetadata<T> providesGenericObjectFromMetadata(
      Key<T> proxyTypeKey) {
    Preconditions.checkNotNull(proxyTypeKey);
    Preconditions.checkArgument(fromMetadataMap.containsKey(proxyTypeKey),
        "Unable to provide proxied object from metadata for type " + proxyTypeKey);

    @SuppressWarnings("unchecked")
    ProvidesGenericObjectFromMetadata<T> result =
        (ProvidesGenericObjectFromMetadata<T>) fromMetadataMap.get(proxyTypeKey);

    return result;
  }

  public boolean isStorableAsMetadata(Object object) {
    if (object == null) {
      return false;
    }

    return isTypeStorableAsMetadata(object.getClass());
  }

  public boolean isTypeStorableAsMetadata(Class<?> type) {
    return findBestMatchForType(type) != null;
  }

  public <T> Key<T> getKeyForType(Class<?> objectType) {
    Preconditions.checkNotNull(objectType);

    Class<?> matchingType = findBestMatchForType(objectType);

    if (matchingType == null) {
      throw new IllegalArgumentException();
    }

    @SuppressWarnings("unchecked")
    Key<T> result = (Key<T>) keyMap.get(matchingType);

    return result;
  }

  public <T> Class<T> getTypeForKey(Key<?> typeKey) {
    Preconditions.checkNotNull(typeKey);
    BiMap<Key<?>, Class<?>> typeMap = keyMap.inverse();
    Preconditions.checkArgument(typeMap.containsKey(typeKey));

    @SuppressWarnings("unchecked")
    Class<T> result = (Class<T>) typeMap.get(typeKey);

    return result;
  }

  public <T> ProvidesMetadataFromGenericObject<T> providesMetadataFromGenericObject(
      Class<?> objectType) {
    Preconditions.checkNotNull(objectType);

    Class<?> matchingType = findBestMatchForType(objectType);

    Preconditions.checkArgument(matchingType != null);

    @SuppressWarnings("unchecked")
    ProvidesMetadataFromGenericObject<T> result =
        (ProvidesMetadataFromGenericObject<T>) toMetadataMap.get(matchingType);

    return result;
  }

  /**
   * Call the 4-argument register method instead of this one.
   * 
   * @param proxyTypeKey
   * @param fromMetadata
   */
  @Deprecated
  public <T> void register(Key<T> proxyTypeKey,
      ProvidesGenericObjectFromMetadata<? extends T> fromMetadata) {
    Preconditions.checkNotNull(proxyTypeKey);
    Preconditions.checkNotNull(fromMetadata);

    Preconditions.checkState(!fromMetadataMap.containsKey(proxyTypeKey),
        "Cannot register metadata proxy more than once for type " + proxyTypeKey);

    fromMetadataMap.put(proxyTypeKey, fromMetadata);
  }

  /**
   * Register proxy metadata store and retrieve objects to work with the supplied key and type.
   * 
   * @param proxyTypeKey the key identifying the type of object that may be obtained by the
   *        {@link ProvidesGenericObjectFromMetadata}. This is encoded in the stored metadata.
   * @param fromMetadata the {@link ProvidesGenericObjectFromMetadata} object to associate with this
   *        key and value type. This object is used to supply an instance when deserializing.
   * @param objectType the specific type of object that may be stored and retrieved to/from
   *        metadata.
   * @param toMetadata the {@link ProvidesMetadataFromGenericObject} object to associate with this
   *        key and value type. This object is used to encode an instance of the object as metadata.
   * @throws IllegalStateException if this InstanceGetter already was called with the supplied
   *         proxyTypeKey or objectType.
   */
  public <T> void register(Key<T> proxyTypeKey,
      ProvidesGenericObjectFromMetadata<? extends T> fromMetadata, Class<?> objectType,
      ProvidesMetadataFromGenericObject<? extends T> toMetadata) {
    Preconditions.checkNotNull(proxyTypeKey);
    Preconditions.checkNotNull(fromMetadata);
    Preconditions.checkNotNull(objectType);
    Preconditions.checkNotNull(toMetadata);

    Preconditions.checkState(!fromMetadataMap.containsKey(proxyTypeKey),
        "Cannot register metadata proxy more than once for type " + proxyTypeKey);
    Preconditions.checkState(!toMetadataMap.containsKey(objectType),
        "Cannot register metadata proxies more than once for object type "
            + Utilities.simpleName(objectType));
    Preconditions.checkState(!keyMap.containsKey(objectType),
        "Cannot register more than one proxy key for an object type");
    Preconditions.checkState(!keyMap.inverse().containsKey(proxyTypeKey),
        "Cannot register more than one object type with the same proxy key");

    fromMetadataMap.put(proxyTypeKey, fromMetadata);
    toMetadataMap.put(objectType, toMetadata);
    keyMap.put(objectType, proxyTypeKey);

    if ((objectType.getModifiers() & Modifier.ABSTRACT) != 0) {
      abstractTypes.add(objectType);
    }

    if (objectType.isInterface()) {
      interfaceTypes.add(objectType);
    }

  }

  /**
   * Deregister (remove/don't track or use) the {@link ProvidesGenericObjectFromMetadata} associated
   * with this key, if any.
   * 
   * @param proxyTypeKey the key identifying the MetadataToObject to remove
   */
  public void deRegister(Key<?> proxyTypeKey) {
    Preconditions.checkNotNull(proxyTypeKey);

    fromMetadataMap.remove(proxyTypeKey);

    Class<?> objectType = keyMap.inverse().get(proxyTypeKey);
    if (objectType != null) {
      toMetadataMap.remove(objectType);
      keyMap.remove(objectType);
      abstractTypes.remove(objectType);
      interfaceTypes.remove(objectType);
    }
  }

  protected Class<?> findBestMatchForType(Class<?> objectType) {
    Preconditions.checkNotNull(objectType);

    Class<?> result = null;

    if (keyMap.containsKey(objectType)) {
      result = objectType;
    }

    if (result == null) {
      // Search for match to a superclass.
      for (Class<?> abstractType : abstractTypes) {
        if (abstractType.isAssignableFrom(objectType)) {
          result = abstractType;
          break;
        }
      }
    }

    if (result == null) {
      // Search for first match to an interface.
      for (Class<?> interfaceType : interfaceTypes) {
        if (interfaceType.isAssignableFrom(objectType)) {
          result = interfaceType;
          break;
        }
      }
    }

    return result;
  }
}

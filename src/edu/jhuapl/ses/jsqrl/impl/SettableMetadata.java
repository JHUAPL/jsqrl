package edu.jhuapl.ses.jsqrl.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.AbstractMetadata;
import edu.jhuapl.ses.jsqrl.impl.Utilities;

public class SettableMetadata extends AbstractMetadata {

  public static SettableMetadata of(Version version) {
    return new SettableMetadata(version, new ArrayList<>(), new HashMap<>());
  }

  public static SettableMetadata of(Metadata metadata) {
    Preconditions.checkNotNull(metadata);
    SettableMetadata result = SettableMetadata.of(metadata.getVersion());
    for (Key<?> key : metadata.getKeys()) {
      @SuppressWarnings("unchecked")
      Key<Object> objectKey = (Key<Object>) key;
      result.put(objectKey, metadata.get(key));
    }
    return result;
  }

  private final List<Key<?>> keys;
  private final Map<Key<?>, Object> map;

  protected SettableMetadata(Version version, List<Key<?>> keys, Map<Key<?>, Object> map) {
    super(version);
    Preconditions.checkNotNull(keys);
    Preconditions.checkNotNull(map);
    this.keys = keys;
    this.map = map;
  }

  @Override
  public ImmutableList<Key<?>> getKeys() {
    return ImmutableList.copyOf(keys);
  }

  @Override
  public SettableMetadata copy() {
    return new SettableMetadata(getVersion(), new ArrayList<>(keys), new HashMap<>(map));
  }

  @Override
  public ImmutableMap<Key<?>, Object> getMap() {
    return ImmutableMap.copyOf(map);
  }

  public final <V> SettableMetadata put(Key<V> key, V value) {
    Preconditions.checkNotNull(key);
    Class<?> storedAsType = checkStorable(value);
    if (!hasKey(key)) {
      keys.add(key);
    }
    if (value == null) {
      map.put(key, getNullObject());
    } else {
      map.put(key, copyOrUse(storedAsType, value));
    }
    return this;
  }

  public void clear() {
    keys.clear();
    map.clear();
  }

  protected static void validateIterable(Iterable<?> iterable) {
    for (Object item : iterable) {
      checkStorable(item);
    }
  }

  protected static void validateMap(Map<?, ?> map) {
    Class<?> mapKeyType = null;
    for (Entry<?, ?> entry : map.entrySet()) {
      Class<?> keyType = checkStorable(entry.getKey());
      if (mapKeyType == null) {
        mapKeyType = keyType;
      } else if (keyType != null && keyType != mapKeyType) {
        throw new IllegalArgumentException(
            "Cannot put a key of type " + Utilities.simpleName(keyType)
                + " in a map using keys of type " + Utilities.simpleName(mapKeyType));
      }
      checkStorable(entry.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  protected static <V> V copyOrUse(Class<?> storedAsType, V value) {
    if (SortedMap.class.isAssignableFrom(storedAsType)) {
      value = (V) new TreeMap<>((SortedMap<?, ?>) value);
    } else if (Map.class.isAssignableFrom(storedAsType)) {
      value = (V) new LinkedHashMap<>((Map<?, ?>) value);
    } else if (List.class.isAssignableFrom(storedAsType)) {
      value = (V) new ArrayList<>((List<?>) value);
    } else if (SortedSet.class.isAssignableFrom(storedAsType)) {
      value = (V) new TreeSet<>((SortedSet<?>) value);
    } else if (Set.class.isAssignableFrom(storedAsType)) {
      value = (V) new LinkedHashSet<>((Set<?>) value);
    }
    return value;
  }

  /**
   * Check whether the specified object may be represented as metadata. If it can, the type used by
   * the metadata system to represent the object is returned. If the object cannot be represented by
   * the metadata system, an {@link IllegalArgumentException} is thrown. This method never returns
   * null.
   * <p>
   * This method uses the {@link Utilities#classifyStorableType(Class)} to determine whether the
   * object is storable using the primary metadata mechanism. Unlike that method, however, if the
   * object is not storable in the primary way, but the item's class implements
   * {@link Serializable}, this method returns Serializable.class, because the item will be
   * successfully stored/retrieved using the secondary mechanism for storing Serializable items, in
   * which objects are serialized as Strings.
   * 
   * @param object the item to be stored as metadata
   * @return the type used to represent this item
   */
  protected static Class<?> checkStorable(Object object) {
    if (object == null) {
      return null;
    }
    if (object instanceof List) {
      validateIterable((Iterable<?>) object);
      return List.class;
    }
    if (object instanceof SortedMap) {
      validateMap((Map<?, ?>) object);
      return SortedMap.class;
    }
    if (object instanceof Map) {
      validateMap((Map<?, ?>) object);
      return Map.class;
    }
    if (object instanceof SortedSet) {
      validateIterable((Iterable<?>) object);
      return SortedSet.class;
    }
    if (object instanceof Set) {
      validateIterable((Iterable<?>) object);
      return Set.class;
    }

    Class<?> type = Utilities.classifyStorableType(object.getClass());

    if (type == null && Serializable.class.isAssignableFrom(object.getClass())) {
      type = Serializable.class;
    }

    if (type == null) {
      throw new IllegalArgumentException("Cannot directly represent objects of type "
          + Utilities.simpleName(object.getClass()) + " as metadata");
    }

    return type;
  }
}

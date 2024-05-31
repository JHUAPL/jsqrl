package edu.jhuapl.ses.jsqrl.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.MetadataManager;
import edu.jhuapl.ses.jsqrl.api.ProvidesMetadataFromGenericObject;
import edu.jhuapl.ses.jsqrl.api.StorableAsMetadata;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.InstanceGetter;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;

public class Utilities {

  // Release notes are now in the package-info file.

  public static <K> ImmutableMap<K, Metadata> bulkStore(Map<K, MetadataManager> managers) {
    Preconditions.checkNotNull(managers);

    ImmutableMap.Builder<K, Metadata> builder = ImmutableMap.builder();
    for (Entry<K, MetadataManager> entry : managers.entrySet()) {
      builder.put(entry.getKey(), entry.getValue().store());
    }

    return builder.build();
  }

  public static <K> void bulkRetrieve(Map<K, MetadataManager> managers, Map<K, Metadata> metadata) {
    Preconditions.checkNotNull(managers);
    Preconditions.checkNotNull(metadata);
    for (Entry<K, MetadataManager> entry : managers.entrySet()) {
      K key = entry.getKey();
      if (metadata.containsKey(key)) {
        entry.getValue().retrieve(metadata.get(key));
      }
    }
  }

  public static Key<?> provideTypeKeyIfPossible(Object object) {
    InstanceGetter instanceGetter = InstanceGetter.defaultInstanceGetter();

    Key<?> key = null;
    if (instanceGetter.isStorableAsMetadata(object)) {
      key = instanceGetter.getKeyForType(object.getClass());
    }

    return key;
  }

  public static Object provideMetadataIfPossible(Object object) {
    InstanceGetter instanceGetter = InstanceGetter.defaultInstanceGetter();
    if (instanceGetter.isStorableAsMetadata(object)) {

      @SuppressWarnings("unchecked")
      Class<Object> objectType = (Class<Object>) object.getClass();

      object = instanceGetter.providesMetadataFromGenericObject(objectType).provide(object);
    }

    return object;
  }

  public static String simpleName(Class<?> type) {
    if (type == null) {
      return null;
    }

    String simpleName = type.getSimpleName();
    if (simpleName.isEmpty()) {
      simpleName = type.getName().replaceAll(".*//.", "");
    }

    return simpleName;
  }

  /**
   * Determine how to represent the specified type as metadata. For all the types the metadata
   * system was designed to handle natively (int, Map, etc.) and for types handled by the
   * {@link InstanceGetter} mechanism, the appropriate base class or interface used to set up
   * serialization/deserialization is returned. If the type is not one of these directly supported
   * types, null is returned.
   * <p>
   * The interface {@link java.io.Serializable} is a special case. If the specified type is not one
   * of the directly-supported types described above, but the type is assignable to Serializable,
   * this method returns null. This signifies that the type in question is not supported by the
   * primary metadata mechanism. However, while this method returns null, the method
   * {@link SettableMetadata#checkStorable(Object)} returns Serializable.class in this case. This
   * works because there is a secondary mechanism for Serializable items, in which they are
   * serialized into a String. This is for dealing with hierarchies for which no explicit metadata
   * rules are set up for whatever reason.
   * 
   * @param type the actual type of object to be represented as metadata
   * @return the type the metadata system will use to store/retrieve such objects
   */
  public static Class<?> classifyStorableType(Class<?> type) {
    if (type == null) {
      return null;
    }
    if (InstanceGetter.defaultInstanceGetter().isTypeStorableAsMetadata(type)) {
      return ProvidesMetadataFromGenericObject.class;
    }
    if (Key.class.isAssignableFrom(type)) {
      return Key.class;
    }
    if (Metadata.class.isAssignableFrom(type)) {
      return Metadata.class;
    }
    if (StorableAsMetadata.class.isAssignableFrom(type)) {
      return StorableAsMetadata.class;
    }
    if (Version.class.isAssignableFrom(type)) {
      return Version.class;
    }
    if (List.class.isAssignableFrom(type)) {
      return List.class;
    }
    if (SortedMap.class.isAssignableFrom(type)) {
      return SortedMap.class;
    }
    if (Map.class.isAssignableFrom(type)) {
      return Map.class;
    }
    if (SortedSet.class.isAssignableFrom(type)) {
      return SortedSet.class;
    }
    if (Set.class.isAssignableFrom(type)) {
      return Set.class;
    }
    if (String.class.isAssignableFrom(type)) {
      return String.class;
    }
    if (Character.class.isAssignableFrom(type)) {
      return Character.class;
    }
    if (Boolean.class.isAssignableFrom(type)) {
      return Boolean.class;
    }
    if (Double.class.isAssignableFrom(type)) {
      return Double.class;
    }
    if (Float.class.isAssignableFrom(type)) {
      return Float.class;
    }
    if (Integer.class.isAssignableFrom(type)) {
      return Integer.class;
    }
    if (Long.class.isAssignableFrom(type)) {
      return Long.class;
    }
    if (Short.class.isAssignableFrom(type)) {
      return Short.class;
    }
    if (Byte.class.isAssignableFrom(type)) {
      return Byte.class;
    }
    if (Date.class.isAssignableFrom(type)) {
      return Date.class;
    }
    if (String[].class.isAssignableFrom(type)) {
      return String[].class;
    }
    if (Character[].class.isAssignableFrom(type)) {
      return Character[].class;
    }
    if (Boolean[].class.isAssignableFrom(type)) {
      return Boolean[].class;
    }
    if (Double[].class.isAssignableFrom(type)) {
      return Double[].class;
    }
    if (Float[].class.isAssignableFrom(type)) {
      return Float[].class;
    }
    if (Integer[].class.isAssignableFrom(type)) {
      return Integer[].class;
    }
    if (Long[].class.isAssignableFrom(type)) {
      return Long[].class;
    }
    if (Short[].class.isAssignableFrom(type)) {
      return Short[].class;
    }
    if (Byte[].class.isAssignableFrom(type)) {
      return Byte[].class;
    }
    if (Date[].class.isAssignableFrom(type)) {
      return Date[].class;
    }
    if (Metadata[].class.isAssignableFrom(type)) {
      return Metadata[].class;
    }
    if (char[].class.isAssignableFrom(type)) {
      return char[].class;
    }
    if (boolean[].class.isAssignableFrom(type)) {
      return boolean[].class;
    }
    if (double[].class.isAssignableFrom(type)) {
      return double[].class;
    }
    if (float[].class.isAssignableFrom(type)) {
      return float[].class;
    }
    if (int[].class.isAssignableFrom(type)) {
      return int[].class;
    }
    if (long[].class.isAssignableFrom(type)) {
      return long[].class;
    }
    if (short[].class.isAssignableFrom(type)) {
      return short[].class;
    }
    if (byte[].class.isAssignableFrom(type)) {
      return byte[].class;
    }
    if (Class.class.isAssignableFrom(type)) {
      return Class.class;
    }

    return null;
  }


}

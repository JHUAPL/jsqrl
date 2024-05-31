package edu.jhuapl.ses.jsqrl.impl.gson;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import com.google.gson.reflect.TypeToken;
import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.StorableAsMetadata;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.InstanceGetter;
import edu.jhuapl.ses.jsqrl.impl.Utilities;

/**
 * Enumerations representing different forms of type information associated with particular classes.
 * These enumerations are used via reflection to serialize objects, so it is important not to remove
 * any of these enumerations, or the associated types of objects will not be serializable. This is
 * true even if the particular enumerated items are not explicitly referenced.
 * 
 * This enumeration must be kept consistent with the object types supported by the metadata package.
 */
enum DataTypeInfo {
  ////////////////////////////////////////////////////////////////
  // Metadata-specific types.
  METADATA_KEY("Key", Key.class, new TypeToken<Key<?>>() {}.getType()), //
  METADATA("Metadata", Metadata.class, new TypeToken<Metadata>() {}.getType()), //
  VERSION("Version", Version.class, new TypeToken<Version>() {}.getType()), //
  ELEMENT("Element", GsonElement.class, new TypeToken<GsonElement>() {}.getType()), //
  PROXIED_OBJECT("ProxiedObject", StorableAsMetadata.class,
      new TypeToken<StorableAsMetadata<?>>() {}.getType()),
  ////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////
  // Collection types.
  LIST("List", List.class, new TypeToken<List<?>>() {}.getType()), //
  SORTED_MAP("SortedMap", SortedMap.class, new TypeToken<SortedMap<?, ?>>() {}.getType()), //
  MAP("Map", Map.class, new TypeToken<Map<?, ?>>() {}.getType()), //
  SORTED_SET("SortedSet", SortedSet.class, new TypeToken<SortedSet<?>>() {}.getType()), //
  SET("Set", Set.class, new TypeToken<Set<?>>() {}.getType()), //
  ITERABLE("Iterable", Iterable.class, new TypeToken<Iterable<?>>() {}.getType()), //
  ////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////
  // Common object types.
  STRING("String", String.class, new TypeToken<String>() {}.getType()), //
  CHARACTER_OBJECT("Character", Character.class, new TypeToken<Character>() {}.getType()), //
  BOOLEAN_OBJECT("Boolean", Boolean.class, new TypeToken<Boolean>() {}.getType()), //
  // Floating-point types.
  DOUBLE_OBJECT("Double", Double.class, new TypeToken<Double>() {}.getType()), //
  FLOAT_OBJECT("Float", Float.class, new TypeToken<Float>() {}.getType()), //
  // Integer types.
  INTEGER_OBJECT("Integer", Integer.class, new TypeToken<Integer>() {}.getType()), //
  LONG_OBJECT("Long", Long.class, new TypeToken<Long>() {}.getType()), //
  SHORT_OBJECT("Short", Short.class, new TypeToken<Short>() {}.getType()), //
  BYTE_OBJECT("Byte", Byte.class, new TypeToken<Byte>() {}.getType()), //
  DATE_OBJECT("Date", Date.class, new TypeToken<Date>() {}.getType()), //
  ////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////
  // Arrays of common object types.
  STRING_ARRAY("String Array", String[].class, new TypeToken<String[]>() {}.getType()), //
  CHARACTER_OBJECT_ARRAY("Character Array", Character[].class,
      new TypeToken<Character[]>() {}.getType()), //
  BOOLEAN_OBJECT_ARRAY("Boolean Array", Boolean[].class, new TypeToken<Boolean[]>() {}.getType()), //
  // Floating-point types.
  DOUBLE_OBJECT_ARRAY("Double Array", Double[].class, new TypeToken<Double[]>() {}.getType()), //
  FLOAT_OBJECT_ARRAY("Float Array", Float[].class, new TypeToken<Float[]>() {}.getType()), //
  // Integer types.
  INTEGER_OBJECT_ARRAY("Integer Array", Integer[].class, new TypeToken<Integer[]>() {}.getType()), //
  LONG_OBJECT_ARRAY("Long Array", Long[].class, new TypeToken<Long[]>() {}.getType()), //
  SHORT_OBJECT_ARRAY("Short Array", Short[].class, new TypeToken<Short[]>() {}.getType()), //
  BYTE_OBJECT_ARRAY("Byte Array", Byte[].class, new TypeToken<Byte[]>() {}.getType()), //
  DATE_ARRAY("Date Array", Date[].class, new TypeToken<Date[]>() {}.getType()), //
  METADATA_ARRAY("metadata Array", Metadata[].class, new TypeToken<Metadata[]>() {}.getType()), //
  ////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////
  // Arrays of primitive types.
  CHARACTER_ARRAY("char Array", char[].class, new TypeToken<char[]>() {}.getType()), //
  BOOLEAN_ARRAY("boolean Array", boolean[].class, new TypeToken<boolean[]>() {}.getType()), //
  // Floating-point types.
  DOUBLE_ARRAY("double Array", double[].class, new TypeToken<double[]>() {}.getType()), //
  FLOAT_ARRAY("float Array", float[].class, new TypeToken<float[]>() {}.getType()), //
  // Integer types.
  INTEGER_ARRAY("int Array", int[].class, new TypeToken<int[]>() {}.getType()), //
  LONG_ARRAY("long Array", long[].class, new TypeToken<long[]>() {}.getType()), //
  SHORT_ARRAY("short Array", short[].class, new TypeToken<short[]>() {}.getType()), //
  BYTE_ARRAY("byte Array", byte[].class, new TypeToken<byte[]>() {}.getType()), //
  ////////////////////////////////////////////////////////////////

  // In a class by itself: Class:
  CLASS("Class", Class.class, new TypeToken<Class<?>>() {}.getType()), //
  SERIALIZABLE("Serializable", Serializable.class, new TypeToken<Serializable>() {}.getType()), //

  // Catch-all case used both to handle nulls and to detect objects that cannot be serialized.
  NULL("Null", Object.class, new TypeToken<Object>() {}.getType()), //
  ;

  public static DataTypeInfo of(String typeId) {
    for (DataTypeInfo info : values()) {
      if (info.typeId.equals(typeId)) {
        return info;
      }
    }
    throw new IllegalArgumentException("Cannot decode unknown type " + typeId);
  }

  public static DataTypeInfo of(Class<?> valueClass) {
    for (DataTypeInfo info : values()) {
      if (info.valueClass.isAssignableFrom(valueClass)) {
        return info;
      }
    }

    // The final type info matches Object, which should be assignable from anything.
    throw new AssertionError();
  }

  public static DataTypeInfo of(Type type) {
    for (DataTypeInfo info : values()) {
      if (info.type.equals(type)) {
        return info;
      }
    }
    throw new IllegalArgumentException();
  }

  public static DataTypeInfo forObject(Object object) {
    DataTypeInfo result = NULL;
    if (object != null) {
      if (InstanceGetter.defaultInstanceGetter().isStorableAsMetadata(object)) {
        result = METADATA;
      }
      if (result == NULL) {
        result = of(object.getClass());
      }
      if (result == NULL) {
        throw new IllegalArgumentException("Cannot serialize object of type "
            + Utilities.simpleName(object.getClass()) + " to JSON format");
      }
    }
    return result;
  }

  private final String typeId;
  private final Class<?> valueClass;
  private final Type type;

  private DataTypeInfo(String typeId, Class<?> valueClass, Type type) {
    this.typeId = typeId;
    this.valueClass = valueClass;
    this.type = type;
  }

  public String getTypeId() {
    return typeId;
  }

  public Class<?> getTypeClass() {
    return valueClass;
  }

  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    return "TypeInfo: " + typeId + ", Type = " + type + ", class = "
        + Utilities.simpleName(valueClass);
  }

}

package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.InstanceGetter;
import edu.jhuapl.ses.jsqrl.impl.Utilities;

final class GsonElement {

  static final class ElementIO
      implements JsonSerializer<GsonElement>, JsonDeserializer<GsonElement> {
    @Override
    public GsonElement deserialize(JsonElement jsonElement, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {
      Preconditions.checkArgument(DataTypeInfo.ELEMENT.getType().equals(typeOfT));
      return GsonElement.of(jsonElement, context);
    }

    @Override
    public JsonElement serialize(GsonElement src, Type typeOfSrc,
        JsonSerializationContext context) {
      Preconditions.checkArgument(DataTypeInfo.ELEMENT.getType().equals(typeOfSrc));
      return src.toElement(context);
    }

  }

  private static final InstanceGetter INSTANCE_GETTER = InstanceGetter.defaultInstanceGetter();

  static final String VALUE_TYPE_KEY = "valueType";

  public static JsonElement encodeItem(Object item, boolean excludeMetadataVersion,
      JsonSerializationContext context) {

    item = Utilities.provideMetadataIfPossible(item);

    DataTypeInfo typeInfo = DataTypeInfo.forObject(item);

    if (excludeMetadataVersion && typeInfo == DataTypeInfo.METADATA) {
      return MetadataIOv2.encodeWithoutVersion((Metadata) item, context);
    }

    return context.serialize(item, typeInfo.getType());
  }

  public static JsonElement encodeItemWithType(Object item, boolean excludeMetadataVersion,
      JsonSerializationContext context) {

    Key<? extends Object> typeKey = Utilities.provideTypeKeyIfPossible(item);

    item = Utilities.provideMetadataIfPossible(item);

    String typeId = typeKey != null ? typeKey.getId() : DataTypeInfo.forObject(item).getTypeId();

    JsonObject result = new JsonObject();

    result.add(typeId, encodeItem(item, excludeMetadataVersion, context));

    return result;
  }

  public static void encodeTypeInfo(String typeId, JsonObject encodedItem) {
    encodedItem.addProperty(VALUE_TYPE_KEY, typeId);
  }

  /**
   * Decode item according to the supplied type, using the supplied version (if not null) for all
   * Metadata.
   * 
   * @param encodedItem
   * @param typeInfo
   * @param context
   * @return
   */
  public static Object decodeItem(JsonElement encodedItem, Key<?> typeKey, Version commonVersion,
      JsonDeserializationContext context) {

    if (INSTANCE_GETTER.isProvidableFromMetadata(typeKey)) {
      Metadata metadata =
          (Metadata) decodeItem(encodedItem, DataTypeInfo.METADATA, commonVersion, context);
      return INSTANCE_GETTER.providesGenericObjectFromMetadata(typeKey).provide(metadata);
    }

    DataTypeInfo typeInfo = DataTypeInfo.of(typeKey.getId());
    return decodeItem(encodedItem, typeInfo, commonVersion, context);
  }

  private static Object decodeItem(JsonElement encodedItem, DataTypeInfo typeInfo,
      Version commonVersion, JsonDeserializationContext context) {

    if (commonVersion != null && typeInfo == DataTypeInfo.METADATA) {
      return MetadataIOv2.decode(encodedItem.getAsJsonObject(), commonVersion, context);
    }

    return context.deserialize(encodedItem, typeInfo.getType());
  }

  /**
   * Decode item using self-contained type informtion but with the supplied Version for decoding
   * compressed Metadata.
   * 
   * @param encodedItem
   * @param commonVersion
   * @param context
   * @return
   */
  public static Object decodeItem(JsonElement encodedItem, Version commonVersion,
      JsonDeserializationContext context) {
    Preconditions.checkArgument(encodedItem.isJsonObject());

    JsonObject encodedTypeAndValue = encodedItem.getAsJsonObject();
    Preconditions.checkState(encodedTypeAndValue.size() == 1);

    // Easiest way to get to the single entry is just to use a one-time loop.
    // That way we don't have to mess with the entry set, iterators etc.
    for (Entry<String, JsonElement> entry : encodedTypeAndValue.entrySet()) {
      Key<?> proxyTypeKey = Key.of(entry.getKey());
      JsonElement encodedValue = entry.getValue();

      if (INSTANCE_GETTER.isProvidableFromMetadata(proxyTypeKey)) {
        Metadata metadata =
            (Metadata) decodeItem(encodedValue, DataTypeInfo.METADATA, commonVersion, context);
        return INSTANCE_GETTER.providesGenericObjectFromMetadata(proxyTypeKey).provide(metadata);
      }

      DataTypeInfo typeInfo = DataTypeInfo.of(proxyTypeKey.getId());

      return decodeItem(encodedValue, typeInfo, commonVersion, context);
    }

    // Can't get here because the loop above is guaranteed to execute at least once.
    throw new AssertionError();
  }

  public static Key<?> decodeTypeInfo(JsonObject encodedItem) {
    Key<?> typeKey = null;

    if (encodedItem.has(VALUE_TYPE_KEY)) {
      JsonElement encodedType = encodedItem.get(VALUE_TYPE_KEY);
      typeKey = Key.of(encodedType.getAsString());
    }

    return typeKey;
  }

  public static GsonElement of(Object object) {
    return new GsonElement(object, DataTypeInfo.forObject(object));
  }

  private static GsonElement of(JsonElement element, JsonDeserializationContext context) {
    Preconditions.checkNotNull(element);
    Preconditions.checkNotNull(context);
    Preconditions.checkArgument(element.isJsonObject());

    JsonObject valueObject = element.getAsJsonObject();

    Set<Entry<String, JsonElement>> entryList = valueObject.entrySet();
    Preconditions.checkState(entryList.size() == 1);

    Entry<String, JsonElement> entry = entryList.iterator().next();
    DataTypeInfo objectInfo = DataTypeInfo.of(entry.getKey());

    Object object = context.deserialize(entry.getValue(), objectInfo.getType());

    return new GsonElement(object, objectInfo);
  }

  private final Object object;
  private final DataTypeInfo objectInfo;

  private GsonElement(Object object, DataTypeInfo objectInfo) {
    Preconditions.checkNotNull(objectInfo);
    this.object = object;
    this.objectInfo = objectInfo;
  }

  public JsonElement toElement(JsonSerializationContext context) {
    JsonObject result = new JsonObject();
    result.add(objectInfo.getTypeId(), context.serialize(object, objectInfo.getType()));
    return result;
  }

  public Object getValue() {
    return object;
  }

}

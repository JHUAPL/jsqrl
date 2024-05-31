package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;

final class MetadataIOv2 implements MetadataIO {

  private static final String VERSION_KEY = "metadata.Version";

  public static JsonObject encodeWithoutVersion(Metadata src, JsonSerializationContext context) {
    JsonObject result = encodeWithVersion(src, context);

    result.remove(DataTypeInfo.VERSION.getTypeId());

    return result;
  }

  public static JsonObject encodeWithVersion(Metadata src, JsonSerializationContext context) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (Key<?> key : src.getKeys()) {
      map.put(key.getId(), src.get(key));
    }

    // Use MapIO to do the detailed encoding.
    MapIOv2 mapIo = new MapIOv2();
    JsonObject encodedMap =
        mapIo.serialize(map, DataTypeInfo.MAP.getType(), context).getAsJsonObject();

    // Pull out info needed to characterize the Metadata. (Skip the Key type -- known to be string).
    JsonObject result = new JsonObject();
    result.add(DataTypeInfo.VERSION.getTypeId(), GsonVersionIO.encode(src.getVersion()));
    if (encodedMap.has(GsonElement.VALUE_TYPE_KEY)) {
      result.add(GsonElement.VALUE_TYPE_KEY, encodedMap.get(GsonElement.VALUE_TYPE_KEY));
    }
    if (encodedMap.has(VERSION_KEY)) {
      result.add(VERSION_KEY, encodedMap.get(VERSION_KEY));
    }

    // Rebundle the serialized map so it's stored in the same order as the metadata.
    JsonObject keyValueObject = encodedMap.get(MapBaseIOv2.VALUE_KEY).getAsJsonObject();
    JsonObject keyValueObjectInOrder = new JsonObject();
    for (Key<?> key : src.getKeys()) {
      String keyId = key.getId();
      keyValueObjectInOrder.add(keyId, keyValueObject.get(keyId));
    }

    result.add(MapBaseIOv2.VALUE_KEY, keyValueObjectInOrder);

    return result;
  }

  public static void encodeVersion(Version version, JsonObject object) {
    object.add(VERSION_KEY, GsonVersionIO.encode(version));
  }

  public static Metadata decode(JsonObject jsonMetadata, Version version,
      JsonDeserializationContext context) {
    Preconditions.checkNotNull(jsonMetadata);
    Preconditions.checkNotNull(version);
    Preconditions.checkNotNull(context);

    // Reverse the encoding process. The ordered map is stored as the "value".
    JsonObject keyValueObjectInOrder = jsonMetadata.get(MapBaseIOv2.VALUE_KEY).getAsJsonObject();

    // Convert the supplied jsonMetadata object into the same format as an encoded map.
    JsonObject encodedMap = new JsonObject();
    // Note the key type was not serialized but we need to add it here.
    encodedMap.addProperty(MapBaseIOv2.KEY_TYPE_KEY, DataTypeInfo.STRING.getTypeId());
    if (jsonMetadata.has(GsonElement.VALUE_TYPE_KEY)) {
      encodedMap.add(GsonElement.VALUE_TYPE_KEY, jsonMetadata.get(GsonElement.VALUE_TYPE_KEY));
    }
    if (jsonMetadata.has(VERSION_KEY)) {
      encodedMap.add(VERSION_KEY, jsonMetadata.get(VERSION_KEY));
    }
    encodedMap.add(MapBaseIOv2.VALUE_KEY, keyValueObjectInOrder);

    // Now decode the synthesized encoded map.
    MapIOv2 mapIo = new MapIOv2();
    @SuppressWarnings("unchecked")
    Map<String, Object> map =
        (Map<String, Object>) mapIo.deserialize(encodedMap, DataTypeInfo.MAP.getType(), context);

    // Finally use the key order from the serialized form to reconstruct the metadata object content
    // in order.
    SettableMetadata metadata = SettableMetadata.of(version);
    for (Entry<String, JsonElement> entry : keyValueObjectInOrder.entrySet()) {
      String keyId = entry.getKey();
      // Pull the reconstructed elements of the metadata from the map.
      metadata.put(Key.of(keyId), map.get(keyId));
    }

    return metadata;
  }

  public static Metadata decodeWithVersion(JsonObject jsonMetadata,
      JsonDeserializationContext context) {
    Preconditions.checkNotNull(jsonMetadata);

    Version version = decodeVersion(jsonMetadata);

    return decode(jsonMetadata, version, context);
  }

  public static Version decodeVersion(JsonObject object) {
    Version result = null;

    if (object.has(VERSION_KEY)) {
      result = GsonVersionIO.decode(object.get(VERSION_KEY).getAsJsonPrimitive());
    } else if (object.has(DataTypeInfo.VERSION.getTypeId())) {
      result =
          GsonVersionIO.decode(object.get(DataTypeInfo.VERSION.getTypeId()).getAsJsonPrimitive());
    }

    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * edu.jhuapl.ses.jsqrl.impl.gson.MetadataIO#serialize(edu.jhuapl.ses.jsqrl.api.Metadata,
   * java.lang.reflect.Type, com.google.gson.JsonSerializationContext)
   */
  @Override
  public JsonElement serialize(Metadata src, Type typeOfSrc, JsonSerializationContext context) {
    Preconditions.checkArgument(DataTypeInfo.METADATA.getType().equals(typeOfSrc));
    return encodeWithVersion(src, context);
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.jhuapl.ses.jsqrl.impl.gson.MetadataIO#deserialize(com.google.gson.JsonElement,
   * java.lang.reflect.Type, com.google.gson.JsonDeserializationContext)
   */
  @Override
  public Metadata deserialize(JsonElement jsonSrc, Type typeOfT,
      JsonDeserializationContext context) {
    Preconditions.checkNotNull(jsonSrc);
    Preconditions.checkArgument(jsonSrc.isJsonObject());
    Preconditions.checkArgument(DataTypeInfo.METADATA.getType().equals(typeOfT));

    return decodeWithVersion(jsonSrc.getAsJsonObject(), context);
  }

}

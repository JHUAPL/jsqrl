package edu.jhuapl.ses.jsqrl.impl.gson;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.Utilities;

abstract class MapBaseIOv2 implements JsonSerializer<Map<?, ?>> {

  protected static Object decodeKeyString(String keyString, DataTypeInfo typeInfo,
      JsonDeserializationContext context) {
    JsonElement keyElement =
        keyString.equals("null") ? JsonNull.INSTANCE : new JsonPrimitive(keyString);
    return context.deserialize(keyElement, typeInfo.getType());
  }

  static final String KEY_TYPE_KEY = "keyType";
  static final String VALUE_KEY = "value";
  private static final Key<?> PRIVATE_NULL_KEY = Key.of("PRIVATE NULL KEY");

  protected MapBaseIOv2() {

  }

  @Override
  public JsonElement serialize(Map<?, ?> src, @SuppressWarnings("unused") Type typeOfSrc,
      JsonSerializationContext context) {
    Preconditions.checkNotNull(src);
    Preconditions.checkNotNull(context);

    Set<?> keys = src.keySet();

    // First pass: if any entries are found, determine types of key and value.
    List<Object> values = new ArrayList<>();
    DataTypeInfo mapKeyInfo = DataTypeInfo.NULL;
    // Note that although mapValueInfo and mapValueTypeKey are very similar, there are nuances
    // that lead to the need to treat them slightly differently with respect to how null values are
    // handled/categorized.
    DataTypeInfo mapValueInfo = DataTypeInfo.NULL; // Meaning a value is a null pointer.
    Key<?> mapValueTypeKey = null; // Meaning the instance getter could not decode this.
    boolean sameValueType = keys.size() > 1;

    for (Object key : keys) {
      DataTypeInfo keyInfo = DataTypeInfo.forObject(key);
      Object value = src.get(key);
      values.add(value);

      if (mapKeyInfo == DataTypeInfo.NULL) {
        mapKeyInfo = keyInfo;
      } else if (keyInfo != DataTypeInfo.NULL && keyInfo != mapKeyInfo) {
        // This could be supported if anyone ever needs it.
        throw new IllegalStateException("Cannot store a key of type " + keyInfo.getTypeId()
            + " in a map inferred to have keys of type " + mapKeyInfo.getTypeId());
      }

      DataTypeInfo valueInfo = DataTypeInfo.forObject(value);
      if (mapValueInfo == DataTypeInfo.NULL) {
        mapValueInfo = valueInfo;
      } else if (valueInfo != DataTypeInfo.NULL && valueInfo != mapValueInfo) {
        sameValueType = false;
      }

      // See if there is a provider for the type indicated by this key.
      Key<?> valueTypeKey = Utilities.provideTypeKeyIfPossible(value);
      if (valueTypeKey == null) {
        // Use a null object rather than null pointer to distinguish between the cases "no provider"
        // and "no provider YET".
        valueTypeKey = PRIVATE_NULL_KEY;
      }

      if (mapValueTypeKey == null) {
        mapValueTypeKey = valueTypeKey;
      } else if (mapValueTypeKey == PRIVATE_NULL_KEY || valueTypeKey == PRIVATE_NULL_KEY) {
        // Use object equality for comparisons involving the private null key. This is to prevent a
        // name collision should the user happen to define type key with the same name as the
        // private null key.
        if (mapValueTypeKey != valueTypeKey) {
          sameValueType = false;
          break;
        }
      } else if (!mapValueTypeKey.equals(valueTypeKey)) {
        sameValueType = false;
      }
    }

    // The private null key is just for use within this method, so replace it with null before
    // continuing.
    if (mapValueTypeKey == PRIVATE_NULL_KEY) {
      mapValueTypeKey = null;
    }

    // Extract metadata version, if any.
    Version commonVersion = IterableIOv2.getCommonVersion(values);
    boolean excludeMetadataVersionInValues = commonVersion != null;

    // Second pass: write the map entries to a JsonObject.
    JsonObject encodedMap = new JsonObject();
    for (Object metadataKey : keys) {
      Object key = metadataKey != null ? metadataKey : JsonNull.INSTANCE;
      Object value = src.get(metadataKey);

      JsonElement encodedValue =
          sameValueType ? GsonElement.encodeItem(value, excludeMetadataVersionInValues, context)
              : GsonElement.encodeItemWithType(value, excludeMetadataVersionInValues, context);

      encodedMap.add(key.toString(), encodedValue);
    }

    // Put type information about key and value, along with the map entries
    // into the resultant object.
    JsonObject result = new JsonObject();
    result.addProperty(KEY_TYPE_KEY, mapKeyInfo.getTypeId());
    if (sameValueType) {
      String mapValueTypeId =
          mapValueTypeKey != null ? mapValueTypeKey.getId() : mapValueInfo.getTypeId();
      GsonElement.encodeTypeInfo(mapValueTypeId, result);
    }
    if (excludeMetadataVersionInValues) {
      MetadataIOv2.encodeVersion(commonVersion, result);
    }
    result.add(VALUE_KEY, encodedMap);

    return result;
  }

  protected void deserialize(JsonObject encodedItem, JsonDeserializationContext context,
      Map<Object, Object> map) {
    DataTypeInfo mapKeyInfo = DataTypeInfo.of(encodedItem.get(KEY_TYPE_KEY).getAsString());
    Key<?> mapValueType = GsonElement.decodeTypeInfo(encodedItem);
    Version commonVersion = MetadataIOv2.decodeVersion(encodedItem);
    JsonObject encodedMap = encodedItem.get(VALUE_KEY).getAsJsonObject();

    for (Entry<String, JsonElement> entry : encodedMap.entrySet()) {
      Object key = decodeKeyString(entry.getKey(), mapKeyInfo, context);
      Object value = mapValueType != null
          ? GsonElement.decodeItem(entry.getValue(), mapValueType, commonVersion, context)
          : GsonElement.decodeItem(entry.getValue(), commonVersion, context);
      map.put(key, value);
    }
  }

  public static void main(String[] args) {
    Map<Integer, String> map1 = new HashMap<>();
    map1.put(1, "One");
    map1.put(null, "Null");

    Map<Integer, String> map2 = new HashMap<>();
    map2.put(0, "Zero");
    map2.put(2, null);

    SortedMap<String, Map<Integer, String>> createdMap = new TreeMap<>();
    createdMap.put("Map one", map1);
    createdMap.put("Map two", map2);

    Gson GSON = new GsonBuilder().serializeNulls()
        .registerTypeAdapter(DataTypeInfo.of(SortedMap.class).getType(), new SortedMapIOv2())
        .registerTypeAdapter(DataTypeInfo.of(Map.class).getType(), new MapIOv2())
        .setPrettyPrinting().create();

    String testPath = Paths.get(System.getProperty("user.home"), "Downloads").toString();
    String file = Paths.get(testPath, "test-map.json").toString();
    try (FileWriter fileWriter = new FileWriter(file)) {
      try (JsonWriter jsonWriter = GSON.newJsonWriter(fileWriter)) {
        GSON.toJson(createdMap, DataTypeInfo.forObject(createdMap).getType(), jsonWriter);
        fileWriter.write('\n');
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    try (JsonReader jsonReader = GSON.newJsonReader(new FileReader(file))) {
      Map<?, ?> readMap = GSON.fromJson(jsonReader, DataTypeInfo.of(Map.class).getType());
      if (!readMap.equals(createdMap)) {
        System.err.println("OUTPUT IS NOT EQUAL TO INPUT!!");
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}

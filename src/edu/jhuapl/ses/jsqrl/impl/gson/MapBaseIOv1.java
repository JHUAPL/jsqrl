package edu.jhuapl.ses.jsqrl.impl.gson;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

abstract class MapBaseIOv1 implements JsonSerializer<Map<?, ?>> {
  /**
   * Internal utility class; just here so the unpack method can return all the distinct pieces of
   * information contained in the input JsonElement.
   */
  protected static class DeserializedJsonObject {
    protected final DataTypeInfo keyTypeInfo;
    protected final DataTypeInfo valueTypeInfo;
    protected final JsonObject jsonMap;

    protected DeserializedJsonObject(DataTypeInfo keyTypeInfo, DataTypeInfo valueTypeInfo,
        JsonObject jsonMap) {
      this.keyTypeInfo = keyTypeInfo;
      this.valueTypeInfo = valueTypeInfo;
      this.jsonMap = jsonMap;
    }
  }

  private static final String MAP_KEY_TYPE = "keyType";
  private static final String MAP_VALUE_TYPE = "valueType";
  private static final String MAP_VALUE = "value";

  protected MapBaseIOv1() {

  }

  @Override
  public JsonElement serialize(Map<?, ?> src, @SuppressWarnings("unused") Type typeOfSrc,
      JsonSerializationContext context) {
    JsonObject result = new JsonObject();

    // First pass: if any entries are found, determine types of key and value.
    DataTypeInfo keyInfo = DataTypeInfo.NULL;
    DataTypeInfo valueInfo = DataTypeInfo.NULL;
    for (Entry<?, ?> entry : src.entrySet()) {
      if (keyInfo == DataTypeInfo.NULL) {
        keyInfo = DataTypeInfo.forObject(entry.getKey());
      }
      if (valueInfo == DataTypeInfo.NULL) {
        valueInfo = DataTypeInfo.forObject(entry.getValue());
      }
      if (keyInfo != DataTypeInfo.NULL && valueInfo != DataTypeInfo.NULL) {
        break;
      }
    }

    // Second pass: write the map entries to a JsonObject.
    JsonObject dest = new JsonObject();
    Type valueType = valueInfo.getType();

    for (Entry<?, ?> entry : src.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      dest.add(key != null ? key.toString() : JsonNull.INSTANCE.toString(),
          context.serialize(value, valueType));
    }

    // Put type information about key and value, along with the map entries
    // into the resultant object.
    result.add(MAP_KEY_TYPE, context.serialize(keyInfo.getTypeId()));
    result.add(MAP_VALUE_TYPE, context.serialize(valueInfo.getTypeId()));
    result.add(MAP_VALUE, dest);

    return result;
  }

  public DeserializedJsonObject unpack(JsonElement jsonElement) {
    if (!jsonElement.isJsonObject()) {
      throw new IllegalArgumentException();
    }

    JsonObject object = jsonElement.getAsJsonObject();

    // Unpack metadata.
    JsonElement keyTypeElement = object.get(MAP_KEY_TYPE);
    if (keyTypeElement == null || !keyTypeElement.isJsonPrimitive()) {
      throw new IllegalArgumentException(
          "Field \"" + MAP_KEY_TYPE + "\" is missing or has wrong type in Json object");
    }

    JsonElement valueTypeElement = object.get(MAP_VALUE_TYPE);
    if (valueTypeElement == null || !valueTypeElement.isJsonPrimitive()) {
      throw new IllegalArgumentException(
          "Field \"" + MAP_VALUE_TYPE + "\" is missing or has wrong type in Json object");
    }

    DataTypeInfo keyInfo = DataTypeInfo.of(keyTypeElement.getAsString());
    DataTypeInfo valueInfo = DataTypeInfo.of(valueTypeElement.getAsString());

    // Unpack data.
    JsonElement dataElement = object.get(MAP_VALUE);
    if (dataElement == null || !dataElement.isJsonObject()) {
      throw new IllegalArgumentException(
          "Field \"" + MAP_VALUE + "\" is missing or has wrong type in Json object");
    }

    return new DeserializedJsonObject(keyInfo, valueInfo, dataElement.getAsJsonObject());
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
        .registerTypeAdapter(DataTypeInfo.of(SortedMap.class).getType(), new SortedMapIOv1())
        .registerTypeAdapter(DataTypeInfo.of(Map.class).getType(), new MapIOv1())
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

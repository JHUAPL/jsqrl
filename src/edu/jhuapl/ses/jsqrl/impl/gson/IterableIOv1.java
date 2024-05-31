package edu.jhuapl.ses.jsqrl.impl.gson;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

abstract class IterableIOv1 implements JsonSerializer<Iterable<?>> {
  /**
   * Internal utility class; just here so the unpack method can return all the distinct pieces of
   * information contained in the input JsonElement.
   */
  protected static class DeserializedJsonArray {
    protected final DataTypeInfo dataTypeInfo;
    protected final JsonArray jsonArray;

    protected DeserializedJsonArray(DataTypeInfo dataTypeInfo, JsonArray jsonArray) {
      this.dataTypeInfo = dataTypeInfo;
      this.jsonArray = jsonArray;
    }
  }

  protected IterableIOv1() {

  }

  private static final String ITERABLE_VALUE_TYPE = "valueType";
  private static final String ITERABLE_VALUE = "value";

  @Override
  public JsonElement serialize(Iterable<?> src, @SuppressWarnings("unused") Type typeOfSrc,
      JsonSerializationContext context) {
    JsonObject result = new JsonObject();

    // First pass: if any entries are found, determine types of key and value based on the type of
    // the first non-null element.
    DataTypeInfo valueInfo = DataTypeInfo.NULL;
    for (Object value : src) {
      if (valueInfo == DataTypeInfo.NULL) {
        valueInfo = DataTypeInfo.forObject(value);
      }
      if (valueInfo != DataTypeInfo.NULL) {
        break;
      }
    }

    // Second pass: write the collection's entries to a JsonArray.
    JsonArray destArray = new JsonArray();
    Type valueType = valueInfo.getType();

    for (Object value : src) {
      destArray.add(context.serialize(value, valueType));
    }

    // Put iterable metadata and data into the resultant object.
    result.add(ITERABLE_VALUE_TYPE, context.serialize(valueInfo.getTypeId()));
    result.add(ITERABLE_VALUE, destArray);

    return result;
  }

  protected DeserializedJsonArray unpack(JsonElement jsonElement) {
    if (!jsonElement.isJsonObject()) {
      throw new IllegalArgumentException();
    }

    JsonObject object = jsonElement.getAsJsonObject();

    // Unpack metadata.
    JsonElement dataTypeElement = object.get(ITERABLE_VALUE_TYPE);
    if (dataTypeElement == null || !dataTypeElement.isJsonPrimitive()) {
      throw new IllegalArgumentException(
          "Field \"" + ITERABLE_VALUE_TYPE + "\" is missing or has wrong type in Json object");
    }

    DataTypeInfo dataTypeInfo = DataTypeInfo.of(dataTypeElement.getAsString());

    // Unpack data.
    JsonElement dataElement = object.get(ITERABLE_VALUE);
    if (dataElement == null || !dataElement.isJsonArray()) {
      throw new IllegalArgumentException(
          "Field \"" + ITERABLE_VALUE + "\" is missing or has wrong type in Json object");
    }

    return new DeserializedJsonArray(dataTypeInfo, dataElement.getAsJsonArray());
  }

  public static void main(String[] args) {
    Set<String> set1 = new HashSet<>();
    set1.add("One");
    set1.add(null);

    Set<String> set2 = new HashSet<>();
    set2.add("Zero");
    set2.add("Two");

    List<Set<String>> createdList = new ArrayList<>();
    createdList.add(set1);
    createdList.add(set2);

    Gson GSON = new GsonBuilder().serializeNulls()
        .registerTypeAdapter(DataTypeInfo.of(SortedSet.class).getType(), new SortedSetIOv1())
        .registerTypeAdapter(DataTypeInfo.of(Set.class).getType(), new SetIOv1())
        .registerTypeAdapter(DataTypeInfo.of(List.class).getType(), new ListIOv1())
        .setPrettyPrinting().create();

    String testPath = Paths.get(System.getProperty("user.home"), "Downloads").toString();
    String file = Paths.get(testPath, "test-iterables.json").toString();
    try (FileWriter fileWriter = new FileWriter(file)) {
      try (JsonWriter jsonWriter = GSON.newJsonWriter(fileWriter)) {
        GSON.toJson(createdList, DataTypeInfo.forObject(createdList).getType(), jsonWriter);
        fileWriter.write('\n');
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    try (JsonReader jsonReader = GSON.newJsonReader(new FileReader(file))) {
      Iterable<?> readList = GSON.fromJson(jsonReader, DataTypeInfo.of(List.class).getType());
      if (!readList.equals(createdList)) {
        System.err.println("OUTPUT IS NOT EQUAL TO INPUT!!");
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}

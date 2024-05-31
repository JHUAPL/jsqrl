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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.Utilities;

abstract class IterableIOv2 implements JsonSerializer<Iterable<?>> {

  /**
   * Iterate over objects that may include implementations of Metadata and extract the common
   * Metadata Version, if there is one.
   * 
   * @param iterable the objects to examine
   * @return the version that all the metadata share, or null if either there are no Metadata
   *         objects, if the Metadata objects do not share a common version, or if there is only one
   *         Metadata object
   */
  public static Version getCommonVersion(Iterable<?> iterable) {
    Version result = null;

    int metadataCount = 0;
    for (Object item : iterable) {
      if (item instanceof Metadata) {
        ++metadataCount;
        Version itemVersion = ((Metadata) item).getVersion();

        if (result == null) {
          // First Metadata found -- provisionally make its Version the result.
          result = itemVersion;

        } else if (!result.equals(itemVersion)) {
          // More than one Metadata was found, and it has a different Version from the provisional
          // result. Discard the provisional result and don't look any further.
          result = null;
          break;

        }
      }
    }

    // If only one metadata object is present, do not return a "common" version.
    if (metadataCount == 1) {
      result = null;
    }

    return result;
  }

  protected IterableIOv2() {

  }

  private static final String VALUE_KEY = "value";
  private static final Key<?> PRIVATE_NULL_KEY = Key.of("PRIVATE NULL KEY");

  @Override
  public JsonElement serialize(Iterable<?> src, @SuppressWarnings("unused") Type typeOfSrc,
      JsonSerializationContext context) {

    // First pass: if any entries are found, determine types of key and value based on the type of
    // the first non-null element.
    // Note that although iterableTypeInfo and iterableTypeKey are very similar, there are nuances
    // that lead to the need to treat them slightly differently with respect to how null values are
    // handled/categorized.
    DataTypeInfo iterableTypeInfo = DataTypeInfo.NULL; // Meaning an entry is a null pointer.
    Key<?> iterableTypeKey = null; // Meaning the instance getter could not decode this.
    boolean sameType = true;
    for (Object item : src) {
      DataTypeInfo valueInfo = DataTypeInfo.forObject(item);
      if (iterableTypeInfo == DataTypeInfo.NULL) {
        iterableTypeInfo = valueInfo;
      } else if (valueInfo != DataTypeInfo.NULL && valueInfo != iterableTypeInfo) {
        sameType = false;
        break;
      }

      // See if there is a provider for the type indicated by this key.
      Key<?> valueTypeKey = Utilities.provideTypeKeyIfPossible(item);
      if (valueTypeKey == null) {
        // Use a null object rather than null pointer to distinguish between the cases "no provider"
        // and "no provider YET".
        valueTypeKey = PRIVATE_NULL_KEY;
      }

      if (iterableTypeKey == null) {
        iterableTypeKey = valueTypeKey;
      } else if (iterableTypeKey == PRIVATE_NULL_KEY || valueTypeKey == PRIVATE_NULL_KEY) {
        // Use object equality for comparisons involving the private null key. This is to prevent a
        // name collision should the user happen to define type key with the same name as the
        // private null key.
        if (iterableTypeKey != valueTypeKey) {
          sameType = false;
          break;
        }
      } else if (!iterableTypeKey.equals(valueTypeKey)) {
        sameType = false;
        break;
      }
    }

    // The private null key is just for use within this method, so replace it with null before
    // continuing.
    if (iterableTypeKey == PRIVATE_NULL_KEY) {
      iterableTypeKey = null;
    }

    // Extract metadata version, if any.
    Version commonVersion = getCommonVersion(src);
    boolean excludeMetadataVersionInValues = commonVersion != null;

    // Second pass: write items, either with or without their types depending on whether they are
    // the same.
    JsonArray jsonArray = new JsonArray();
    for (Object item : src) {
      JsonElement encodedItem =
          sameType ? GsonElement.encodeItem(item, excludeMetadataVersionInValues, context)
              : GsonElement.encodeItemWithType(item, excludeMetadataVersionInValues, context);
      jsonArray.add(encodedItem);
    }

    // Put iterable metadata and data into the resultant object.
    JsonObject result = new JsonObject();
    if (sameType) {
      String iterableTypeId =
          iterableTypeKey != null ? iterableTypeKey.getId() : iterableTypeInfo.getTypeId();
      GsonElement.encodeTypeInfo(iterableTypeId, result);
    }
    if (excludeMetadataVersionInValues) {
      MetadataIOv2.encodeVersion(commonVersion, result);
    }
    result.add(VALUE_KEY, jsonArray);

    return result;
  }

  protected List<?> deserialize(JsonObject encodedIterable, JsonDeserializationContext context) {
    Key<?> typeKey = GsonElement.decodeTypeInfo(encodedIterable);

    Version commonVersion = MetadataIOv2.decodeVersion(encodedIterable);

    JsonArray encodedArray = encodedIterable.get(VALUE_KEY).getAsJsonArray();

    List<Object> result = new ArrayList<>();
    for (JsonElement encodedItem : encodedArray) {
      if (typeKey != null) {
        result.add(GsonElement.decodeItem(encodedItem, typeKey, commonVersion, context));
      } else {
        result.add(GsonElement.decodeItem(encodedItem, commonVersion, context));
      }
    }
    return result;
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
        .registerTypeAdapter(DataTypeInfo.of(SortedSet.class).getType(), new SortedSetIOv2())
        .registerTypeAdapter(DataTypeInfo.of(Set.class).getType(), new SetIOv2())
        .registerTypeAdapter(DataTypeInfo.of(List.class).getType(), new ListIOv2())
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

package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

final class SortedSetIOv1 extends IterableIOv1 implements SortedSetIO {
  @Override
  public SortedSet<?> deserialize(JsonElement jsonElement, @SuppressWarnings("unused") Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    DeserializedJsonArray array = unpack(jsonElement);
    DataTypeInfo dataInfo = array.dataTypeInfo;
    JsonArray jsonArray = array.jsonArray;

    // Create output data object.
    SortedSet<?> result = new TreeSet<>();

    Type valueType = dataInfo.getType();
    for (JsonElement entryElement : jsonArray) {
      result.add(context.deserialize(entryElement, valueType));
    }
    return result;
  }

}

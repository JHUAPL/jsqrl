package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

final class SortedMapIOv1 extends MapBaseIOv1 implements SortedMapIO {
  @Override
  public SortedMap<?, ?> deserialize(JsonElement jsonElement,
      @SuppressWarnings("unused") Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    SortedMap<?, ?> result = new TreeMap<>();

    DeserializedJsonObject object = unpack(jsonElement);
    Type keyType = object.keyTypeInfo.getType();
    Type valueType = object.valueTypeInfo.getType();

    for (Entry<String, JsonElement> entry : object.jsonMap.getAsJsonObject().entrySet()) {
      String keyString = entry.getKey();
      JsonElement keyElement =
          keyString.equals("null") ? JsonNull.INSTANCE : new JsonPrimitive(keyString);
      JsonElement valueElement = entry.getValue();

      result.put(context.deserialize(keyElement, keyType),
          context.deserialize(valueElement, valueType));
    }

    return result;
  }

}

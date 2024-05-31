package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

final class MapIOv1 extends MapBaseIOv1 implements MapIO {
  @Override
  public Map<?, ?> deserialize(JsonElement jsonElement, @SuppressWarnings("unused") Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    Map<?, ?> result = new HashMap<>();

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

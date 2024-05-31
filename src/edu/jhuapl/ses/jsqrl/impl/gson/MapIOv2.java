package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;

final class MapIOv2 extends MapBaseIOv2 implements MapIO {

  @Override
  public Map<?, ?> deserialize(JsonElement jsonElement, @SuppressWarnings("unused") Type typeOfT,
      JsonDeserializationContext context) {

    Preconditions.checkArgument(jsonElement.isJsonObject());

    Map<Object, Object> result = new LinkedHashMap<>();
    deserialize(jsonElement.getAsJsonObject(), context, result);

    return result;
  }

}

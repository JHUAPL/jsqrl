package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;

final class SortedMapIOv2 extends MapBaseIOv2 implements SortedMapIO {

  @Override
  public SortedMap<?, ?> deserialize(JsonElement jsonElement,
      @SuppressWarnings("unused") Type typeOfT, JsonDeserializationContext context) {

    Preconditions.checkArgument(jsonElement.isJsonObject());

    SortedMap<Object, Object> result = new TreeMap<>();
    deserialize(jsonElement.getAsJsonObject(), context, result);

    return result;
  }

}

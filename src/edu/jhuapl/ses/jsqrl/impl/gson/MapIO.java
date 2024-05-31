package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;


interface MapIO extends JsonDeserializer<Map<?, ?>> {

  @Override
  Map<?, ?> deserialize(JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context);

}

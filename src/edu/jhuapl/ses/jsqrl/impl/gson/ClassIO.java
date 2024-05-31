package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.impl.InstanceGetter;

public class ClassIO implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

  private static final InstanceGetter instanceGetter = InstanceGetter.defaultInstanceGetter();

  @Override
  public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
    String typeId = null;

    try {
      Key<?> key = instanceGetter.getKeyForType(src);
      typeId = key.getId();
    } catch (@SuppressWarnings("unused") Exception e) {

    }

    if (typeId == null) {
      DataTypeInfo info = DataTypeInfo.of(src);
      if (info != DataTypeInfo.NULL) {
        typeId = info.getTypeId();
      }
    }

    if (typeId == null) {
      typeId = src.getName();
    }

    return new JsonPrimitive(typeId);
  }

  @Override
  public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    String typeId = json.getAsJsonPrimitive().getAsString();
    Class<?> result = null;

    try {
      result = instanceGetter.getTypeForKey(Key.of(typeId));
    } catch (@SuppressWarnings("unused") Exception e) {

    }

    if (result == null) {
      try {
        DataTypeInfo info = DataTypeInfo.of(typeId);
        result = info.getTypeClass();
      } catch (@SuppressWarnings("unused") Exception e) {

      }
    }

    if (result == null) {
      try {
        result = Class.forName(typeId);
      } catch (@SuppressWarnings("unused") Exception e) {
        result = Object.class;
      }
    }

    return result;
  }

}

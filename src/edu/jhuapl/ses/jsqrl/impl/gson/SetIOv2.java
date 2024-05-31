package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;

final class SetIOv2 extends IterableIOv2 implements SetIO {

  @Override
  public Set<?> deserialize(JsonElement jsonElement, @SuppressWarnings("unused") Type typeOfT,
      JsonDeserializationContext context) {
    Preconditions.checkArgument(jsonElement.isJsonObject());

    return new LinkedHashSet<>(deserialize(jsonElement.getAsJsonObject(), context));
  }

}

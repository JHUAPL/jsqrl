package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;

final class SortedSetIOv2 extends IterableIOv2 implements SortedSetIO {

  @Override
  public SortedSet<?> deserialize(JsonElement jsonElement, @SuppressWarnings("unused") Type typeOfT,
      JsonDeserializationContext context) {
    Preconditions.checkArgument(jsonElement.isJsonObject());

    return new TreeSet<>(deserialize(jsonElement.getAsJsonObject(), context));
  }

}

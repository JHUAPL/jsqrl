package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;

final class ListIOv2 extends IterableIOv2 implements ListIO {

  ListIOv2() {

  }

  @Override
  public List<?> deserialize(JsonElement jsonElement, @SuppressWarnings("unused") Type typeOfT,
      JsonDeserializationContext context) {
    Preconditions.checkArgument(jsonElement.isJsonObject());

    return deserialize(jsonElement.getAsJsonObject(), context);
  }

}

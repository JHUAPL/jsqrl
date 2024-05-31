package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;

final class MetadataIOv1 implements MetadataIO {
  @Override
  public JsonElement serialize(Metadata src, Type typeOfSrc, JsonSerializationContext context) {
    Preconditions.checkNotNull(src);
    Preconditions.checkArgument(DataTypeInfo.METADATA.getType().equals(typeOfSrc));
    Preconditions.checkNotNull(context);

    JsonArray array = new JsonArray();
    array.add(context.serialize(src.getVersion(), DataTypeInfo.VERSION.getType()));

    JsonObject jsonMetadata = new JsonObject();
    for (Key<?> key : src.getKeys()) {
      Object value = src.get(key);
      jsonMetadata.add(key.getId(),
          context.serialize(GsonElement.of(value), DataTypeInfo.ELEMENT.getType()));
    }

    array.add(jsonMetadata);

    return array;
  }

  @Override
  public Metadata deserialize(JsonElement jsonSrc, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    Preconditions.checkNotNull(jsonSrc);
    Preconditions.checkArgument(jsonSrc.isJsonArray());
    Preconditions.checkArgument(DataTypeInfo.METADATA.getType().equals(typeOfT));
    Preconditions.checkNotNull(context);

    JsonArray jsonArray = jsonSrc.getAsJsonArray();
    JsonElement jsonElement = null;
    Iterator<JsonElement> iterator = jsonArray.iterator();

    if (!iterator.hasNext()) {
      throw new IllegalArgumentException();
    }
    jsonElement = iterator.next();

    Version version = context.deserialize(jsonElement, DataTypeInfo.VERSION.getType());

    final SettableMetadata metadata = SettableMetadata.of(version);
    if (!iterator.hasNext()) {
      throw new IllegalArgumentException();
    }
    jsonElement = iterator.next();
    JsonObject jsonMetadata = jsonElement.getAsJsonObject();
    for (Entry<String, JsonElement> entry : jsonMetadata.entrySet()) {
      Key<Object> key = Key.of(entry.getKey());
      GsonElement element = context.deserialize(entry.getValue(), DataTypeInfo.ELEMENT.getType());
      metadata.put(key, element.getValue());
    }
    return metadata;
  }

}

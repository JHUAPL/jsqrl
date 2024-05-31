package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.StorableAsMetadata;
import edu.jhuapl.ses.jsqrl.impl.InstanceGetter;

final class ProxyIOv2<T> implements ProxyIO<T> {

  @Override
  public JsonElement serialize(Object src, @SuppressWarnings("unused") Type typeOfSrc,
      JsonSerializationContext context) {
    JsonObject object = new JsonObject();

    Key<?> key;
    Metadata metadata;
    if (src instanceof StorableAsMetadata) {
      StorableAsMetadata<?> storable = (StorableAsMetadata<?>) src;

      key = storable.getKey();
      metadata = storable.store();
    } else {
      @SuppressWarnings("unchecked")
      Class<Object> objectType = (Class<Object>) src.getClass();
      InstanceGetter instanceGetter = InstanceGetter.defaultInstanceGetter();

      key = instanceGetter.getKeyForType(objectType);
      metadata = instanceGetter.providesMetadataFromGenericObject(objectType).provide(src);
    }

    object.addProperty("proxiedType", key.getId());
    object.add("proxyMetadata", context.serialize(metadata, DataTypeInfo.METADATA.getType()));

    return object;
  }

  @Override
  public T deserialize(JsonElement json, @SuppressWarnings("unused") Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    JsonObject object = json.getAsJsonObject();
    Key<T> proxyKey = Key.of(object.get("proxiedType").getAsString());
    Metadata objectMetadata =
        context.deserialize(object.get("proxyMetadata"), DataTypeInfo.METADATA.getType());

    return InstanceGetter.defaultInstanceGetter().providesGenericObjectFromMetadata(proxyKey)
        .provide(objectMetadata);
  }

}

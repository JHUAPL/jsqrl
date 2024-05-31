package edu.jhuapl.ses.jsqrl.impl.gson;

import java.lang.reflect.Type;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import edu.jhuapl.ses.jsqrl.api.Version;

final class GsonVersionIO implements JsonSerializer<Version>, JsonDeserializer<Version> {
  private static final String KEY_ID = DataTypeInfo.VERSION.getTypeId();

  public static JsonPrimitive encode(Version version) {
    Preconditions.checkNotNull(version);
    return new JsonPrimitive(version.toString());
  }

  public static Version decode(JsonPrimitive jsonVersion) {
    Preconditions.checkNotNull(jsonVersion);

    return Version.of(jsonVersion.getAsString());
  }

  public static Version decode(JsonObject jsonObject) {
    Preconditions.checkNotNull(jsonObject);

    JsonElement jsonVersion = jsonObject.get(KEY_ID);
    Preconditions.checkArgument(jsonVersion.isJsonPrimitive());

    return decode(jsonVersion.getAsJsonPrimitive());
  }

  @Override
  public JsonElement serialize(Version src, @SuppressWarnings("unused") Type typeOfSrc,
      @SuppressWarnings("unused") JsonSerializationContext context) {
    JsonObject result = new JsonObject();
    result.add(KEY_ID, encode(src));
    return result;
  }

  @Override
  public Version deserialize(JsonElement jsonElement, @SuppressWarnings("unused") Type typeOfT,
      @SuppressWarnings("unused") JsonDeserializationContext context) {
    Preconditions.checkNotNull(jsonElement);
    Preconditions.checkArgument(jsonElement.isJsonObject());

    JsonObject object = jsonElement.getAsJsonObject();

    // Unpack metadata.
    JsonElement keyIdElement = object.get(KEY_ID);
    if (keyIdElement == null || !keyIdElement.isJsonPrimitive()) {
      throw new IllegalArgumentException(
          "Field \"" + KEY_ID + "\" is missing or has wrong type in Json object");
    }

    return decode(keyIdElement.getAsJsonPrimitive());
  }

}

package edu.jhuapl.ses.jsqrl.impl.gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SerializableIO
    implements JsonSerializer<Serializable>, JsonDeserializer<Serializable> {

  public SerializableIO() {
    super();
  }

  @Override
  public JsonElement serialize(Serializable src, Type typeOfSrc, JsonSerializationContext context) {
    String encodedString;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (ObjectOutputStream oos =
          new ObjectOutputStream(/* new GZIPOutputStream( */new GZIPOutputStream(baos))) {
        oos.writeObject(src);
      }
      encodedString = Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (Exception e) {
      encodedString = null;
    }

    return encodedString != null ? new JsonPrimitive(encodedString) : JsonNull.INSTANCE;
  }

  @Override
  public Serializable deserialize(JsonElement jsonElement, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    Preconditions.checkArgument(jsonElement.isJsonPrimitive());

    String encodedString = jsonElement.getAsString();

    byte[] data = Base64.getDecoder().decode(encodedString);

    Serializable serializable;
    try (ObjectInputStream ois =
        new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))) {
      serializable = (Serializable) ois.readObject();
    } catch (Exception e) {
      serializable = null;
    }


    return serializable;
  }

}

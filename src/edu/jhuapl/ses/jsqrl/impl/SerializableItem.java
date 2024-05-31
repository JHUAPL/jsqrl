package edu.jhuapl.ses.jsqrl.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import com.google.common.base.Preconditions;
import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.InstanceGetter;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;

/**
 * Adaptor to serialize/deserialize objects that do not explicitly cooperate with the metadata
 * framework, but that *do* implement {@link Serializable}. This adaptor uses byte array objects and
 * gzipping/gunzipping streams to serialize an object to a string, then saves that string using the
 * metadata framework. This adaptor can also do the reverse and create an object from a string that
 * holds the object's serialized form.
 * <p>
 * The purpose of this class is to make it easier to support hierarchies and complex objects with
 * the metadata framework without writing explicit instance-getting code. The trade-off is that the
 * strings written are long and not human readable, whereas writing instance-getting code for each
 * type allows thec caller to control how objects are written and read.
 * 
 * @author James Peachey
 *
 * @param <S> the type of item, which must extend {@link Serializable}
 */
public final class SerializableItem<S extends Serializable> {

  /**
   * Boilerplate metadata stuff.
   */
  private static final Version MetadataVersion = Version.of(1, 0);
  private static final Key<Class<Serializable>> ItemTypeKey = Key.of("itemType");
  private static final Key<String> EncodedStringKey = Key.of("encodedString");

  static {

    /**
     * Add to the metadata framework the necessary code to handle encoding/decoding the item.
     */
    InstanceGetter.defaultInstanceGetter().register(Key.of("SerializableItem"), //
        metadata -> {
          Class<Serializable> itemType = metadata.get(ItemTypeKey);
          String encodedString = metadata.get(EncodedStringKey);

          Serializable serializable;
          try {
            serializable = decodeFromString(itemType, encodedString);
          } catch (Exception e) {
            serializable = null;
          }

          return new SerializableItem<Serializable>(itemType, serializable);
        }, SerializableItem.class, settings -> {
          SettableMetadata metadata = SettableMetadata.of(MetadataVersion);

          metadata.put(ItemTypeKey, settings.getItemType());
          String encodedString;
          try {
            encodedString = encodeToString(settings.getItem());
          } catch (Exception e) {
            encodedString = null;
          }
          metadata.put(EncodedStringKey, encodedString);

          return metadata;
        });

  }

  /**
   * Empty static method that may be called to ensure static initialization blocks have executed.
   */
  public static void init() {

  }

  /**
   * Decode an item that is encoded in the specified string and cast it to the specified item type.
   * 
   * @param <S> generic type, which must extend {@link Serializable}
   * @param itemType type of item being decoded
   * @param encodedString the string
   * @return the decoded item
   * @throws IOException if the gunzipping/streaming operation(s) throw one
   * @throws ClassNotFoundException if the class of the object type being decoded cannot be found
   */
  public static <S extends Serializable> S decodeFromString(Class<S> itemType, String encodedString)
      throws IOException, ClassNotFoundException {
    Preconditions.checkNotNull(itemType);

    if (encodedString == null) {
      return null;
    }

    byte[] data = Base64.getDecoder().decode(encodedString);

    try (ObjectInputStream ois =
        new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))) {
      return itemType.cast(ois.readObject());
    }
  }

  /**
   * Encode the specified {@link Serializable} item into a string.
   * 
   * @param item the item to encode
   * @return the encoded string
   * @throws IOException if the gzipping/streaming operation(s) throw one
   */
  public static String encodeToString(Serializable item) throws IOException {
    if (item == null || item instanceof String) {
      return (String) item;
    }

    String encodedString = null;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (ObjectOutputStream oos =
          new ObjectOutputStream(/* new GZIPOutputStream( */new GZIPOutputStream(baos))) {
        oos.writeObject(item);
      }
      encodedString = Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    return encodedString;
  }

  private final Class<S> itemType;
  private final S item;

  public SerializableItem(Class<S> itemType, S item) {
    this.itemType = itemType;
    this.item = item;
  }

  /**
   * @return the concrete type of the serializable item
   */
  public Class<S> getItemType() {
    return itemType;
  }

  /**
   * @return the item that can be serialized
   */
  public S getItem() {
    return item;
  }

  @Override
  public int hashCode() {
    return Objects.hash(item, itemType);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SerializableItem)) {
      return false;
    }
    SerializableItem<?> other = (SerializableItem<?>) obj;

    return Objects.equals(item, other.item) && Objects.equals(itemType, other.itemType);
  }

  @Override
  public String toString() {
    return item != null ? item.toString() : null;
  }

}

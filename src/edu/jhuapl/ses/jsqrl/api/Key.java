package edu.jhuapl.ses.jsqrl.api;

import java.util.regex.Pattern;

/**
 * @param <T> type of the object that may be associated with this key, used for compile-time safety
 *        only
 */
public class Key<T> implements Comparable<Key<?>> {

  private static final Pattern NoLeadingWhiteSpace = Pattern.compile("^\\S.*", Pattern.DOTALL);
  private static final Pattern NoTrailingWhiteSpace = Pattern.compile(".*\\S$", Pattern.DOTALL);

  /**
   * Return a key based on the supplied identification string.
   * 
   * @param keyId the identification string of the key to be returned.
   * @return the key
   * 
   * @throws NullPointerException if argument is null
   */
  public static <T> Key<T> of(String keyId) {
    return new Key<>(keyId);
  }

  private final String keyId;

  protected Key(String keyId) {
    checkNotNull(keyId);
    checkArgument(NoLeadingWhiteSpace.matcher(keyId).matches());
    checkArgument(NoTrailingWhiteSpace.matcher(keyId).matches());
    this.keyId = keyId;
  }

  public String getId() {
    return keyId;
  }

  @Override
  public final int compareTo(Key<?> that) {
    if (that == null) {
      return 1;
    }
    return this.getId().compareTo(that.getId());
  }

  @Override
  public final int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + getId().hashCode();
    return result;
  }

  @Override
  public final boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof Key) {
      Key<?> that = (Key<?>) other;
      return this.getId().equals(that.getId());
    }
    return false;
  }

  @Override
  public String toString() {
    return keyId;
  }

  private static void checkNotNull(Object object) {
    if (object == null) {
      throw new NullPointerException();
    }
  }

  private static void checkArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

}

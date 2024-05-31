package edu.jhuapl.ses.jsqrl.api;

import java.util.Collection;

/**
 * Interface representing a heterogenous collection of objects that may be retrieved using keys,
 * similar to {@link java.util.Map}, but with significant differences, mainly in how nulls and
 * missing keys are treated, and in how type safety is (and is not) enforced.
 */
public interface Metadata {
  /**
   * Return the {@link Version} for the content of this collection of metadata (not different
   * versions of the Metadata interface or associated abstractions).
   * 
   * @return the content version object
   */
  Version getVersion();

  /**
   * Get a collection of {@link Key}s contained in this metadata object. Implementors must guarantee
   * this method remains consistent with other methods on the class, as described in the get(Key<?>
   * key) method.
   * 
   * @return the collection of keys
   */
  Collection<Key<?>> getKeys();

  /**
   * Check whether this metadata object has the supplied key. Implementors must guarantee this
   * method remains consistent with other methods on the class, as described in the get(Key<?> key)
   * method.
   * 
   * @param key the key to check
   * @return true if this metadata object has a value associated with the provided key
   * @throws NullPointerException if the provided key parameter is null
   */
  boolean hasKey(Key<?> key);

  /**
   * Retrieve the value associated with the provided key, if that key-value pair is contained in
   * this metadata object. Keys may not be null, but implementors are required to support null
   * values associated with keys.
   * <p>
   * 
   * This method provides compile-time type safety only. Because of type erasure, it is possible to
   * obtain a key that is present in the metadata, but which has the wrong parametrized type. This
   * will lead to a ClassCastException when this method is called. It is the caller's responsibility
   * to avoid and/or mitigate this contingency.
   * <p>
   * 
   * Implementions are required to be self-consistent in the following ways:
   * <p>
   * 
   * 1) For any metadata object, get(Key<?> key) must throw an IllegalArgumentException if and only
   * if hasKey(Key<?> key) would return false for that same key parameter.
   * <p>
   * 
   * 2) For any metadata object, hasKey(Key<?> key) must return false if the key parameter would not
   * be contained in the Collection returned by getKeys(). Implementations may choose whether to
   * allow "private keys", i.e., keys not returned by getKeys, but for which hasKey returns true.
   * <p>
   * 
   * @param key whose associated value to retrieve
   * 
   * @return the value (possibly null) associated with the key
   * 
   * @throws NullPointerException if the provided key parameter is null
   * 
   * @throws IllegalArgumentException if this metadata does not contain a key-value pairing
   *         associated with the key parameter
   * 
   * @throws ClassCastException if the value associated with the key cannot be cast to the type
   *         provided by the key's type parameter
   */
  <V> V get(Key<V> key);

  /**
   * Create a completely independent copy of this metadata object, i.e., a separate object whose
   * methods would return identical results to the current object immediately after this method is
   * called. Implementations are not required to return an object of the same type as the object on
   * which this method is invoked.
   * 
   * @return the copy of the metadata object
   */
  Metadata copy();

}

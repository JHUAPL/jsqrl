package edu.jhuapl.ses.jsqrl.api;

/**
 * Functional interface whose method uses a supplied object of the parametrized type &lt;T&gt; to
 * provide a {@link Metadata} object that encapsulates the identity and/or state of the original
 * object. An implementation of this interface is typically paired with an implementation of
 * {@link ProvidesGenericObjectFromMetadata}, an implementation of this interface may be used to
 * store all information needed to instantiate on object of type &lt;T&gt;.
 * 
 * @param <T> the object type for which Metadata can be provided.
 * @see {@link ProvidesGenericObjectFromMetadata}
 */
public interface ProvidesMetadataFromGenericObject<T> {

  /**
   * Use the supplied object to create a {@link Metadata} object that encapsulates the state of the
   * original object.
   * 
   * @param object the object whose state will be encapsulated in Metadata.
   * @return the Metadata.
   * @see {@link ProvidesGenericObjectFromMetadata}
   */
  Metadata provide(T object);

}

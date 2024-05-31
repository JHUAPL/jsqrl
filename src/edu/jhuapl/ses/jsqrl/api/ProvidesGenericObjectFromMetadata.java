package edu.jhuapl.ses.jsqrl.api;

/**
 * Functional interface whose method uses a supplied {@link Metadata} object to provide an object
 * instance of a particular parametrized type &lt;T&gt. If &lt;T&gt is an instantiable class, this
 * implies that the stored metadata must contain all information necessary to instantiate an object
 * of type &lt;T&gt. For non-instantiable types (i.e. enumerations or other singletons), the
 * metadata object need only contain enough information to identify the object to return.
 * <p>
 * In general if one provides an implementation of this interface, one provides also a complementary
 * implementation of {@link ProvidesMetadataFromGenericObject} interface and registers both of these
 * implementations with an {@InstanceGetter}.
 * 
 * @param <T> the object type that can be provided from suitable Metadata
 * @see {@link ProvidesMetadataFromGenericObject}
 */
public interface ProvidesGenericObjectFromMetadata<T> {

  /**
   * Use the supplied {@link Metadata} to create (or get) an object of the appropriate instance
   * type.
   * 
   * @param metadata the metadata to use to provide the instance
   * @return an instance of the object of the parametrized type T obtained based on the metadata
   */
  T provide(Metadata metadata);

}

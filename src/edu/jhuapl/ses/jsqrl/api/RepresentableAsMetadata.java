package edu.jhuapl.ses.jsqrl.api;

/**
 * Abstraction that is capable of representing (storing) the content or state of one or more objects
 * as {@link Metadata}.
 * 
 * Implementations may provide representations for themselves, or on behalf of other objects.
 * 
 * @see {@link MetadataManager}
 */
public interface RepresentableAsMetadata {
  /**
   * Return a set of {@link Metadata} that represents the content or state of one or more objects.
   * 
   * @return the metadata representing the object of the parameterized type
   */
  Metadata store();

}

package edu.jhuapl.ses.jsqrl.api;

/**
 * Abstraction representing a manager of {@link Metadata}, capable of storing/retrieving the content
 * or state of one or more objects to/from a single Metadata object.
 */
public interface MetadataManager extends RepresentableAsMetadata {
  /**
   * Return a (complete and self-consistent) set of metadata derived from the content or state of
   * one or more objects.
   * 
   * @return destination object in which the metadata are stored
   */
  @Override
  Metadata store();

  /**
   * Retrieve a (complete and self-consistent) set of metadata in the provided source object. The
   * metadata retrieved will typically be used to create or restore the state of one or more
   * objects.
   * 
   * @param source the source metadata object
   */
  void retrieve(Metadata source);

}

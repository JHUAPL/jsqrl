package edu.jhuapl.ses.jsqrl.impl;

import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.base.Preconditions;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.MetadataManager;
import edu.jhuapl.ses.jsqrl.api.RepresentableAsMetadata;
import edu.jhuapl.ses.jsqrl.api.Serializer;
import edu.jhuapl.ses.jsqrl.impl.gson.Serializers;

/**
 * This is a helper implementation designed to wrap another implementation of the
 * {@link MetadataManager} interface.
 */
public final class TrackedMetadataManager implements MetadataManager, RepresentableAsMetadata {
  public static TrackedMetadataManager of(String metadataId) {
    return new TrackedMetadataManager(metadataId, Serializers.getDefault());
  }

  private static final SortedSet<String> MANAGER_IDENTIFIERS = new TreeSet<>();
  private final Key<Metadata> metadataKey;
  private final Serializer serializer;
  private MetadataManager manager;

  /**
   * Construct a new manager that will use the provided serializer. The identifying string provided
   * must be unique within the currently running application. Note that the new manager is not added
   * to the serializer by this constructor. That will be done when another manager is registered
   * with this manager.
   * 
   * @param metadataId string used to idenfity this manager within the serializer
   * @param serializer the serializer
   * @throws IllegalStateException if the serializer already has a manager associated with the
   *         provided string
   * @throws NullPointerException if any argument is null
   */
  private TrackedMetadataManager(String metadataId, Serializer serializer) {
    Preconditions.checkNotNull(metadataId);
    Preconditions.checkNotNull(serializer);
    Preconditions.checkState(!MANAGER_IDENTIFIERS.contains(metadataId),
        "Duplicated manager identifier " + metadataId);
    MANAGER_IDENTIFIERS.add(metadataId);
    this.metadataKey = Key.of(metadataId);
    this.serializer = serializer;
    this.manager = null;
  }

  /**
   * Perform all initializations required prior to serializing/deserializing metadata managed by
   * this manager. Note that all of this implementation's functions as a manager work just fine
   * without calling this method. Moreover, it is safe to call this method multiple times.
   * 
   * A separate initialize method is provided so that source objects may be serialized in the
   * natural order established by the runtime function of the tool, rather than the order in which
   * source objects were instantiated, which may not be the same.
   * 
   * @throws IllegalStateException if a manager was already registered, or if the serializer fails
   *         to register this manager cleanly.
   */
  public void register(MetadataManager manager) {
    Preconditions.checkNotNull(manager);
    Preconditions.checkState(this.manager == null);
    serializer.register(metadataKey, this);
    this.manager = manager;
  }

  public boolean isRegistered() {
    return manager != null;
  }

  public Key<Metadata> getKey() {
    return metadataKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Metadata store() {
    Preconditions.checkState(manager != null);
    return manager.store();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void retrieve(Metadata sourceMetadata) {
    Preconditions.checkNotNull(sourceMetadata);
    Preconditions.checkState(manager != null);

    manager.retrieve(sourceMetadata);
  }

}

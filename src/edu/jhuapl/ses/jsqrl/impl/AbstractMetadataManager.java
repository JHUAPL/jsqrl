package edu.jhuapl.ses.jsqrl.impl;

import edu.jhuapl.ses.jsqrl.api.MetadataManager;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;

public abstract class AbstractMetadataManager implements MetadataManager {

  private final Version version;

  protected AbstractMetadataManager(int major, int minor) {
    this(Version.of(major, minor));
  }

  protected AbstractMetadataManager(Version version) {
    this.version = version;
  }

  protected SettableMetadata createMetadata() {
    return SettableMetadata.of(version);
  }

}

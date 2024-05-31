package edu.jhuapl.ses.jsqrl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.MetadataManager;

public class MetadataManagerCollection {
  public static MetadataManagerCollection of() {
    return new MetadataManagerCollection();
  }

  private final List<Key<? extends Metadata>> keysInOrder;
  private final SortedMap<Key<? extends Metadata>, MetadataManager> managers;

  private MetadataManagerCollection() {
    this.keysInOrder = new ArrayList<>();
    this.managers = new TreeMap<>();
  }

  /**
   * Add a manager to this collection of managers. The manager will be associated with Metadata
   * identified by the supplied key.
   * 
   * @param key the key to Metadata objects this manager will manage
   * @param manager the manager
   * @throws IllegalStateException if there is already a manager associated with the supplied key
   * @throws NullPointerException if any argument is null
   */
  public void add(Key<? extends Metadata> key, MetadataManager manager) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(manager);
    Preconditions.checkState(!managers.containsKey(key));

    keysInOrder.add(key);
    managers.put(key, manager);
  }

  public void remove(Key<? extends Metadata> key) {
    Preconditions.checkNotNull(key);

    keysInOrder.remove(key);
    managers.remove(key);
  }

  public ImmutableList<Key<? extends Metadata>> getKeys() {
    return ImmutableList.copyOf(keysInOrder);
  }

  public MetadataManager getManager(Key<? extends Metadata> key) {
    Preconditions.checkNotNull(key);
    Preconditions.checkArgument(managers.containsKey(key));
    return managers.get(key);
  }

}

package edu.jhuapl.ses.jsqrl.impl.gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.MetadataManager;
import edu.jhuapl.ses.jsqrl.api.RepresentableAsMetadata;
import edu.jhuapl.ses.jsqrl.api.Serializer;
import edu.jhuapl.ses.jsqrl.api.StorableAsMetadata;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.InstanceGetter;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;
import edu.jhuapl.ses.jsqrl.impl.EmptyMetadata;
import edu.jhuapl.ses.jsqrl.impl.MetadataManagerCollection;
import edu.jhuapl.ses.jsqrl.impl.gson.GsonElement.ElementIO;

public class GsonSerializer implements Serializer {

  // Release notes are now in the package-info file.

  // Encapsulation versions.
  // This version supports serializing proxy class metadata using the same format used
  // by standard Metadata (as in the Metadata interface). Instead of the key "Metadata",
  // the name of the type serves as the key in the output file format. Previous versions used a
  // separate proxy encapsulation. For backward compatibility, the proxy is still supported.
  private static final Version SERIALIZER_VERSION_4 = Version.of(4, 0);

  // This version reads and writes syntactically correct JSON format files. Previous versions
  // were flawed in that they stored two consecutive JSON objects at the top level of the file.
  private static final Version SERIALIZER_VERSION_3 = Version.of(3, 0);

  // This version added support for:
  // 1) saving/loading heterogeneous collections and
  // 2) compressed format when a collection of Metadata objects happen to have the same
  // version. In this case the version is stored once for the whole array or collection.
  private static final Version SERIALIZER_VERSION_2 = Version.of(2, 0);

  // Initial version.
  private static final Version SERIALIZER_VERSION_1 = Version.of(1, 0);

  private final MetadataManagerCollection managerCollection;

  public static GsonSerializer of() {
    return new GsonSerializer();
  }

  protected GsonSerializer() {
    this.managerCollection = MetadataManagerCollection.of();
  }

  @Override
  public Version getVersion() {
    return SERIALIZER_VERSION_4;
  }

  @Override
  public void register(Key<? extends Metadata> key, MetadataManager manager) {
    managerCollection.add(key, manager);
  }

  @Override
  public void deregister(Key<? extends Metadata> key) {
    managerCollection.remove(key);
  }

  @Override
  public void load(File file) throws IOException {
    Preconditions.checkNotNull(file);

    Version fileVersion = loadVersion(file);

    if (SERIALIZER_VERSION_3.compareTo(fileVersion) > 0) {
      loadBeforeV3(file);
      return;
    }
    Gson gson = configureGson(fileVersion);

    try (JsonReader reader = gson.newJsonReader(new FileReader(file))) {
      Metadata source = gson.fromJson(reader, DataTypeInfo.METADATA.getType());
      retrieveInSingleThreadContext(source);
    }

  }

  @Override
  public void save(File file) throws IOException {
    Preconditions.checkNotNull(file);

    File dir = file.getParentFile();
    if (!dir.exists()) {
      dir.mkdirs();
    }
    save(file, getVersion());
  }

  private Version loadVersion(File file) throws IOException {
    Preconditions.checkNotNull(file);

    Gson gson = createGsonBuilder().create();
    try (JsonReader jsonReader = gson.newJsonReader(new FileReader(file))) {
      jsonReader.beginObject();

      String name = jsonReader.nextName();
      if (!name.equals(DataTypeInfo.VERSION.getTypeId())) {
        throw new IOException("Invalid Metadata file format");
      }

      String versionString = jsonReader.nextString();

      return Version.of(versionString);
    }

  }

  private void loadBeforeV3(File file) throws IOException {
    Preconditions.checkNotNull(file);

    SettableMetadata source = SettableMetadata.of(Version.of(0, 0));
    Gson versionOnlyGson = configureGsonToReadVersionOnly();
    try (JsonReader reader = versionOnlyGson.newJsonReader(new FileReader(file))) {
      Version fileVersion = null;
      try {
        fileVersion = versionOnlyGson.fromJson(reader, DataTypeInfo.VERSION.getType());
      } catch (Exception e) {
        throw new IOException("Metadata reader version " + getVersion()
            + " cannot read metadata format of file " + file, e);
      }

      Gson gson = configureGson(fileVersion);

      Map<String, Metadata> metadataMap = gson.fromJson(reader, DataTypeInfo.MAP.getType());
      for (Entry<String, Metadata> entry : metadataMap.entrySet()) {
        source.put(Key.of(entry.getKey()), entry.getValue());
      }
    }
    retrieveInSingleThreadContext(source);
  }

  private void save(File file, Version version) throws IOException {
    if (version.equals(SERIALIZER_VERSION_1)) {
      saveV1(file);
      return;
    } else if (version.equals(SERIALIZER_VERSION_2)) {
      saveV2(file);
      return;
    }

    Gson gson = configureGson(version);
    try (FileWriter fileWriter = new FileWriter(file)) {
      try (JsonWriter jsonWriter = gson.newJsonWriter(fileWriter)) {

        SettableMetadata metaMetadata = SettableMetadata.of(version);
        Map<String, Metadata> metadataMap = new LinkedHashMap<>();
        for (Key<? extends Metadata> key : managerCollection.getKeys()) {
          MetadataManager manager = managerCollection.getManager(key);
          Metadata metadata = manager.store();
          if (!EmptyMetadata.instance().equals(metadata)) {
            metaMetadata.put(Key.of(key.getId()), metadata);
            metadataMap.put(key.getId(), metadata);
          }
        }

        gson.toJson(metaMetadata, DataTypeInfo.METADATA.getType(), jsonWriter);

        fileWriter.write('\n');
      }
    }
  }

  private void saveV2(File file) throws IOException {
    Preconditions.checkNotNull(file);
    Gson gson = configureGson(SERIALIZER_VERSION_2);
    try (FileWriter fileWriter = new FileWriter(file)) {
      try (JsonWriter jsonWriter = gson.newJsonWriter(fileWriter)) {
        Map<String, Metadata> metadataMap = new HashMap<>();
        for (Key<? extends Metadata> key : managerCollection.getKeys()) {
          MetadataManager manager = managerCollection.getManager(key);
          Metadata metadata = manager.store();
          if (!EmptyMetadata.instance().equals(metadata)) {
            metadataMap.put(key.getId(), metadata);
          }
        }
        gson.toJson(SERIALIZER_VERSION_2, DataTypeInfo.VERSION.getType(), jsonWriter);
        jsonWriter.flush();
        fileWriter.write('\n');
        gson.toJson(metadataMap, DataTypeInfo.MAP.getType(), jsonWriter);
        jsonWriter.flush();
        fileWriter.write('\n');
      }
    }
  }

  private void saveV1(File file) throws IOException {
    Preconditions.checkNotNull(file);

    Gson gson = configureGson(SERIALIZER_VERSION_1);
    try (FileWriter fileWriter = new FileWriter(file)) {
      try (JsonWriter jsonWriter = gson.newJsonWriter(fileWriter)) {
        Map<String, Metadata> metadataMap = new HashMap<>();
        for (Key<? extends Metadata> key : managerCollection.getKeys()) {
          MetadataManager manager = managerCollection.getManager(key);
          Metadata metadata = manager.store();
          if (!EmptyMetadata.instance().equals(metadata)) {
            metadataMap.put(key.getId(), metadata);
          }
        }
        gson.toJson(SERIALIZER_VERSION_1, DataTypeInfo.VERSION.getType(), jsonWriter);
        jsonWriter.flush();
        fileWriter.write('\n');
        gson.toJson(metadataMap, DataTypeInfo.MAP.getType(), jsonWriter);
        jsonWriter.flush();
        fileWriter.write('\n');
      }
    }
  }

  private void retrieveInSingleThreadContext(Metadata source) {
    for (Key<? extends Metadata> key : managerCollection.getKeys()) {
      if (source.hasKey(key)) {
        Metadata element = source.get(key);
        if (element != null) {
          managerCollection.getManager(key).retrieve(element);
        }
      }
    }
  }

  private static Gson configureGsonToReadVersionOnly() {
    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(DataTypeInfo.VERSION.getType(), new GsonVersionIO());
    return builder.create();
  }

  private static Gson configureGson(Version serializerVersion) {
    if (SERIALIZER_VERSION_1.equals(serializerVersion)) {
      return configureGsonV1();
    }

    return configureGsonBuilder().create();
  }

  private static GsonBuilder createGsonBuilder() {
    GsonBuilder builder = new GsonBuilder();

    builder.serializeNulls();
    builder.setPrettyPrinting();
    builder.serializeSpecialFloatingPointValues();
    builder.disableHtmlEscaping();

    return builder;
  }

  private static GsonBuilder configureGsonBuilder() {
    GsonBuilder builder = createGsonBuilder();

    builder.registerTypeAdapter(DataTypeInfo.SORTED_SET.getType(), new SortedSetIOv2());
    builder.registerTypeAdapter(DataTypeInfo.SET.getType(), new SetIOv2());
    builder.registerTypeAdapter(DataTypeInfo.LIST.getType(), new ListIOv2());
    builder.registerTypeAdapter(DataTypeInfo.ITERABLE.getType(), new ListIOv2());
    builder.registerTypeAdapter(DataTypeInfo.SORTED_MAP.getType(), new SortedMapIOv2());
    builder.registerTypeAdapter(DataTypeInfo.MAP.getType(), new MapIOv2());
    builder.registerTypeAdapter(DataTypeInfo.METADATA.getType(), new MetadataIOv2());
    builder.registerTypeAdapter(DataTypeInfo.VERSION.getType(), new GsonVersionIO());
    builder.registerTypeAdapter(DataTypeInfo.ELEMENT.getType(), new ElementIO());
    builder.registerTypeAdapter(DataTypeInfo.PROXIED_OBJECT.getType(), new ProxyIOv2<>());
    builder.registerTypeAdapter(DataTypeInfo.CLASS.getType(), new ClassIO());
    builder.registerTypeAdapter(DataTypeInfo.SERIALIZABLE.getType(), new SerializableIO());

    return builder;
  }

  private static Gson configureGsonV1() {
    GsonBuilder builder = createGsonBuilder();

    builder.registerTypeAdapter(DataTypeInfo.SORTED_SET.getType(), new SortedSetIOv1());
    builder.registerTypeAdapter(DataTypeInfo.SET.getType(), new SetIOv1());
    builder.registerTypeAdapter(DataTypeInfo.LIST.getType(), new ListIOv1());
    builder.registerTypeAdapter(DataTypeInfo.ITERABLE.getType(), new ListIOv1());
    builder.registerTypeAdapter(DataTypeInfo.SORTED_MAP.getType(), new SortedMapIOv1());
    builder.registerTypeAdapter(DataTypeInfo.MAP.getType(), new MapIOv1());
    builder.registerTypeAdapter(DataTypeInfo.METADATA.getType(), new MetadataIOv1());
    builder.registerTypeAdapter(DataTypeInfo.VERSION.getType(), new GsonVersionIO());
    builder.registerTypeAdapter(DataTypeInfo.ELEMENT.getType(), new ElementIO());
    builder.registerTypeAdapter(DataTypeInfo.PROXIED_OBJECT.getType(), new ProxyIOv1<>());
    builder.registerTypeAdapter(DataTypeInfo.CLASS.getType(), new ClassIO());

    return builder.create();
  }

  /**
   * This enumeration demonstrates how to serialize/deserialize an enumeration by implementing the
   * StorableAsMetadata interface.
   *
   */
  private static final Key<String> NAME = Key.of("Name");
  private static final Key<TestEnum> TEST_ENUM_PROXY_KEY = Key.of("TestEnum");
  private static final Version VERSION = Version.of(1, 0);

  private enum TestEnum implements StorableAsMetadata<TestEnum> {
    OPTION0("Option 0"), OPTION1("Option 1");

    // Need to live with this warning until the register method has been phased out.
    public static void register(InstanceGetter instanceGetter) {
      instanceGetter.register(TEST_ENUM_PROXY_KEY, (metadata) -> {
        return valueOf(metadata.get(NAME));
      });
    }

    private final String text;

    TestEnum(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }

    @Override
    public Key<TestEnum> getKey() {
      return TEST_ENUM_PROXY_KEY;
    }

    @Override
    public Metadata store() {
      return SettableMetadata.of(VERSION).put(NAME, name());
    }
  }

  /**
   * This enumeration demonstrates how to serialize/deserialize an enumeration without implementing
   * StorableAsMetadata.
   */
  private enum TestEnum2 {
    OPTION0, OPTION1;
  }

  private static final Key<TestEnum2> TEST_ENUM2_PROXY_KEY = Key.of("TestEnum2");

  private static void registerTestEnum2(InstanceGetter instanceGetter) {
    instanceGetter.register(TEST_ENUM2_PROXY_KEY, metadata -> {
      return TestEnum2.valueOf(metadata.get(NAME));
    }, TestEnum2.class, object -> {
      return SettableMetadata.of(VERSION).put(NAME, object.name());
    });
  }

  private static class TestManager implements MetadataManager {
    private final SettableMetadata metadata;

    TestManager(SettableMetadata metadata) {
      this.metadata = metadata;
    }

    @Override
    public Metadata store() {
      SettableMetadata destination = SettableMetadata.of(metadata.getVersion());
      for (Key<?> key : metadata.getKeys()) {
        @SuppressWarnings("unchecked")
        Key<Object> newKey = (Key<Object>) key;
        destination.put(newKey, metadata.get(key));
      }

      return destination;
    }

    @Override
    public void retrieve(Metadata source) {
      metadata.clear();
      for (Key<?> key : source.getKeys()) {
        Object value = source.get(key);
        @SuppressWarnings("unchecked")
        Key<Object> newKey = (Key<Object>) key;
        metadata.put(newKey, value);
      }
    }

  }

  // These are all just fodder for test cases, These version numbers have nothing to do with the
  // version of
  // the serializer.
  private static final Version SAMPLE_METADATA_VERSION = Version.of(1, 0);
  private static final Version SAMPLE_SUB_METADATA_VERSION = Version.of(3, 1);
  private static final String SUB_METADATA_ID = "Bennu / V3";
  private static final Key<SettableMetadata> SAMPLE_METADATA_KEY = Key.of("testState");
  private static final Key<SettableMetadata> SAMPLE_SUB_METADATA_KEY = Key.of(SUB_METADATA_ID);
  private static final Key<List<List<String>>> LIST_LIST_STRING_KEY = Key.of("listListString");

  public static void main(String[] args) throws IOException {
    TestEnum.register(InstanceGetter.defaultInstanceGetter());
    registerTestEnum2(InstanceGetter.defaultInstanceGetter());

    // Test failure when trying to serialize something that can't be serialized.
    {
      class UnserializableObject {

      };

      UnserializableObject unserializableObject = new UnserializableObject();

      try {
        SettableMetadata.of(SAMPLE_METADATA_VERSION).put(Key.of("unserializable"),
            unserializableObject);
        System.err.println("Saving an unserializable object did not throw an exception");
      } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
        // This is as it should be.
      }
    }

    // Test V1.
    String testPath = Paths.get(System.getProperty("user.home"), "Downloads").toString();
    {
      SettableMetadata state = createV1SampleMetadata();
      File file = Paths.get(testPath, "MyStateV1.sbmt").toFile();
      testSaveAndReloadState(state, file, SERIALIZER_VERSION_1);
    }

    // Test V2.
    {
      SettableMetadata state = createV2SampleMetadata();
      File file = Paths.get(testPath, "MyStateV2.sbmt").toFile();
      testSaveAndReloadState(state, file, SERIALIZER_VERSION_2);
    }

    // Test V3.
    {
      SettableMetadata state = createV3SampleMetadata();
      File file = Paths.get(testPath, "MyStateV3.sbmt").toFile();
      testSaveAndReloadState(state, file, SERIALIZER_VERSION_3);
    }

    // Test current version.
    {
      SettableMetadata state = createV4SampleMetadata();
      File file = Paths.get(testPath, "MyState.sbmt").toFile();
      state = testSaveAndReloadState(state, file, SERIALIZER_VERSION_4);

      SettableMetadata subState = state.get(SAMPLE_SUB_METADATA_KEY);

      // Test some further unpacking.
      state.get(Key.of("double array"));
      state.get(Key.of("int array"));
      state.get(Key.of("Double array"));
      state.get(Key.of("Integer array"));
      state.get(Key.of("String array"));

      subState.get(Key.of("longNull"));
      if (TestEnum.OPTION1 != subState.get(Key.of("testEnum"))) {
        System.err.println("testEnum value was not option 1");
      }

      state.get(Key.of("stringSet"));

      if (TestEnum2.OPTION1 != subState.get(Key.of("testEnum2"))) {
        System.err
            .println("testEnum2 value was " + subState.get(Key.of("testEnum2")) + ", not option 1");
      }

      // This fails at runtime. If this were a real key templated on Long it would fail at compile
      // time.
      // Float fVal = subState.get(Key.of("long"));

      // This doesn't fail at runtime or compile time but I wish it would:
      // List<List<Integer>> unpackedListList = state.get(Key.of("listListString"));

      // But the following does fail to compile, which is probably good enough.
      // unpackedListList = state.get(listListStringKey);
      // And this fails at runtime, as it should.
      // System.out.println(unpackedListList.get(1).get(1) * 7);

      // It would be OK if this were to work but it doesn't.
      // float fVal = subState.get(Key.of("resolution"));

      // This one is supposed to throw an exception.
      // Short longAsShort = subState.get(Key.of("facets"));
      // System.err.println("long retrieved as a short is " + longAsShort);
    }

  }

  private static SettableMetadata createV1SampleMetadata() {

    SettableMetadata subState = SettableMetadata.of(SAMPLE_SUB_METADATA_VERSION);
    TestEnum testEnumBefore = TestEnum.OPTION1;

    subState.put(Key.of("tab"), "1");
    subState.put(Key.of("facets"), 2000000001L);
    subState.put(Key.of("showBaseMap"), true);
    subState.put(Key.of("resolution"), -5.e64);
    subState.put(Key.of("int"), 20);
    subState.put(Key.of("long"), (long) 20);
    subState.put(Key.of("short"), (short) 20);
    subState.put(Key.of("byte"), (byte) 20);
    subState.put(Key.of("double"), (double) 20);
    subState.put(Key.of("float"), (float) 20);
    subState.put(Key.of("char"), (char) 20);
    subState.put(Key.of("boolean"), false);
    subState.put(Key.of("string"), "a string");
    subState.put(Key.of("htmlString"), "<pre>A literal escaped in html</pre>");
    subState.put(Key.of("stringNull"), null);
    subState.put(Key.of("longNull"), null);
    subState.put(Key.of("testEnum"), testEnumBefore);
    subState.put(Key.of("List<TestEnum>"), ImmutableList.of(TestEnum.OPTION1, TestEnum.OPTION0));

    List<String> stringList = new ArrayList<>();
    stringList.add("String0");
    stringList.add(null);
    stringList.add("String2");
    Key<List<String>> stringListKey = Key.of("stringList");
    subState.put(stringListKey, stringList);

    List<Integer> intList = new ArrayList<>();
    intList.add(0);
    intList.add(null);
    intList.add(2);
    Key<List<Integer>> intListKey = Key.of("intList");
    subState.put(intListKey, intList);

    List<List<String>> listListString = new ArrayList<>();
    listListString.add(null);
    listListString.add(ImmutableList.of("X", "y", "z"));
    listListString.add(stringList);

    final SettableMetadata state = SettableMetadata.of(SAMPLE_METADATA_VERSION);

    state.put(Key.of("Current View"), SUB_METADATA_ID);
    state.put(Key.of("Tab Number"), new Integer(3));
    state.put(Key.of("Current View2"), SUB_METADATA_ID);
    state.put(LIST_LIST_STRING_KEY, listListString);
    state.put(Key.of("stringSet"), ImmutableSortedSet.of("liver", "spleen", "aardvark"));
    state.put(Key.of("Metadata"), "a string that happens to have the key \"Metadata\"");
    // The following type has built-in support through DataTypeInfo.
    state.put(Key.of("Double.class"), Double.class);
    // The following type is registered with InstanceGetter. This should work using the registerd
    // name.
    state.put(Key.of("TestEnum2.class"), TestEnum2.class);
    // The following type is not registered with InstanceGetter. This should also work using the
    // full class name.
    state.put(Key.of("Metadata.class"), RepresentableAsMetadata.class);

    Map<Byte, Short> byteShortMap = new HashMap<>();
    byteShortMap.put((byte) 1, null);
    byteShortMap.put(null, (short) 12);
    byteShortMap.put((byte) 11, (short) 23);
    byteShortMap.put((byte) 10, (short) 17);
    Key<Map<Byte, Short>> byteShortMapKey = Key.of("byteShortMap");
    state.put(byteShortMapKey, byteShortMap);

    SortedMap<Byte, Short> byteSortedMap = new TreeMap<>();
    byteSortedMap.put((byte) 1, null);
    byteSortedMap.put((byte) 11, (short) 23);
    byteSortedMap.put((byte) 10, (short) 17);
    Key<SortedMap<Byte, Short>> byteSortedMapKey = Key.of("byteSortedMap");
    state.put(byteSortedMapKey, byteSortedMap);

    SortedMap<String, SortedMap<Integer, String>> homogeneousMapMap = new TreeMap<>();
    homogeneousMapMap.put("nullMap", null);
    SortedMap<Integer, String> intMap = new TreeMap<>();
    intMap.put(0, "zero");
    intMap.put(1, "one");
    homogeneousMapMap.put("map0", intMap);
    homogeneousMapMap.put("map1", intMap);
    state.put(Key.of("homogeneous map of map"), homogeneousMapMap);
    homogeneousMapMap = state.get(Key.of("homogeneous map of map"));

    SortedSet<String> treeSet = new TreeSet<>();
    treeSet.add("string0");
    state.put(Key.of("treeSet"), treeSet);
    treeSet = state.get(Key.of("treeSet"));

    state.put(Key.of("single element map"), ImmutableMap.of("key0", "value0"));
    state.put(Key.of("single element list"), ImmutableList.of(7));

    state.put(Key.of("double array"), new double[] {3., 4., 5.});
    state.put(Key.of("int array"), new int[] {6, 7, 8});
    state.put(Key.of("Double array"), new Double[] {9., 10., 11.});
    state.put(Key.of("Integer array"), new Integer[] {12, 13, 14});
    state.put(Key.of("String array"), new String[] {"a", "b", "c"});

    state.put(SAMPLE_SUB_METADATA_KEY, subState);

    return state;
  }

  private static SettableMetadata createV2SampleMetadata() {
    // Start with the V1 metadata set.
    SettableMetadata state = createV1SampleMetadata();
    SettableMetadata subState = state.get(SAMPLE_SUB_METADATA_KEY);

    // Add tests for things supported in V2 and above.
    SortedMap<String, SortedMap<Integer, Object>> mapHeterogeneousMap = new TreeMap<>();
    mapHeterogeneousMap.put("nullMap", null);
    SortedMap<Integer, Object> nestedMap = new TreeMap<>();
    nestedMap.put(0, "zero");
    nestedMap.put(1, 1.);
    mapHeterogeneousMap.put("map0", nestedMap);
    mapHeterogeneousMap.put("map1", nestedMap);
    state.put(Key.of("map of heterogenous maps"), mapHeterogeneousMap);

    List<Integer> intList = subState.get(Key.of("intList"));

    SortedMap<String, Object> mapOfCollections = new TreeMap<>();
    mapOfCollections.put("nullMap", null);
    mapOfCollections.put("map", nestedMap);
    mapOfCollections.put("intList", intList);
    state.put(Key.of("map containing nested objects"), mapOfCollections);

    List<Object> objectList = new ArrayList<Object>();
    objectList.add(null);
    objectList.add("a");
    objectList.add(1);
    objectList.add(2.);
    objectList.add(false);
    objectList.add(SettableMetadata.of(Version.of(1, 3)));
    objectList.add(SettableMetadata.of(Version.of(1, 3)).put(Key.of("key0"), "value0"));
    subState.put(Key.of("objectList"), objectList);

    Set<Object> objectSet = new HashSet<>();
    objectSet.add("a");
    objectSet.add(null);
    objectSet.add(1);
    objectSet.add(2.);
    objectSet.add(true);
    objectSet.add(SettableMetadata.of(Version.of(1, 3)).put(Key.of("key0"), "value0"));
    subState.put(Key.of("objectSet"), objectSet);

    Map<Object, Object> objectMap = new HashMap<>();
    objectMap.put("key0", "repeatedValue");
    objectMap.put("key1", "repeatedValue");
    objectMap.put(null, "valueForNullKey");
    objectMap.put("key2", "distinctValue");
    subState.put(Key.of("homogeneousObjectMap"), objectMap);
    try {
      // Now test exception for heterogenous keys.
      objectMap.put(7, "stringValue");
      subState.put(Key.of("invalidObjectMap"), objectMap);
    } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
      // This is as it should be.
    }

    objectMap = new HashMap<>();
    objectMap.put("key0", "repeatedValue");
    objectMap.put("key1", "repeatedValue");
    objectMap.put(null, 0);
    objectMap.put("key2", 4.2);
    objectMap.put("meta0", SettableMetadata.of(Version.of(2, 2)).put(Key.of("key0"), "value0"));
    objectMap.put("meta1", SettableMetadata.of(Version.of(2, 2)).put(Key.of("key1"), "value1"));
    objectMap.put("meta2", SettableMetadata.of(Version.of(2, 3)).put(Key.of("key1"), "value1"));
    subState.put(Key.of("heterogeneousObjectMap"), objectMap);

    objectMap = new HashMap<>();
    objectMap.put("key0", "repeatedValue");
    objectMap.put("key1", "repeatedValue");
    objectMap.put(null, 0);
    objectMap.put("key2", 4.2);
    objectMap.put("meta0", SettableMetadata.of(Version.of(2, 2)).put(Key.of("key0"), "value0"));
    objectMap.put("meta1", SettableMetadata.of(Version.of(2, 2)).put(Key.of("key0"), "value0")
        .put(Key.of("key1"), "value1"));
    subState.put(Key.of("heterogeneousObjectHomogeneousMetadataMap"), objectMap);

    objectMap = new HashMap<>();
    objectMap.put("meta0", SettableMetadata.of(Version.of(2, 2)).put(Key.of("key0"), "value0"));
    objectMap.put("meta1", SettableMetadata.of(Version.of(2, 2)).put(Key.of("key1"), "value1"));
    subState.put(Key.of("homogeneousMetadataMap"), objectMap);

    objectMap = new HashMap<>();
    objectMap.put("meta0", SettableMetadata.of(Version.of(2, 2)).put(Key.of("key0"), "value0"));
    objectMap.put("meta1", SettableMetadata.of(Version.of(2, 3)).put(Key.of("key1"), "value1"));
    subState.put(Key.of("heterogeneousMetadataMap"), objectMap);

    List<Metadata> metadataList = new ArrayList<>();
    metadataList.add(SettableMetadata.of(Version.of(0, 1)).put(Key.of("key0"), "version 0.1"));
    metadataList.add(SettableMetadata.of(Version.of(0, 1)).put(Key.of("key0"), "version 0.1")
        .put(Key.of("key1"), "version 0.1"));
    subState.put(Key.of("metadataList"), metadataList);
    metadataList.add(SettableMetadata.of(Version.of(0, 2)).put(Key.of("key2"), "version 0.2"));
    subState.put(Key.of("heterogeneousMetadataList"), metadataList);

    Set<Metadata> metadataSet = new HashSet<>();
    metadataSet.add(SettableMetadata.of(Version.of(0, 1)).put(Key.of("key0"), "version 0.1"));
    metadataSet.add(SettableMetadata.of(Version.of(0, 1)).put(Key.of("key0"), "version 0.1")
        .put(Key.of("key1"), "version 0.1"));
    subState.put(Key.of("metadataSet"), metadataSet);
    metadataSet.add(SettableMetadata.of(Version.of(0, 2)).put(Key.of("key2"), "version 0.2"));
    subState.put(Key.of("heterogeneousMetadataSet"), metadataSet);

    SettableMetadata homogeneousMetaMetadata = SettableMetadata.of(Version.of(2, 0));
    homogeneousMetaMetadata.put(Key.of("md0"),
        SettableMetadata.of(Version.of(2, 0)).put(Key.of("key0"), "value0"));
    homogeneousMetaMetadata.put(Key.of("md1"),
        SettableMetadata.of(Version.of(2, 0)).put(Key.of("key0"), 100).put(Key.of("key1"), 101.));
    // subState.put(Key.of("homogeneousMetaMetadata"), homogeneousMetaMetadata);

    return state;
  }

  private static SettableMetadata createV3SampleMetadata() {
    // V3 was a change to the low level format; no difference in the types of objects that could be
    // supported.
    return createV2SampleMetadata();
  }

  private static SettableMetadata createV4SampleMetadata() {
    SettableMetadata state = createV3SampleMetadata();

    SettableMetadata subState = state.get(SAMPLE_SUB_METADATA_KEY);

    // Starting in V4, possible to serialize proxy objects directly as metadata using newer
    // capabilities of the InstanceGetter class.
    TestEnum2 testEnum2Before = TestEnum2.OPTION1;
    subState.put(Key.of("testEnum2"), testEnum2Before);
    subState.put(Key.of("testEnum2List"), ImmutableList.of(TestEnum2.OPTION1, TestEnum2.OPTION0));
    subState.put(Key.of("testEnum2Map"),
        ImmutableMap.of("enumA", TestEnum2.OPTION1, "enumB", TestEnum2.OPTION0));

    return state;
  }

  private static SettableMetadata testSaveAndReloadState(SettableMetadata originalState, File file,
      Version saveVersion) throws IOException {
    SettableMetadata originalSubState = originalState.get(SAMPLE_SUB_METADATA_KEY);

    {
      TestManager stateManager = new TestManager(originalState);
      TestManager subStateManager = new TestManager(originalSubState);

      GsonSerializer serializer = GsonSerializer.of();
      serializer.register(SAMPLE_METADATA_KEY, stateManager);
      serializer.register(SAMPLE_SUB_METADATA_KEY, subStateManager);

      serializer.save(file, saveVersion);
    }

    {
      // Blank states with the same keys as the originals.
      SettableMetadata reloadedState = SettableMetadata.of(SAMPLE_METADATA_VERSION);
      SettableMetadata reloadedSubState = SettableMetadata.of(SAMPLE_SUB_METADATA_VERSION);

      TestManager stateManager = new TestManager(reloadedState);
      TestManager subStateManager = new TestManager(reloadedSubState);

      GsonSerializer serializer = GsonSerializer.of();
      // Go in reverse order here just to test that the order doesn't need to be the same.
      serializer.register(SAMPLE_SUB_METADATA_KEY, subStateManager);
      serializer.register(SAMPLE_METADATA_KEY, stateManager);

      serializer.load(file);

      System.out.println("Reloaded " + saveVersion + " state was"
          + (originalState.equals(reloadedState) ? " " : " ******* NOT ******* ")
          + "found equal to original");

      System.out.println("Reloaded " + saveVersion + " sampleState was"
          + (originalSubState.equals(reloadedSubState) ? " " : " ******* NOT ******* ")
          + "found equal to original");

      return reloadedState;
    }

  }

}

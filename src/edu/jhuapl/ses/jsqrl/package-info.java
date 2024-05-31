/**
 * Core implementation of the metadata API. This includes factories for implementations of the
 * primary interfaces (Metadata etc.) as well as utility classes and abstract base implementations.
 * <p>
 * Release Notes
 * <p>
 * Package Version 4.4.1, 2020-03-11
 * <p>
 * 1. Fixed a bug which caused SortedMap and SortedSet implementations to be serialized as normal
 * Maps and Sets, respectively. This led to spurious exceptions when they were deserialized.
 * <p>
 * Package Version 4.4, 2020-03-05
 * <p>
 * 1. Support storing/retrieving Class objects.
 * <p>
 * 2. Use LinkedHashSet and LinkedHashMap to do a better job of preserving order.
 * <p>
 * Package Version 4.3, 2019-11-15
 * <p>
 * Bug fix: if InstanceGetter had a custom serializer/deserializer for a type that implemented Map,
 * Set or List, two bugs would cause an object of that type to be serialized as a Map, Set or List,
 * bypassing the custom serializer.
 * <p>
 * Package Version 4.2, 2019-10-02
 * <p>
 * Bug fix: when serializing Iterables and Maps, under some circumstances, objects that should have
 * been recognized as having different types were being treated as if they were stored the same way.
 * This resulted in errors when trying to deserialize metadata files that had been written by the
 * buggy code.
 * <p>
 * Package Version 4.1, 2019-04-10
 * <p>
 * InstanceGetter: Add support for matching abstract types and interfaces when encoding/decoding
 * objects as proxy metadata.
 * <p>
 * Package Version 4.0, 2019-03-25
 * <p>
 * 1. Accept, serialize and deserialize Class<?> objects.
 * <p>
 * 2. Create directories as needed to serialize the file in question.
 * <p>
 */
package edu.jhuapl.ses.jsqrl;

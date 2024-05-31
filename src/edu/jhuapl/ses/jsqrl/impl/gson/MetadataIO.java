package edu.jhuapl.ses.jsqrl.impl.gson;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import edu.jhuapl.ses.jsqrl.api.Metadata;

interface MetadataIO extends JsonSerializer<Metadata>, JsonDeserializer<Metadata> {

}

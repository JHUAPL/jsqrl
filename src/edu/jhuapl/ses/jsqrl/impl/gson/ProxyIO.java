package edu.jhuapl.ses.jsqrl.impl.gson;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

interface ProxyIO<T> extends JsonSerializer<Object>, JsonDeserializer<T> {

}

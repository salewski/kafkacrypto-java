package org.kafkacrypto;

import org.kafkacrypto.CryptoStore;
import org.kafkacrypto.msgs.ByteString;
import org.kafkacrypto.exceptions.KafkaCryptoException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

public class KafkaCryptoStore extends CryptoStore
{
  public KafkaCryptoStore(String file) throws KafkaCryptoException
  {
    super(file);
  }

  public Properties get_kafka_config(String use, String... extra)
  {
    Properties rv = new Properties();
    String extra0 = null;
    Map<ByteString,ByteString> base_config = this.load_section("kafka",false);
    List<String> extras = new ArrayList<String>();
    if (use != null)
      extras.add("kafka-" + use);
    for (String e : extra) {
      if (extra0 == null)
        extra0 = e;
      extras.add("kafka-" + extra);
      if (use != null)
        extras.add("kafka-" + extra + "-" + use);
    }
    for (String e : extras) {
      Map<ByteString,ByteString> add_config = this.load_section(e,false);
      for (ByteString bs : add_config.keySet()) {
        ByteString bsv = add_config.get(bs);
        if (bsv == null || bsv.length() < 1 && base_config.containsKey(bs))
          base_config.remove(bs);
        else
          base_config.put(bs, bsv);
      }
    }
    if (!base_config.containsKey(new ByteString("group_id"))) {
      if (extra0 != null && extra0.equals("crypto"))
        base_config.put(new ByteString("group_id"), new ByteString(this.get_nodeID()+".kafkacrypto"));
      else
        base_config.put(new ByteString("group_id"), new ByteString(this.get_nodeID()));
    }
    if (!use.equals("consumer"))
      base_config.remove(new ByteString("group_id"));

    if (extra0 != null && extra0.equals("crypto")) {
      if (use.equals("consumer")) {
        if (!base_config.containsKey(new ByteString("key_deserializer")))
          base_config.put(new ByteString("key_deserializer"), new ByteString("org.apache.kafka.common.serialization.ByteArrayDeserializer"));
        if (!base_config.containsKey(new ByteString("value_deserializer")))
          base_config.put(new ByteString("value_deserializer"), new ByteString("org.apache.kafka.common.serialization.ByteArrayDeserializer"));
      }
      if (use.equals("producer")) {
        if (!base_config.containsKey(new ByteString("key_serializer")))
          base_config.put(new ByteString("key_serializer"), new ByteString("org.apache.kafka.common.serialization.ByteArraySerializer"));
        if (!base_config.containsKey(new ByteString("value_serializer")))
          base_config.put(new ByteString("value_serializer"), new ByteString("org.apache.kafka.common.serialization.ByteArraySerializer"));
      }
    }
    for (ByteString bs : base_config.keySet()) {
      String key = bs.toString().replace('_','.');
      // more filtering?
      rv.setProperty(key, base_config.get(bs).toString());
    }
    return rv;
  }
}
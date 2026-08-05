package sd2526.trab.impl.kafka;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaSubscriber {

    static public KafkaSubscriber createSubscriber(String addr, List<String> topics) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, addr);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaSubscriber(new KafkaConsumer<>(props), topics);
    }

    private static final long POLL_TIMEOUT = 1L;

    final KafkaConsumer<String, String> consumer;

    public KafkaSubscriber(KafkaConsumer<String, String> consumer, List<String> topics) {
        this.consumer = consumer;
        this.consumer.subscribe(topics);
    }

    public interface SubscriberListener {
        void onReceive(String topic, String key, String value);
    }

    public void consume(SubscriberListener listener) {
        for (;;) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(POLL_TIMEOUT));
            for (Iterator<ConsumerRecord<String, String>> it = records.iterator(); it.hasNext();) {
                ConsumerRecord<String, String> r = it.next();
                listener.onReceive(r.topic(), r.key(), r.value());
            }
        }
    }

    public void start(RecordProcessor recordProcessor) {
        new Thread(() -> {
            for (;;) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(POLL_TIMEOUT));
                for (Iterator<ConsumerRecord<String, String>> it = records.iterator(); it.hasNext();) {
                    recordProcessor.onReceive(it.next());
                }
            }
        }).start();
    }
}
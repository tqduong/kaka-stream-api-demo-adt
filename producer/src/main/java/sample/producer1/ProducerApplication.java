package sample.producer1;

import com.example.DewpointSensor;
import com.example.TemperatureSensor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

@SpringBootApplication
@RestController
public class ProducerApplication {

    BlockingQueue<Message<TemperatureSensor>> temperatureSensorBlockingQueue = new LinkedBlockingQueue<Message<TemperatureSensor>>();
    BlockingQueue<Message<DewpointSensor>> dewpointSensorBlockingQueue = new LinkedBlockingQueue<Message<DewpointSensor>>();
    private final Random random = new Random();

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }

    @RequestMapping(value = "/publishTemperature", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> publishTemperature(@RequestBody TemperatureSensor data) {
        data.setId(UUID.randomUUID().toString());
        Message<TemperatureSensor> message = MessageBuilder.withPayload(data).setHeader(KafkaHeaders.MESSAGE_KEY, data.getLocation()).build();

        temperatureSensorBlockingQueue.offer(message);
        return new ResponseEntity<String>(data.toString(), HttpStatus.OK);
    }


    @RequestMapping(value = "/publishDewpoint", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> publishHumidity(@RequestBody DewpointSensor data) {
        data.setId(UUID.randomUUID().toString());

        Message<DewpointSensor> message = MessageBuilder.withPayload(data).setHeader(KafkaHeaders.MESSAGE_KEY, data.getLocation()).build();

        dewpointSensorBlockingQueue.offer(message);
        return new ResponseEntity<String>(data.toString(), HttpStatus.OK);
    }


    @Bean
    public Supplier<Message<TemperatureSensor>> temperatureSensorSupplier() {
        return () -> temperatureSensorBlockingQueue.poll();
    }


    @Bean
    public Supplier<Message<DewpointSensor>> dewpointSensorSupplier() {
        return () -> dewpointSensorBlockingQueue.poll();
    }


}

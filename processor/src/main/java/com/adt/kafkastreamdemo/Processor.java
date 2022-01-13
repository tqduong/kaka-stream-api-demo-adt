package com.adt.kafkastreamdemo;


import com.example.DewpointSensor;
import com.example.RelativeHumidity;
import com.example.TemperatureSensor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Configuration
public class Processor {


    @Bean
    public BiFunction<KStream<String, TemperatureSensor>, KStream<String, DewpointSensor>, KStream<String, RelativeHumidity>> relativeHumdityProcessor() {

        return (temperatureSensorKStream, dewpointSensorKStream) -> temperatureSensorKStream.join(dewpointSensorKStream,
                (temperatureSensor, dewpointSensor) -> new RelativeHumidity("123", dewpointSensor.getLocation(),
                        calRelativeHumidity(dewpointSensor.getDewpoint(), temperatureSensor.getTemperature())),
                JoinWindows.of(Duration.ofSeconds(30)));
    }



    @Bean
    public Consumer<KStream<String, RelativeHumidity>> rhConsumer() {
        return kstream -> kstream.foreach((key, value) -> {
            System.out.println(String.format("RelativeHumidity: => key: [%s] value: [%s]", key, value));
        });
    }


    @Bean
    public Consumer<KStream<String, RelativeHumidity>> comfortConsumer() {
        return kstream -> kstream.map(
                (key, value) -> {

                    String comfortLevel = "COMFORT";
                    Float rh = value.getRelativeHumidity();

                    if( rh < 20){
                        comfortLevel = "DRY";
                    }else if (rh > 60 ){

                        comfortLevel = "WET";
                    }

                    KeyValue<String, RelativeHumidity> kv = KeyValue.pair( comfortLevel , value);

                    return  kv;


                }).groupByKey().count().toStream().peek( (k, v) ->  System.out.println(String.format("Count: => key: [%s] value: [%s]", k, v)));
    }



    float calRelativeHumidity(double dewpoint, double temperature) {
        double temperatureCelsius = (temperature - 32) / (9.0 / 5.0);
        double dewpointCelsius = (dewpoint - 32) / (9.0 / 5.0);

        double relativeHumidityOne = 100 * (Math.exp((17.625 * dewpointCelsius) / (243.04 + dewpointCelsius))
                / Math.exp((17.625 * temperatureCelsius) / (243.04 + temperatureCelsius)));
        return (float) relativeHumidityOne;
    }

}

package com.umbc.mc.assignment2;

public class SensorData {
    float value;
    Long timestamp;
    SensorData(float value, Long timestamp){
        this.value = value;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "value=" + value +
                ", timestamp=" + timestamp +
                '}';
    }
}

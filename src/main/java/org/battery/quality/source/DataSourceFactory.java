package org.battery.quality.source;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.battery.model.Gb32960Data;

import java.io.Serializable;

public interface DataSourceFactory extends Serializable {
    DataStream<Gb32960Data> createSource(StreamExecutionEnvironment env);
} 
package com.rookiex.day02.transformations;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.sql.Connection;
import java.sql.DriverManager;

public class MapDemo4 {

    public static void main(String[] args) throws Exception{

        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        //整个job的并行度
        int parallelism = env.getParallelism();
        System.out.println("当前job的执行环境默认的并行度为：" + parallelism);

        //DataStreamSource是DataStream的子类
        DataStreamSource<String> words = env.socketTextStream("localhost", 8888);

        SingleOutputStreamOperator<String> wordAndOne = words.map(new RichMapFunction<String, String>() {

            private Connection connection;

            @Override
            public void open(Configuration parameters) throws Exception {
                //查询数据库
                //创建数据流连接
                connection = DriverManager.getConnection("", "", "");
                //super.open(parameters);
            }

            @Override
            public String map(String s) throws Exception {
                //getRuntimeContext()
                //使用connection窗PrepareStatement
                return null;
            }

            @Override
            public void close() throws Exception {
                //super.close();
                connection.close();
            }
        });

        wordAndOne.print();

        //启动并执行
        env.execute();





    }

}

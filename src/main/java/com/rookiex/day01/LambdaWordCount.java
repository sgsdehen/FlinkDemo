package com.rookiex.day01;


import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * @Author RookieX
 * @Date 2021/8/18 1:55 下午
 * @Description:
 */
public class LambdaWordCount {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> dataStreamSource = executionEnvironment.socketTextStream("localhost", 8899);

        //将数据压平和 1 合并在一起

        //如果输入类型和输出类型不一致, 使用 lambda 表达式就要使用 return 指定类型
        //如果输入一行, 使用 collector 输出多行, 使用 lambda 表达式就要使用 return 指定类型
        SingleOutputStreamOperator<Tuple2<String, Integer>> wordAndOne = dataStreamSource.flatMap((String line, Collector<Tuple2<String, Integer>> out) -> {
            String[] words = line.split(" ");
            for (String word : words) {
                out.collect(Tuple2.of(word, 1));
            }
        }).returns(Types.TUPLE(Types.STRING, Types.INT));
        KeyedStream<Tuple2<String, Integer>, String> keyBy = wordAndOne.keyBy(tp -> tp.f0);
        SingleOutputStreamOperator<Tuple2<String, Integer>> sum = keyBy.sum("f1");
        //输出
        sum.print();
        //启动
        executionEnvironment.execute("WorkCount3");
    }
}

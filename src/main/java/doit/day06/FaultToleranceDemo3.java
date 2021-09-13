package doit.day06;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 不开启checkpoint，并且希望Flink可以容错（我自己编程，将中间结果保存起来）
 * 不使用状态，而是在subtask中定义一个HashMap<String, Integer>
 */
public class FaultToleranceDemo3 {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        //设置规定次数的重启策略
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 5000));

        DataStreamSource<String> lines = env.socketTextStream("localhost", 8888);

        SingleOutputStreamOperator<String> words = lines.flatMap(new FlatMapFunction<String, String>() {
            @Override
            public void flatMap(String line, Collector<String> collector) throws Exception {
                for (String word : line.split(" ")) {
                    if("error".equals(word)) {
                       throw new RuntimeException("有问题数据，出异常了！");
                    }
                    collector.collect(word);
                }
            }
        });

        ;
        SingleOutputStreamOperator<Tuple2<String, Integer>> wordAndOne = words.map(new MapFunction<String, Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> map(String word) throws Exception {
                return Tuple2.of(word, 1);
            }
        });

        KeyedStream<Tuple2<String, Integer>, String> keyed = wordAndOne.keyBy(t -> t.f0);

        //对keyby之后的DataStream进行操作
        SingleOutputStreamOperator<Tuple2<String, Integer>> summed = keyed.map(new RichMapFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {

            private HashMap<String, Integer> counter;

            private Integer indexOfThisSubtask;

            @Override
            public void open(Configuration parameters) throws Exception {
                int indexOfThisSubtask = getRuntimeContext().getIndexOfThisSubtask();
                String path = "/Users/xing/Desktop/chk/" + indexOfThisSubtask + ".txt";
                File file = new File(path);
                if (file.exists()) {
                    //从文件中读取数据
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                    //恢复历史数据
                    counter = (HashMap<String, Integer>) ois.readObject();
                    ois.close();
                } else {
                    file.createNewFile();
                    counter = new HashMap<>();
                }
                //启动一个定时器
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                Thread.sleep(10000);
                                //将数据持久化
                                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
                                oos.writeObject(counter);
                                oos.flush();
                                oos.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }

            @Override
            public Tuple2<String, Integer> map(Tuple2<String, Integer> tp) throws Exception {
                String key = tp.f0;
                Integer current = tp.f1;
                Integer history = counter.get(key);
                if (history == null) {
                    history = 0;
                }
                int sum = history + current;
                //更新(保存到Map中)
                counter.put(key, sum);
                tp.f1 = sum;
                //返回输出
                return tp;
            }
        });

        summed.print();

        env.execute();

    }
}

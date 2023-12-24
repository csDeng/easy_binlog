package com.example.easy_binlog;

import com.gitee.Jmysy.binlog4j.core.BinlogClient;
import com.gitee.Jmysy.binlog4j.core.BinlogClientConfig;
import com.gitee.Jmysy.binlog4j.core.BinlogEvent;
import com.gitee.Jmysy.binlog4j.core.IBinlogEventHandler;
import com.gitee.Jmysy.binlog4j.springboot.starter.annotation.BinlogSubscriber;
import io.redisearch.Document;
import io.redisearch.Query;
import io.redisearch.Schema;
import io.redisearch.SearchResult;
import io.redisearch.client.Client;
import io.redisearch.client.AddOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;


@SpringBootApplication
@Slf4j
@RestController
public class EasyBinlogApplication {

    private static final JedisPool  jedisPool = new JedisPool("localhost", 6379);

    @GetMapping("/search")
    public String search(@RequestParam("key") String key) {
        System.out.println("start");
        final Client client = new Client("user", "localhost", 6379);
        Query q = new Query(key) // 设置查询条件
            .setLanguage("chinese") // 设置为中文编码
            .limit(0,5);
        // 返回查询结果
        SearchResult res = client.search(q);
        // 输出查询结果
        System.out.println(res.docs);
        return res.docs.toString();
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(EasyBinlogApplication.class);

    }

    @BinlogSubscriber(clientName = "master")
    static class UserEventHandler implements IBinlogEventHandler<User> {
        @Override
        public void onInsert(BinlogEvent<User> binlogEvent) {
            log.info("插入数据:{}", binlogEvent);
            User data = binlogEvent.getData();
            log.info("data={}", data);
            try (Jedis jedis = jedisPool.getResource()){
                HashMap<String, String> map = new HashMap<>();
                map.put("name", data.getName());
                map.put("pwd", data.getPwd());
                Long l = jedis.hset("user:" + data.getId(), map);
                log.info("同步了{}个记录",l);
            }
        }

        @Override
        public void onUpdate(BinlogEvent<User> binlogEvent) {
            log.info("修改数据:{}", binlogEvent);
            User originalData = binlogEvent.getOriginalData();
            log.info("origin data={}", originalData);
            log.info("data={}", binlogEvent.getData());
        }

        @Override
        public void onDelete(BinlogEvent<User> binlogEvent) {
            log.info("删除数据:{}", binlogEvent.toString());
            User originalData = binlogEvent.getOriginalData();
            log.info("origin data={}", originalData);
            User data = binlogEvent.getData();
            log.info("data={}", data);
        }
    }

    public void createIndex() {
        // 连接 Redis 服务器和指定索引
        Client client = new Client("user", "127.0.0.1", 6379);
        // 定义索引
        Schema schema = new Schema().addTextField("title",
            5.0).addTextField("desc", 1.0);
        // 删除索引
        client.dropIndex();
//         创建索引
        client.createIndex(schema, Client.IndexOptions.Default());
//         设置中文编码
        AddOptions addOptions = new AddOptions();
        addOptions.setLanguage("chinese");
        // 添加数据
        Document document = new Document("doc1");
        document.set("title", "天气预报");
        document.set("desc", "今天的天气很好，是个阳光明媚的大晴天，有蓝蓝的天空和白白的云朵。");
        // 向索引中添加文档
        client.addDocument(document,addOptions);
        // 查询
        Query q = new Query("天气") // 设置查询条件
            .setLanguage("chinese") // 设置为中文编码
            .limit(0,5);
        // 返回查询结果
        SearchResult res = client.search(q);
        // 输出查询结果
        System.out.println(res.docs);
    }



}

package com.elasticsearch.ais.restClient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.elasticsearch.ais.config.Config;
import com.elasticsearch.ais.config.EsClient;
import com.elasticsearch.ais.utils.StringUtils;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/*
 *@author:lihang
 *@email:631533483@qq.com
 */
@EnableConfigurationProperties(Config.class)
public class RestPerformIpml implements  RestPerform {


    private Logger log = LoggerFactory.getLogger(RestPerformIpml.class);
    private EsClient esClient;
    private RestClient restClient;

    public RestPerformIpml() {
    }

    public RestPerformIpml(EsClient esClient) {
        this.esClient = esClient;
        this.restClient = esClient.getRestClient();
    }

    @Override
    public String executeString(String method, String endpoint, String query) throws IOException {
        Request request = new Request(method, endpoint);
        request.setJsonEntity(query);
        if(StringUtils.isNotBlank(query)){
        log.info(query.replaceAll("[\\n|\\s+]",""));
        }
        Response response = restClient.performRequest(request);
        //restClient.close();
        String result = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        return result;
    }

    @Override
    public <T> List<T> execute(String method, String endpoint, String query, Class<T> tClass) throws IOException {
        String json = executeString(method, endpoint, query);
        JSONArray object = JSON.parseObject(json).getJSONObject("hits")
                .getJSONArray("hits");
        List<T> t = new ArrayList<T>();
        for (int i = 0; i < object.size(); i++) {
            t.add(object.getJSONObject(i).getJSONObject("_source").toJavaObject(tClass));
        }
        // T t= JSON.
        return t;
    }

    @Override
    public JSONObject executeJSON(String method, String endpoint, String query) throws IOException {
        String json = executeString(method, endpoint, query);
        JSONObject jsonArray = JSONObject.parseObject(json);
        return jsonArray;
    }

    @Override
    public <T> T executeOne(String method, String endpoint, String query, Class<T> tClass) throws IOException {
        String json = executeString(method, endpoint, query);
        T t = JSON.parseObject(json).getJSONObject("hits")
                .getJSONArray("hits").getJSONObject(0).getJSONObject("_source").toJavaObject(tClass);
        return t;
    }


    public String executeAsyncString(String method, String endpoint, String query,int num) throws IOException, InterruptedException {
        Request request = new Request(method, endpoint);
        request.setJsonEntity(query);
        String[] result = new String[2];
        final CountDownLatch latch = new CountDownLatch(num);
        for(int i=0;i<num;i++) {
            ResponseListener responseListener = new ResponseListener() {
                @Override
                public void onSuccess(Response response) {
                    try {
                        result[0] = EntityUtils.toString(response.getEntity());
                        EntityUtils.consume(response.getEntity());
                        latch.countDown();
                    } catch (Exception e) {
                        log.warn("Successful, but error in return data");
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    log.error("Asynchronous execution failed");
                }
            };
            restClient.performRequestAsync(request, responseListener);
        }
       latch.await();
        return result[0];
    }

    public void executeAsync(String method, String endpoint, String query,ResponseListener responseListener) throws IOException {
        Request request = new Request(method, endpoint);
        request.setJsonEntity(query);
        restClient.performRequestAsync(request,responseListener);

    }


    @Override
    public <T> List<T> executeAsync(String method, String endpoint, String query,int num, Class<T> tClass) throws IOException, InterruptedException {
        String json = executeAsyncString(method, endpoint, query,num);
        if (StringUtils.isNotBlank(json)) {
            JSONArray object = JSON.parseObject(json).getJSONObject("hits")
                    .getJSONArray("hits");
            List<T> t = new ArrayList<T>();
            for (int i = 0; i < object.size(); i++) {
                t.add(object.getJSONObject(i).getJSONObject("_source").toJavaObject(tClass));
            }
            // T t= JSON.
            return t;
        }
        return null;
    }

    @Override
    public JSONObject executeJSONAsync(String method, String endpoint, String query,int num) throws IOException, InterruptedException {

        String json = executeAsyncString(method, endpoint, query,num);
        if (StringUtils.isNotBlank(json)) {
            JSONObject jsonArray = JSONObject.parseObject(json);
            return jsonArray;
        }
        return null;
    }

    @Override
    public <T> T executeOneAsync(String method, String endpoint, String query,int num ,Class<T> tClass) throws IOException, InterruptedException {
        String json = executeAsyncString(method, endpoint, query,num);
        if (StringUtils.isNotBlank(json)) {
            T t = JSON.parseObject(json).getJSONObject("hits")
                    .getJSONArray("hits").getJSONObject(0).getJSONObject("_source").toJavaObject(tClass);
            return t;
        }
        return null;
    }

}

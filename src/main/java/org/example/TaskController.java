package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class TaskController {
    private static Logger log = LogManager.getLogger();
    private CloseableHttpClient client = null;
    private HttpClientBuilder builder;
    private String server;
    private int retryDelay = 5 * 1000;
    private int retryCount = 2;
    private int metadataTimeout = 30 * 1000;

    public TaskController(String _server) {
        CookieStore httpCookieStore = new BasicCookieStore();
        builder = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore);
        client = builder.build();
        this.server = _server;
    }

    public Document getUrl(String url) {
        int code = 0;
        boolean bStop = false;
        Document doc = null;
        for (int iTry = 0; iTry < retryCount && !bStop; iTry++) {
            log.info("Получение страницы по адресу: " + url);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(metadataTimeout)
                    .setConnectTimeout(metadataTimeout)
                    .setConnectionRequestTimeout(metadataTimeout)
                    .setExpectContinueEnabled(true)
                    .build();
            HttpGet request = new HttpGet(url);
            request.setConfig(requestConfig);
            CloseableHttpResponse response = null;
            try {
                response = client.execute(request);
                code = response.getStatusLine().getStatusCode();
                if (code == 404) {
                    log.warn("error get url " + url + " code " + code);
                    bStop = true;//break;
                } else if (code == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        try {
                            doc = Jsoup.parse(entity.getContent(), "UTF-8", server);
                            break;
                        } catch (IOException e) {
                            log.error(e);
                        }
                    }
                    bStop = true;//break;
                } else {
                    //if (code == 403) {
                    log.warn("error get url " + url + " code " + code);
                    response.close();
                    response = null;
                    client.close();
                    CookieStore httpCookieStore = new BasicCookieStore();
                    builder.setDefaultCookieStore(httpCookieStore);
                    client = builder.build();
                    int delay = retryDelay * 1000 * (iTry + 1);
                    log.info("wait " + delay / 1000 + " s...");
                    try {
                        Thread.sleep(delay);
                        continue;
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            } catch (IOException e) {
                log.error(e);
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
        return doc;
    }

    public String GetPage(String link) {
        Document ndoc = getUrl(link);
        String text = "";
        if (ndoc != null) {
            // The text of the publication
            String textPublication = ndoc.select("div.content-box").text();
            log.info("Текст публикации: " + textPublication);

            // The title of the publication
            String header = ndoc.select("title").first().text();
            log.info("Заголовок: " + header);

            // The time of the publication
            String timePublication = ndoc.select("meta[property=article:published_time]").first().attr("content");
            log.info("Время публикации: " + timePublication);

            // Author of the publication
            String authorPublication = ndoc.select("meta[name=twitter:data1]").first().attr("content");
            log.info("Автор публикации: " + authorPublication);

            // Link to the page
            log.info("Ссылка на страницу: " + link);
        }
        return text;
    }
}
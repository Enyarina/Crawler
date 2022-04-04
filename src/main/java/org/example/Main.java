package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
    private static Logger log = LogManager.getLogger();
    private static TaskController taskController;
    private static String site = "https://spmiyaki.ru/";

    static public void ParseNews(Document doc) {
        Elements news = doc.getElementsByClass("theiaStickySidebar").select("div[class*=news_loop_img]");
        for (Element element : news) {
            try {
                Element eTitle = element.child(0);
                String link = eTitle.attr("href");
                String text = taskController.GetPage(link);
                log.info(text);

            } catch (Exception e) {
                log.error(e);
            }
        }

        return;
    }

    public static void main(String[] args) {
        taskController = new TaskController(site);
        Document doc = taskController.getUrl(site);
        String title;
        if (doc != null) {
            title = doc.title();
            log.info(title);
            ParseNews(doc);
        }
        return;
    }
}

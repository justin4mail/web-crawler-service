package au.com.qantas.crawler.service.impl;

import java.io.IOException;
import java.util.Optional;

import javax.cache.annotation.CacheResult;
import javax.inject.Named;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;

import au.com.qantas.crawler.configuration.QantasProperties.CrawlerProperties;
import au.com.qantas.crawler.model.PageInfo;
import au.com.qantas.crawler.model.PageTreeInfo;
import au.com.qantas.crawler.service.CrawlerService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author buddy
 *
 */
@Slf4j
@Named
public class CrawlerServiceImpl implements CrawlerService {

    @Value("#{qantasProperties.crawler}")
    private CrawlerProperties crawlerProperties;

    /*
     * recursive crawler to fetch child pages upto desired depth / max depth
     * (non-Javadoc)
     *
     * @see au.com.qantas.crawler.service.CrawlerService#deepCrawl(java.lang.String,
     * int)
     */
    @Override
    @CacheResult(cacheName = "web-crawler-service")
    public PageTreeInfo deepCrawl(final String url, final int depth) {

        log.debug("Starting crawler for url {} for depth {}", url, depth);
        if (depth < 0) {
            log.info("Maximum depth reached, backing out for url {}", url);
            return null;
        } else {
            final PageTreeInfo pageTreeInfo = new PageTreeInfo(url);
            crawl(url).ifPresent(pageInfo -> {
                pageTreeInfo.title(pageInfo.getTitle()).valid(true);
                log.info("Found {} links on the web page: {}", pageInfo.getLinks().size(), url);
                pageInfo.getLinks().forEach(link -> {
                    pageTreeInfo.addNodesItem(deepCrawl(link.attr("abs:href"), depth - 1));
                });
            });
            return pageTreeInfo;
        }

    }

    /*
     * Method to fetch web page content. Cache is used for better performance
     *
     * @see au.com.qantas.crawler.service.CrawlerService#crawl(java.lang.String)
     */
    @Override
    @CacheResult(cacheName = "web-crawler-service")
    public Optional<PageInfo> crawl(final String url) {

        log.info("Fetching contents for url: {}", url);
        try {
            final Document doc = Jsoup.connect(url).timeout(crawlerProperties.getTimeOut())
                    .followRedirects(crawlerProperties.isFollowRedirects()).get();

            /** .select returns a list of links here **/
            final Elements links = doc.select("a[href]");
            final String title = doc.title();
            log.debug("Fetched title: {}, links[{}] for url: {}", title, links.nextAll(), url);
            return Optional.of(new PageInfo(title, url, links));
        } catch (final IOException e) {
            log.error(String.format("Error getting contents of url %s", url), e);
            return Optional.empty();
        }

    }

}

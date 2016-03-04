package org.dspace.seoproxy;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public abstract class AbstractHTMLUnitServlet extends HttpServlet {
    protected static final String P_TARGET_URI = "targetUri";
    protected static final String P_MAX_CACHE_SIZE = "maxCacheSize";
    protected static final String P_CACHE_EXPIRE_DURATION_IN_MINUTES =
            "cacheExpireDurationInMinutes";
    protected static final int DEFAULT_MAX_CACHE_SIZE = 10000;
    protected static final int DEFAULT_CACHE_EXPIRE_DURATION = 15;

    protected String targetUri;
    protected URI targetUriObj;
    protected HttpHost targetHost;
    protected Cache<String, String> cache;

    public void init() throws ServletException {
        initCache();
        initTargetURI();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter writer = response.getWriter();

        if (cache.asMap().containsKey(requestURI)) {
            String cachedValue = cache.asMap().get(requestURI);
            writer.print(cachedValue);
        }
        else {
            final WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER);
            WebClientOptions options = webClient.getOptions();
            options.setThrowExceptionOnScriptError(false);
            options.setCssEnabled(false);

            try {
                HtmlPage page = webClient.getPage(new URL(targetUriObj.toURL(), requestURI));
                detectPageHasFinishedLoading(webClient, page);
                String result = page.asXml();
                cache.put(requestURI, result);
                writer.print(result);
            } catch (InterruptedException e) {
                throw new ServletException(e);
            }
            finally {
                webClient.close();
            }
        }

    }

    protected abstract void detectPageHasFinishedLoading(WebClient webClient, HtmlPage page) throws InterruptedException;

    protected void initTargetURI() throws ServletException {
        targetUri = getConfigParam(P_TARGET_URI);
        if (targetUri == null)
            throw new ServletException(P_TARGET_URI+" is required.");
        //test it's valid
        try {
            targetUriObj = new URI(targetUri);
        } catch (Exception e) {
            throw new ServletException("Trying to process targetUri init parameter: "+e,e);
        }
        targetHost = URIUtils.extractHost(targetUriObj);
    }

    protected String getConfigParam(String key) {
        return getServletConfig().getInitParameter(key);
    }

    protected void initCache() throws ServletException {
        int maxCacheSize = getConfigParamAsInt(P_MAX_CACHE_SIZE, DEFAULT_MAX_CACHE_SIZE);
        int expireDuration = getConfigParamAsInt(P_CACHE_EXPIRE_DURATION_IN_MINUTES,
                DEFAULT_CACHE_EXPIRE_DURATION);

        cache = CacheBuilder.newBuilder()
                .maximumSize(maxCacheSize)
                .expireAfterWrite(expireDuration, TimeUnit.MINUTES)
                .build();
    }

    protected int getConfigParamAsInt(String param, int defaultValue) throws ServletException {
        String strValue = getConfigParam(param);
        if (strValue != null) {
            try {
                return Integer.parseInt(strValue);
            } catch (NumberFormatException e) {
                throw new ServletException("The value for " + param + " is not an integer: "+e,e);
            }
        }
        else {
            return defaultValue;
        }
    }

}

package org.dspace.seoproxy;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import javax.servlet.ServletException;

public class HTMLUnitServlet extends AbstractHTMLUnitServlet {
    protected static final String P_WAIT_TIME_IN_MS = "waitTimeInMs";
    protected static final int DEFAULT_WAIT_TIME = 2000;

    protected int waitTime;

    public void init() throws ServletException {
        super.init();
        waitTime = getConfigParamAsInt(P_WAIT_TIME_IN_MS, DEFAULT_WAIT_TIME);
    }

    /**
     * This implementation doesn't verify anything, it just waits for a configurable amount of ms to
     * let the javascript settle. This doesn't require knowledge of the JS app, but is also slower
     * in most cases, and more error prone.
     */
    protected void detectPageHasFinishedLoading(WebClient webClient, HtmlPage page) throws
            InterruptedException {
        webClient.waitForBackgroundJavaScript(waitTime);
    }
}

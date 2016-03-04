package org.dspace.seoproxy;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.collections.CollectionUtils;

public class DSEmberServlet extends AbstractHTMLUnitServlet {

    /**
     * Detect whether the app has finished loading, by checking every 50ms for a max of 10s
     * <p/>
     * Afterwards, wait an extra two seconds after the app has officially loaded to let the trail
     * settle. This is due to a design decision I made to not let the user wait for the trail to
     * load before showing any content, because it meant that every parent object of the current DSO
     * had to be fetched recursively, and that could take a while.
     * <p/>
     * This issue could be solved by not checking when ember shows the content (as I'm doing now)
     * but setting a global JS variable when everything has finished loading, and have HTMLUnit look
     * for that. I didn't go through the trouble because the REST API recently got a hierarchies
     * endpoint that makes the recursive lookup obsolete.
     */
    protected void detectPageHasFinishedLoading(WebClient webClient, HtmlPage
            page) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            if (hasFinishedLoading(page)) {
                break;
            }
            synchronized (page){
                page.wait(50);
            }
        }

        synchronized (page){
            page.wait(2000);
        }
    }

    /**
     * The app has finished loading when it has a main-container (so it has been initialized), and
     * no longer has the loading-pane
     */
    protected boolean hasFinishedLoading(HtmlPage page) {
        return CollectionUtils.isNotEmpty(page.getByXPath("//main[@id = 'main-container']")) &&
                CollectionUtils.isEmpty(page.getByXPath("//div[contains(@class, 'loading-pane')]"));
    }


}

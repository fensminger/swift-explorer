package org.swiftexplorer.auth.oauth;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.swiftexplorer.auth.server.SynchronousDataProvider;
import org.swiftexplorer.auth.webbrowser.AuthBatch;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Created by fer on 09/09/2014.
 */
public class AuthBatchTest extends AuthBatch {

    public AuthBatchTest() {

    }

    protected AuthBatchTest(SynchronousDataProvider<?> syncDataProvider, String url) {
        super(syncDataProvider, url);
    }

    @Test
    public void testFirstAuthGet() throws IOException {
        URL resource = getClass().getResource("/HubicFirstGet.html");
        File input = new File(resource.getFile());
        Document doc = Jsoup.parse(input, "UTF-8", "http://example.com/");
        Map<String, String> res = anaParamsForFirstGet(doc, "user_login", "MotDePasse");
        System.out.println("Parameter : " + res);
        System.out.println("Action : " + loadAction(doc));
    }
}

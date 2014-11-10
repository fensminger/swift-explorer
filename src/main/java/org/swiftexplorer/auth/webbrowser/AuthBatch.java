/*
 * Copyright 2014 Loic Merckel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.swiftexplorer.auth.webbrowser;

import com.google.common.io.CharStreams;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.auth.server.SynchronousDataProvider;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class AuthBatch {

	final private Logger logger = LoggerFactory.getLogger(AuthBatch.class);

	private final Object lock = new Object () ;

	private volatile SynchronousDataProvider<?> syncDataProvider = null ;

    public AuthBatch() {

    }

    protected AuthBatch(SynchronousDataProvider<?> syncDataProvider, String url)
	{
		super () ;
		this.syncDataProvider = syncDataProvider ;
	}
	
	
	public static AuthBatch authenticate(SynchronousDataProvider<?> syncDataProvider, String url)
	{
		return new AuthBatch(syncDataProvider, url) ;
	}
	
	public void startHubic(final String url) {
        start(url);
    }

	private void start(final String url)
	{
        boolean val = true;
	    Runnable runnable = () -> {
            int nbRetry = 0;
            while (!tryOneHubicConnection(url)) {
                nbRetry++;
                final long waiting;
                switch (nbRetry) {
                    case 0:
                        waiting = 1000 * 30;
                        break;
                    case 1:
                        waiting = 1000 * 60;
                        break;
                    case 3:
                        waiting = 1000 * 60 * 2;
                        break;
                    default:
                        waiting = 1000 * 60 * 5;
                }
                logger.warn("Impossible de se connecter à Hubic. Nombre de tentatives : " + nbRetry);
                logger.warn("Retentative dans " + waiting + "ms");
                try {
                    Thread.sleep(waiting);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
	}

    private boolean tryOneHubicConnection(String url) {
        try {
            ConnectionKeepAliveStrategy myStrategy = (response, context) -> {
                // Honor 'keep-alive' header
                HeaderElementIterator it = new BasicHeaderElementIterator(
                        response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        try {
                            return Long.parseLong(value) * 1000;
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
                return 30 * 1000;
            };


            HttpClientContext httpClientContext = HttpClientContext.create();

            CookieStore cookieStore = new BasicCookieStore();
            httpClientContext.setCookieStore(cookieStore);
            cookieStore.addCookie(new BasicClientCookie("_pk_id.21.7fb5", "b2b408d941f96548.1386016715.2.1395093565.1386016715."));

            CloseableHttpClient httpClient = HttpClients.custom()
                    //.setKeepAliveStrategy(myStrategy)
                    .build();

            HttpGet httpGet = new HttpGet(url);
            Header[] getHeaders = new Header[]{
                    new BasicHeader("Host", "api.hubic.com"),
                    new BasicHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:31.0) Gecko/20100101 Firefox/31.0"),
                    new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"),
                    new BasicHeader("Accept-Language", "fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3"),
                    new BasicHeader("Accept-Encoding", "gzip, deflate"),
                    new BasicHeader("Cache-control", "max-age=0"),
                    new BasicHeader("Connection", "keep-alive")
            };
            httpGet.setHeaders(getHeaders);

            HttpResponse response = httpClient.execute(httpGet, httpClientContext);

            final HttpEntity entity = response.getEntity();
            String body = "";
            try (InputStreamReader content = new InputStreamReader(entity.getContent())) {
                body = CharStreams.toString(content);
            }
            Header[] headers = response.getAllHeaders();
            for(Header header : headers) {
                logger.info("Header : " + header.toString());
            }

            for(Cookie cookie : cookieStore.getCookies()) {
                logger.info("Cookie : " + cookie.toString());
            }

            Document doc = Jsoup.parse(body);
            Properties properties = new Properties();
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("hubiccon.properties"));
            Map<String, String> params = anaParamsForFirstGet(doc, properties.getProperty("login"), properties.getProperty("pwd"));
            String action = loadAction(doc);

            logger.info("Accès à l'URL : " + url);

            //logger.info(body);

            logger.info("===================================================================================================================");
            String urlPost = "https://api.hubic.com/oauth/auth/"; //"https://api.hubic.com/" ;//+ action;
            Header[] postHeaders = new Header[]{
                    new BasicHeader("Host", "api.hubic.com"),
                    new BasicHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:31.0) Gecko/20100101 Firefox/31.0"),
                    new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"),
                    new BasicHeader("Accept-Language", "fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3"),
                    new BasicHeader("Accept-Encoding", "gzip, deflate"),
                    new BasicHeader("Referer", url),
                    new BasicHeader("Connection", "keep-alive")
            };
            for(Header header : postHeaders) {
                logger.info("Post Header : " + header.toString());
            } // "https://api.hubic.com/oauth/auth/?client_id=api_hubic_hB3LO1RcO0Rz2xhqiYZBvYyFv0OQ5mmM&redirect_uri=http%3A%2F%2Flocalhost%3A9000%2F&response_type=code&state=RandomString&scope=credentials.r"

            CookieStore cookiePostStore = new BasicCookieStore();
            //httpClientContext.setCookieStore(cookiePostStore);
            cookiePostStore.addCookie(new BasicClientCookie("_pk_id.21.7fb5", "b2b408d941f96548.1386016715.2.1395093565.1386016715."));

            HttpPost httpPost = new HttpPost(urlPost);
            httpPost.setHeaders(postHeaders);
            List<NameValuePair> paramList = new ArrayList<NameValuePair>();
            for(String key : params.keySet()) {
                paramList.add(new BasicNameValuePair(key, params.get(key)));
                logger.info("Paramètres : " + key + " : " + params.get(key));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(paramList));
            httpPost.setHeader("ContentType", "application/x-www-form-urlencoded");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(),e);
            }

            HttpResponse responsePost = httpClient.execute(httpPost, httpClientContext);
            StatusLine statusLine = responsePost.getStatusLine();
            logger.info("statusLine: " + statusLine);
            String bodyPost = "";
            try (InputStreamReader content = new InputStreamReader(responsePost.getEntity().getContent())) {
                bodyPost = CharStreams.toString(content);
            }

//                logger.info("Accès à l'URL de post: " + urlPost);
//                for(Cookie cookie : cookieStore.getCookies()) {
//                    logger.info("Cookie : " + cookie.toString());
//                }

            String urlRedirectLocal = null;
            for(Header header : responsePost.getAllHeaders()) {
                logger.info("Response Post Header : " + header.toString());
                if ("Location".equals(header.getName())) {
                    urlRedirectLocal = header.getValue();
                }
            }

            if (urlRedirectLocal!=null) {
                logger.info("Url de redirection : " + urlRedirectLocal);
                HttpGet httpGetRedirect = new HttpGet(urlRedirectLocal);
                HttpResponse responseRedirect = httpClient.execute(httpGetRedirect);
                logger.info("statusLine redirect: " + responseRedirect.getStatusLine());
                logger.info("statusLine status code: " + responseRedirect.getStatusLine().getStatusCode());
                String bodyRedirectResponse = "";
                try (InputStreamReader content = new InputStreamReader(responseRedirect.getEntity().getContent())) {
                    bodyRedirectResponse = CharStreams.toString(content);
                }
                logger.info("Response body redirect : " + bodyRedirectResponse);
                if (!bodyRedirectResponse.contains("Host = [")) {
                    return false;
                }
            }

            // Bonne réponse :
//                2014-11-09 17:45:06,792 INFO  org.swiftexplorer.auth.webbrowser.AuthBatch lambda$start$1- Url de redirection : http://localhost:9998/?code=14155551068lKKFWaBkvEbDaTHK9sw2g1m9B4bxukogu0gbqpmPVK4xbk8CnwLTd1NlDiHpj0a&scope=account.r,credentials.r&state=RandomString
//                2014-11-09 17:45:06,819 INFO  org.swiftexplorer.auth.webbrowser.AuthBatch lambda$start$1- statusLine redirect: HTTP/1.1 200 OK
//                2014-11-09 17:45:06,821 INFO  org.swiftexplorer.auth.webbrowser.AuthBatch lambda$start$1- Response body redirect : Accept-encoding = [gzip,deflate]
//                Connection = [Keep-Alive]
//                Host = [localhost:9998]
//                User-agent = [Apache-HttpClient/4.3.5 (java 1.5)]
//
//                2014-11-09 17:45:11,517 INFO  org.swiftexplorer.swift.util.HubicBatchSwift getSwiftAccess- Swift access token expiry date: 2014-11-09T23:25:56+01:00
//                2


        } catch (IOException e) {
            logger.error("Impossible de se connecter à hubic. " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    protected Map<String, String> anaParamsForFirstGet(Document doc, String user, String password) {
        Elements links = doc.select("input");
        HashMap<String, String> res = new HashMap<>();
        for(Element elt : links) {
            String type = elt.attr("type");
            if (!"button".equals(type) && !"submit".equals(type)) {
                String name = elt.attr("name");
                String value = elt.attr("value");
                if (value==null || "".equals(value.trim())) {
                    if (name.contains("pwd") || name.contains("password")) {
                        value = password;
                    }
                    if (name.contains("login")) {
                        value = user;
                    }
                }
                res.put(name, value);
            }
        }
        return res;
    }

    protected String loadAction(Document doc) {
        return doc.select("form").attr("action");
    }

}

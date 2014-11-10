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

package org.swiftexplorer.auth.oauth;

import org.scribe.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.auth.builder.api.HubicApi;
import org.swiftexplorer.auth.server.AuthHttpServer;
import org.swiftexplorer.auth.webbrowser.AuthBatch;
import org.swiftexplorer.auth.webbrowser.AuthWebView;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class HubicBatchOAuth20ServiceImpl extends HubicOAuth20ServiceImpl {

	final private Logger logger = LoggerFactory.getLogger(HubicBatchOAuth20ServiceImpl.class);

	public HubicBatchOAuth20ServiceImpl(HubicApi api, OAuthConfig config) {
        super(api, config);
	}
	

    @Override
	public Verifier obtainVerifier ()
	{
		int port = 80 ;
		try 
		{
			port = new URL(config.getCallback()).getPort() ;
		} 
		catch (MalformedURLException e) 
		{
			logger.error("Error occurred while obtaining the code verifier", e);
		}
		
		AuthHttpServer httpServer = new AuthHttpServer(port);
		
		// here we assume that the server will be started by the time the
		// user enters the required information
//        AuthWebView authWebView = AuthWebView.openNewBrowser (httpServer, getAuthorizationUrl(null)) ;
        final String authorizationUrl = getAuthorizationUrl(null);
        AuthBatch authBatch = AuthBatch.authenticate (httpServer, authorizationUrl) ;

		new Thread(() -> {
			try {
				// Pour être certain que le serveur web est bien démarré car le NAS est très lent.
				Thread.currentThread().sleep(5000L);
				authBatch.startHubic(authorizationUrl);
			} catch (InterruptedException e) {
				logger.error("Authentification interrompue : " + e.getMessage());
			}
		}).start();

		Map<String, String> params = null ;
		try 
		{
			// the server is being started
			params = httpServer.startAndWaitForData() ;
			httpServer.stopServer();
			//authWebView.setVisible(false);
			
		} 
		catch (IOException | InterruptedException e) 
		{	
			logger.error("Error occurred while obtaining the code verifier", e);
		}
		finally
		{
//			authWebView.dispose();
		}
		if (params == null)
			return null ;
		
		String code = params.get("code") ;
		
		return ((code == null) ? (null) : (new Verifier(code))) ;
	}
	
}

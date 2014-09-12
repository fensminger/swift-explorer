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

package org.swiftexplorer.auth.builder.api;


import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;
import org.scribe.utils.Preconditions;
import org.swiftexplorer.auth.extractors.HubicTokenExtractorImpl;
import org.swiftexplorer.auth.oauth.HubicBatchOAuth20ServiceImpl;
import org.swiftexplorer.auth.oauth.HubicOAuth20ServiceImpl;
import org.swiftexplorer.config.Configuration;


public class HubicBatchApi extends HubicApi {

	@Override
	public OAuthService createService(OAuthConfig config)
	{
		return new HubicBatchOAuth20ServiceImpl(this, config);
	}
	
}

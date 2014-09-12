package org.swiftexplorer.swift.util;

import com.google.gson.Gson;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.auth.builder.api.HubicApi;
import org.swiftexplorer.auth.builder.api.HubicBatchApi;
import org.swiftexplorer.auth.oauth.HubicBatchOAuth20ServiceImpl;
import org.swiftexplorer.auth.oauth.HubicOAuth20ServiceImpl;
import org.swiftexplorer.config.Configuration;
import org.swiftexplorer.config.auth.HasAuthenticationSettings;
import org.swiftexplorer.swift.SwiftAccess;

public final class HubicBatchSwift {

    final private static Logger logger = LoggerFactory.getLogger(HubicBatchSwift.class);

    private HubicBatchSwift() { super () ; } ;

    private static final Gson gson  = new Gson () ;

    private static final String scope = "credentials.r" ;

    public static SwiftAccess getSwiftAccess ()
    {
        final HasAuthenticationSettings authSettings = Configuration.INSTANCE.getAuthenticationSettings() ;
        String apiKey = authSettings.getClientId() ;
        String apiSecret = authSettings.getClientSecret() ;

        HubicBatchOAuth20ServiceImpl service = (HubicBatchOAuth20ServiceImpl) new ServiceBuilder()
                .provider(HubicBatchApi.class).apiKey(apiKey).apiSecret(apiSecret)
                        //.scope("account.r,links.rw,usage.r,credentials.r").callback(HubicApi.CALLBACK_URL)
                .scope(scope).callback(HubicApi.CALLBACK_URL)
                .build();

        Verifier verif = service.obtainVerifier();

        if (verif == null)
            return null ;

        Token accessToken = service.getAccessToken(null, verif);
        return getSwiftAccess (service, accessToken) ;
    }


    public static SwiftAccess refreshAccessToken(Token expiredToken)
    {
        final HasAuthenticationSettings authSettings = Configuration.INSTANCE.getAuthenticationSettings();
        String apiKey = authSettings.getClientId();
        String apiSecret = authSettings.getClientSecret();

        HubicBatchOAuth20ServiceImpl service = (HubicBatchOAuth20ServiceImpl) new ServiceBuilder()
                .provider(HubicBatchApi.class).apiKey(apiKey).apiSecret(apiSecret)
                        //.scope("account.r,links.rw,usage.r,credentials.r")
                .scope(scope)
                .callback(HubicApi.CALLBACK_URL).build();

        Token accessToken = service.refreshAccessToken(expiredToken);
        return getSwiftAccess (service, accessToken) ;
    }


    private static SwiftAccess getSwiftAccess (HubicBatchOAuth20ServiceImpl service, Token accessToken)
    {
        String urlCredential = HubicApi.CREDENTIALS_URL;

        OAuthRequest request = new OAuthRequest(Verb.GET, urlCredential);
        request.setConnectionKeepAlive(false);
        service.signRequest(accessToken, request);
        Response responseReq = request.send();

        SwiftAccess ret = gson.fromJson(responseReq.getBody(), SwiftAccess.class) ;
        ret.setAccessToken(accessToken);

        logger.info("Swift access token expiry date: " + ret.getExpires());

        return ret ;
    }
}

package org.swiftexplorer;

import org.apache.commons.configuration.ConfigurationException;
import org.scribe.model.OAuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.auth.builder.api.HubicApi;
import org.swiftexplorer.config.Configuration;
import org.swiftexplorer.config.localization.HasLocalizationSettings;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.localization.LocalizedStringsImpl;
import org.swiftexplorer.swift.SwiftAccess;
import org.swiftexplorer.swift.util.HubicBatchSwift;
import org.swiftexplorer.swift.util.HubicSwift;

import java.io.File;
import java.util.Locale;

/**
 * Created by fer on 07/09/2014.
 */
public class HubicSyncBatch {
    final static Logger logger = LoggerFactory.getLogger(SwiftExplorer.class);

    public HubicSyncBatch() {
        super();
    }

    public static void main( String[] args ) {
        // load the settings
        String settingsFile = "swiftexplorer-settings.xml" ;
        if (!new File(settingsFile).exists())
            settingsFile = null ;
        try
        {
            Configuration.INSTANCE.load(settingsFile);
        }
        catch (ConfigurationException e)
        {
            logger.error("Error occurred while initializing the configuration", e);
        }


        Locale locale = Locale.getDefault() ;
        HasLocalizationSettings localizationSettings = Configuration.INSTANCE.getLocalizationSettings() ;
        if (localizationSettings != null)
        {
            Locale.Builder builder = new Locale.Builder () ;
            HasLocalizationSettings.LanguageCode lang = localizationSettings.getLanguage() ;
            HasLocalizationSettings.RegionCode reg = localizationSettings.getRegion() ;
            builder.setLanguage(lang.toString()) ;
            if (reg != null)
                builder.setRegion(reg.toString()) ;
            else
                builder.setRegion("") ;
            locale = builder.build();
        }

        final HasLocalizedStrings localizedStrings = new LocalizedStringsImpl(locale) ;

        SwiftAccess access = HubicBatchSwift.getSwiftAccess();

        System.out.println(""+access);
    }


}

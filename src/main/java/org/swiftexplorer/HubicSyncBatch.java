package org.swiftexplorer;

import org.apache.commons.configuration.ConfigurationException;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.Directory;
import org.javaswift.joss.model.DirectoryOrObject;
import org.javaswift.joss.model.StoredObject;
import org.scribe.model.OAuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.auth.builder.api.HubicApi;
import org.swiftexplorer.config.Configuration;
import org.swiftexplorer.config.HasConfiguration;
import org.swiftexplorer.config.localization.HasLocalizationSettings;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.localization.LocalizedStringsImpl;
import org.swiftexplorer.gui.login.CloudieCallbackWrapper;
import org.swiftexplorer.swift.SwiftAccess;
import org.swiftexplorer.swift.client.factory.AccountConfigFactory;
import org.swiftexplorer.swift.operations.CallBackInfo;
import org.swiftexplorer.swift.operations.SwiftOperations;
import org.swiftexplorer.swift.operations.SwiftOperationsImpl;
import org.swiftexplorer.swift.util.HubicBatchSwift;
import org.swiftexplorer.swift.util.HubicSwift;

import java.io.File;
import java.util.*;

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

        final SwiftOperations ops = new SwiftOperationsImpl() ;
        final CallBackInfo callBackInfo = new CallBackInfo();

        SwiftOperations.SwiftCallback swiftCallBack = new SwiftOperations.SwiftCallback() {
            @Override
            public void onStart() {
                logger.info("onStart");
            }

            @Override
            public void onDone() {
                logger.info("onDone");
            }

            @Override
            public void onError(CommandException ex) {
                logger.info("onError");
            }

            @Override
            public void onUpdateContainers(Collection<Container> containers) {
                logger.info("onUpdateContainers");
                callBackInfo.containers = containers;
            }

            @Override
            public void onNewStoredObjects() {
                logger.info("onNewStoredObjects");
            }

            @Override
            public void onAppendStoredObjects(Container container, int page, Collection<StoredObject> storedObjects) {
                logger.info("onAppendStoredObjects");
            }

            @Override
            public void onLoginSuccess() {
                logger.info("onLoginSuccess -> yes");
            }

            @Override
            public void onLogoutSuccess() {
                logger.info("onLogoutSuccess");
            }

            @Override
            public void onContainerUpdate(Container container) {
                logger.info("onContainerUpdate");
            }

            @Override
            public void onStoredObjectUpdate(StoredObject obj) {
                logger.info("onStoredObjectUpdate");
            }

            @Override
            public void onNumberOfCalls(int nrOfCalls) {
                logger.info("onNumberOfCalls : " + nrOfCalls);
            }

            @Override
            public void onStoredObjectDeleted(Container container, StoredObject storedObject) {
                logger.info("onStoredObjectDeleted");
            }

            @Override
            public void onStoredObjectDeleted(Container container, Collection<StoredObject> storedObjects) {
                logger.info("onStoredObjectDeleted");
            }

            @Override
            public void onProgress(double totalProgress, String totalMsg, double currentProgress, String currentMsg) {
                logger.info("onProgress");
            }

            @Override
            public void onStopped() {
                logger.info("onStopped");
            }
        };

        ops.login(AccountConfigFactory.getHubicAccountConfig(access), Configuration.INSTANCE.getHttpProxySettings(), swiftCallBack);

        ops.refreshContainers(swiftCallBack);

        Container mainContainer = null;
        for (Container container : callBackInfo.containers) {
            logger.info("Container name : " + container.getName());
            if ("default".equals(container.getName())) {
                mainContainer = container;
            }
        }

//        Collection<DirectoryOrObject> mainDirList = mainContainer.listDirectory();
//
//        for(DirectoryOrObject directoryOrObject : mainDirList ) {
//            logger.info("is Directory : " + directoryOrObject.isDirectory() + ", name : " + directoryOrObject.getName());
//            if (directoryOrObject.isObject()) {
//                StoredObject storeObject = directoryOrObject.getAsObject();
//                logger.info("Last modified:  "+storeObject.getLastModified());
//                logger.info("ETag:           "+storeObject.getEtag());
//                logger.info("Content type:   "+storeObject.getContentType());
//                logger.info("Content length: " + storeObject.getContentLength());
//            }
//        }

        Collection<StoredObject> res = getAllContainedStoredObject(mainContainer, null);
        for(StoredObject storedObject : res) {
                logger.info("Content type:   "+storedObject.getContentType() + " -> " + storedObject.getName());
        }

        System.out.println("Nombre de contenaires : "+callBackInfo.containers.size());
    }

    private static Collection<StoredObject> getAllContainedStoredObject (Container container, Directory parent)
    {
        Set<StoredObject> results = new TreeSet<>();
        Queue<DirectoryOrObject> queue = new ArrayDeque<> () ;
        queue.addAll((parent == null) ? (container.listDirectory()) : (container.listDirectory(parent))) ;
        while (!queue.isEmpty())
        {
            DirectoryOrObject currDirOrObj = queue.poll() ;
            if (currDirOrObj != null)
            {
                if (currDirOrObj.isObject())
                    results.add(currDirOrObj.getAsObject()) ;
                if (currDirOrObj.isDirectory())
                    queue.addAll(container.listDirectory(currDirOrObj.getAsDirectory())) ;
            }
        }
        return results ;
    }

}

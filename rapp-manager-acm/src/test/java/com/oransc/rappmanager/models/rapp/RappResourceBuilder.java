package com.oransc.rappmanager.models.rapp;

import com.oransc.rappmanager.models.rappinstance.RappACMInstance;
import com.oransc.rappmanager.models.rappinstance.RappDMEInstance;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import java.util.Set;

public class RappResourceBuilder {

    public RappResources getResources() {
        RappResources rappResources = new RappResources();
        RappResources.ACMResources acmResources = new RappResources.ACMResources("compositions", Set.of());
        acmResources.setCompositionInstances(Set.of("kserve-instance"));
        rappResources.setAcm(acmResources);
        RappResources.DMEResources dmeResources =
                new RappResources.DMEResources(Set.of("json-file-data-from-filestore"),
                        Set.of("xml-file-data-from-filestore"), Set.of("json-file-data-producer"),
                        Set.of("json-file-consumer"));
        rappResources.setDme(dmeResources);
        return rappResources;
    }

    public RappInstance getRappInstance() {
        RappInstance rappInstance = new RappInstance();
        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance("kserve-instance");
        rappInstance.setAcm(rappACMInstance);
        RappDMEInstance rappDMEInstance = new RappDMEInstance();
        rappDMEInstance.setInfoTypeConsumer("json-file-data-from-filestore");
        rappDMEInstance.setInfoTypesProducer(Set.of("xml-file-data-from-filestore"));
        rappDMEInstance.setInfoProducer("json-file-data-producer");
        rappDMEInstance.setInfoConsumer("json-file-consumer");
        rappInstance.setDme(rappDMEInstance);
        return rappInstance;
    }
}

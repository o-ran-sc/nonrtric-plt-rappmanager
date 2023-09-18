package com.oransc.rappmanager.models.rapp;

import com.oransc.rappmanager.models.rappinstance.RappACMInstance;
import com.oransc.rappmanager.models.rappinstance.RappInstance;
import java.util.Set;

public class RappResourceBuilder {

    public RappResources getResources() {
        RappResources rappResources = new RappResources();
        RappResources.ACMResources acmResources = new RappResources.ACMResources("compositions", Set.of());
        acmResources.setCompositionInstances(Set.of("kserve-instance"));
        rappResources.setAcm(acmResources);
        return rappResources;
    }

    public RappInstance getRappInstance() {
        RappInstance rappInstance = new RappInstance();
        RappACMInstance rappACMInstance = new RappACMInstance();
        rappACMInstance.setInstance("kserve-instance");
        rappInstance.setAcm(rappACMInstance);
        return rappInstance;
    }
}

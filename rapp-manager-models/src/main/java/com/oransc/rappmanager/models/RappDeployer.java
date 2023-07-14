package com.oransc.rappmanager.models;

public interface RappDeployer {
    boolean deployRapp(Rapp rapp);
    boolean undeployRapp(Rapp rapp);
}

package org.oransc.ran.nssmf.simulator.dto;

import lombok.Data;

import java.util.List;

@Data
public class NetworkSliceSubnetAttributesDTO {
    private String operationalState;
    private String administrativeState;
    private String networkSliceSubnetType;
    private List<String> managedFunctionRef;
    private List<String> networkSliceSubnetRef;
    private List<SliceProfileItemDTO> sliceProfileList;
}

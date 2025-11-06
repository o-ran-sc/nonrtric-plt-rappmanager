package org.oransc.ran.nssmf.simulator.dto;

import lombok.Data;

import java.util.List;

@Data
public class SliceProfileItemDTO {
    private String sliceProfileId;
    private SliceProfileExtensionsDTO extensions;
    private List<PlmnInfoListDTO> pLMNInfoList; // Note: JSON uses pLMNInfoList, Java can't start field with lowercase p after uppercase, so Lombok will map it.
    private RanSliceSubnetProfileDTO RANSliceSubnetProfile; // Note: JSON uses RANSliceSubnetProfile
}

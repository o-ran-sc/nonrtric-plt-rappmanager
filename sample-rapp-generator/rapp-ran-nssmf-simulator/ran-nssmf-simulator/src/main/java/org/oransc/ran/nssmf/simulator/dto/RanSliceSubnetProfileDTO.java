package org.oransc.ran.nssmf.simulator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RanSliceSubnetProfileDTO {
    private List<Integer> coverageAreaTAList;
    private String resourceSharingLevel;
    @JsonProperty("RRU.PrbDl")
    private Integer RRU_PrbDl;
    @JsonProperty("RRU.PrbUl")
    private Integer RRU_PrbUl;
}

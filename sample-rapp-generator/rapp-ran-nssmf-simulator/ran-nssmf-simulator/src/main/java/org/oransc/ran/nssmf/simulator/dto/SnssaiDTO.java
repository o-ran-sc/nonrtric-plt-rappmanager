package org.oransc.ran.nssmf.simulator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SnssaiDTO {
    @JsonProperty("sst")
    private Integer sst; // Slice/Service Type
    @JsonProperty("sd")
    private String sd;  // Slice Differentiator
}

package org.oransc.ran.nssmf.simulator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PlmnInfoListDTO {
    @JsonProperty("pLMNId")
    private PlmnIdDTO pLMNId;
    @JsonProperty("sNSSAI")
    private SnssaiDTO sNSSAI;
}

package org.oransc.ran.nssmf.simulator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SubscriptionDTO {
    @JsonProperty("consumerReference")
    private String callbackUri;
}
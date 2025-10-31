package org.oransc.ran.nssmf.simulator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SubscriptionRequestDTO {

    @NotBlank(message = "Not allow null or empty to 'consumerReference'")
    @JsonProperty("consumerReference")
    private String callbackUri;
}

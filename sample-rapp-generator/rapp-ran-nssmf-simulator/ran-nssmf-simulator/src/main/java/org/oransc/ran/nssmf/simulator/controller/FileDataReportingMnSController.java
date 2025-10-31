package org.oransc.ran.nssmf.simulator.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.oransc.ran.nssmf.simulator.dto.SubscriptionDTO;
import org.oransc.ran.nssmf.simulator.dto.SubscriptionRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/3GPPManagement/FileDataReportingMnS/${mns.fileDataReporting.version}")
@EnableScheduling
@RequiredArgsConstructor
public class FileDataReportingMnSController {

    private static final Logger logger = LoggerFactory.getLogger(FileDataReportingMnSController.class);
    
    Map<Integer, SubscriptionRequestDTO> subscriptionMap = new HashMap<>();
    static int subscriptionId = 0;

    @PostMapping("/subscriptions")
    public ResponseEntity<SubscriptionDTO> subscribe(HttpServletRequest httpRequest, @Valid @RequestBody SubscriptionRequestDTO request)
            throws Exception {
        logger.info("Received new subscription request: {}", request);
        
        subscriptionId = subscriptionId + 1;
        logger.info("Generated subscription ID: {}", subscriptionId);
        
        subscriptionMap.put(subscriptionId, request);
        logger.info("Stored subscription in map. Total subscriptions: {}", subscriptionMap.size());
        
        SubscriptionDTO subscriptionDTO = new SubscriptionDTO();
        subscriptionDTO.setCallbackUri(request.getCallbackUri());
        URI location = URI.create(httpRequest.getRequestURL().toString() + "/" + subscriptionId);
        
        logger.info("Successfully created subscription with ID: {} and callback URI: {}", 
                   subscriptionId, request.getCallbackUri());
        
        return ResponseEntity.created(location).body(subscriptionDTO);
    }
}

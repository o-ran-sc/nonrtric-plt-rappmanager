package org.oransc.ran.nssmf.simulator.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.oransc.ran.nssmf.simulator.dto.NotifyFileReadyDTO;
import org.oransc.ran.nssmf.simulator.dto.SubscriptionDTO;
import org.oransc.ran.nssmf.simulator.dto.SubscriptionRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate;


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

    @Scheduled(fixedRate = 300000) // 15 minutes = 900,000 ms
    public void sendFileReadyNotifications() {
        System.out.println("Starting to send file ready notifications to " + subscriptionMap.size() + " subscribers");

        subscriptionMap.forEach((subscriptionId, subscription) -> {
            try {
                NotifyFileReadyDTO notification = NotifyFileReadyDTO.createSampleNotification();

                ResponseEntity<String> response = restTemplate.postForEntity(
                        subscription.getCallbackUri(),
                        notification,
                        String.class
                );

                System.out.println("Successfully sent notification to subscription " + subscriptionId +
                        " at " + subscription.getCallbackUri() +
                        ". Response status: " + response.getStatusCode());
            } catch (Exception e) {
                System.err.println("Failed to send notification to subscription " + subscriptionId +
                        " at " + subscription.getCallbackUri() +
                        ". Error: " + e.getMessage());
            }
        });

        System.out.println("Completed sending file ready notifications");
    }
}

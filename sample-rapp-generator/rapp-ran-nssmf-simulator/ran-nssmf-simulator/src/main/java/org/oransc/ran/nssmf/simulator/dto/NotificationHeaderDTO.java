package org.oransc.ran.nssmf.simulator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class NotificationHeaderDTO {
    private String notificationId;
    private String notificationType;
    private DateTimeDTO eventTime;
    
    public static NotificationHeaderDTO createFileReadyHeader() {
        return NotificationHeaderDTO.builder()
                .notificationId("notif-" + System.currentTimeMillis())
                .notificationType("notifyFileReady")
                .eventTime(DateTimeDTO.now())
                .build();
    }
}

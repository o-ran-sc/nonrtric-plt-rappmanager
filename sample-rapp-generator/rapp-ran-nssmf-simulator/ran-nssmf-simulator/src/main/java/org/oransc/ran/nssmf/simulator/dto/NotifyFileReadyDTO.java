package org.oransc.ran.nssmf.simulator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class NotifyFileReadyDTO {
    private NotificationHeaderDTO notificationHeader;
    private List<FileInfoDTO> fileInfoList;
    private String additionalText;
    
    public static NotifyFileReadyDTO createSampleNotification() {
        return NotifyFileReadyDTO.builder()
                .notificationHeader(NotificationHeaderDTO.createFileReadyHeader())
                .fileInfoList(List.of(FileInfoDTO.createSampleFileInfo()))
                .additionalText("Sample file ready notification from NSSMF simulator")
                .build();
    }
}

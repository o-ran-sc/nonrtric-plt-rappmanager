package org.oransc.ran.nssmf.simulator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class FileInfoDTO {
    private String fileLocation;
    private Integer fileSize;
    private DateTimeDTO fileReadyTime;
    private DateTimeDTO fileExpirationTime;
    private String fileCompression;
    private String fileFormat;
    private String fileDataType;
    private String jobId;
    
    public static FileInfoDTO createSampleFileInfo() {
        return FileInfoDTO.builder()
                .fileLocation("http://example.com/files/sample-performance-data-" + System.currentTimeMillis() + ".csv")
                .fileSize(1024)
                .fileReadyTime(DateTimeDTO.now())
                .fileExpirationTime(DateTimeDTO.builder()
                        .dateTime(java.time.LocalDateTime.now().plusHours(24).format(java.time.format.DateTimeFormatter.ISO_DATE_TIME))
                        .build())
                .fileCompression("gzip")
                .fileFormat("CSV")
                .fileDataType("Performance")
                .jobId("job-" + System.currentTimeMillis())
                .build();
    }
}

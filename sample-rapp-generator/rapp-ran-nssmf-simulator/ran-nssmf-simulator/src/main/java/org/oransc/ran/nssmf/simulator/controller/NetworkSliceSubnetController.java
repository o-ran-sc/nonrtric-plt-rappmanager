package org.oransc.ran.nssmf.simulator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.validation.Valid;

import org.oransc.ran.nssmf.simulator.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;


@RestController
@RequestMapping("/3GPPManagement/ProvMnS/${mns.fileDataReporting.version}") // Example version
public class NetworkSliceSubnetController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkSliceSubnetController.class);

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * Retrieves an existing Network Slice Subnet by its ID.
     *
     * @param subnetId The unique identifier of the Network Slice Subnet to be retrieved.
     * @return A ResponseEntity with HTTP status 200 (OK) and the NetworkSliceSubnetDTO,
     *         or 404 (Not Found) if no such subnet exists.
     */
    @GetMapping("/NetworkSliceSubnets/{subnetId}")
    public ResponseEntity<NetworkSliceSubnetDTO> getNetworkSliceSubnet(@PathVariable String subnetId) {
        logger.info("Get request received for subnetId: {}", subnetId);
        NetworkSliceSubnetDTO responseDto = new NetworkSliceSubnetDTO();
        responseDto.setId(subnetId);

        NetworkSliceSubnetAttributesDTO attributes = new NetworkSliceSubnetAttributesDTO();
        attributes.setOperationalState("enabled");
        attributes.setAdministrativeState("UNLOCKED");
        attributes.setNetworkSliceSubnetType("RAN_SLICESUBNET");
        attributes.setManagedFunctionRef(Arrays.asList(
                "2c000978-15e3-4393-984e-a20d32c96004-AUPF_200000",
                "2c000978-15e3-4393-984e-a20d32c96004-DU_200000",
                "2c000978-15e3-4393-984e-a20d32c96004-ACPF_200000"
        ));
        attributes.setNetworkSliceSubnetRef(List.of());

        SliceProfileItemDTO sliceProfileItem = new SliceProfileItemDTO();
        SliceProfileExtensionsDTO extensions = new SliceProfileExtensionsDTO();
        extensions.setState("IN_SERVICE");
        sliceProfileItem.setExtensions(extensions);

        PlmnInfoListDTO plmnInfo = new PlmnInfoListDTO();
        PlmnIdDTO plmnId = new PlmnIdDTO();
        plmnId.setMcc("330");
        plmnId.setMnc("220");
        plmnInfo.setPLMNId(plmnId);

        SnssaiDTO snssai = new SnssaiDTO();
        plmnInfo.setSNSSAI(snssai);
        sliceProfileItem.setPLMNInfoList(List.of(plmnInfo));

        RanSliceSubnetProfileDTO ranProfile = new RanSliceSubnetProfileDTO();
        ranProfile.setCoverageAreaTAList(Arrays.asList(1, 2));
        ranProfile.setResourceSharingLevel("shared");

        switch (subnetId) {
            case "9090d36f-6af5-4cfd-8bda-7a3c88fa82fa":
                sliceProfileItem.setSliceProfileId("2f1ca17d-5c44-4355-bfed-e9800a2996c1");
                snssai.setSst(1);
                snssai.setSd("000001");
                break;
            case "9090d36f-6af5-4cfd-8bda-7a3c88fa82fb":
                sliceProfileItem.setSliceProfileId("2f1ca17d-5c44-4355-bfed-e9800a2996c2");
                snssai.setSst(1);
                snssai.setSd("000002");
                ranProfile.setRRU_PrbDl(1024);
                ranProfile.setRRU_PrbUl(3096);
                break;
            case "9090d36f-6af5-4cfd-8bda-7a3c88fa82fc":
                sliceProfileItem.setSliceProfileId("2f1ca17d-5c44-4355-bfed-e9800a2996c3");
                snssai.setSst(2);
                snssai.setSd("000003");
                break;
            case "9090d36f-6af5-4cfd-8bda-7a3c88fa82fd":
                sliceProfileItem.setSliceProfileId("2f1ca17d-5c44-4355-bfed-e9800a2996c4");
                snssai.setSst(2);
                snssai.setSd("000004");
                ranProfile.setRRU_PrbDl(256);
                ranProfile.setRRU_PrbUl(512);
                break;
            case "9090d36f-6af5-4cfd-8bda-7a3c88fa82fe":
                sliceProfileItem.setSliceProfileId("2f1ca17d-5c44-4355-bfed-e9800a2996c5");
                snssai.setSst(3);
                snssai.setSd("000005");
                break;
            case "9090d36f-6af5-4cfd-8bda-7a3c88fa82ff":
                sliceProfileItem.setSliceProfileId("2f1ca17d-5c44-4355-bfed-e9800a2996c6");
                snssai.setSst(1);
                snssai.setSd("000006");
                ranProfile.setRRU_PrbDl(2048);
                ranProfile.setRRU_PrbUl(4096);
                break;
            default:
                return ResponseEntity.notFound().build();
        }

        sliceProfileItem.setRANSliceSubnetProfile(ranProfile);
        attributes.setSliceProfileList(List.of(sliceProfileItem));
        responseDto.setAttributes(attributes);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * Modifies an existing Network Slice Subnet with the provided profile.
     *
     * @param subnetId The unique identifier of the Network Slice Subnet to be modified.
     * @param updatedSliceProfile The DTO containing the updated characteristics for the Network Slice Subnet.
     * @return A ResponseEntity with HTTP status 200 (OK) and the updated profile,
     *         or 204 (No Content) if no content is returned on success.
     */
    @PutMapping("/NetworkSliceSubnets/{subnetId}")
    public ResponseEntity<SliceProfileDTO> modifyNetworkSliceSubnet(@PathVariable String subnetId, @Valid @RequestBody NetworkSliceSubnetDTO updatedSliceProfile) {
        try {
            String updatedSliceProfileJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(updatedSliceProfile);
            logger.info("Modification request received for subnetId: {} with data:\\n {}",  subnetId, updatedSliceProfileJson);
        } catch (Exception e) {
            logger.error("Error converting updatedSliceProfile to JSON: {}", e.getMessage());
        }
        // Assume modification is successful and return the updated profile (placeholder)
        SliceProfileDTO updatedProfile = new SliceProfileDTO(); // In a real scenario, this would be populated
        return ResponseEntity.ok(updatedProfile);
    }
}

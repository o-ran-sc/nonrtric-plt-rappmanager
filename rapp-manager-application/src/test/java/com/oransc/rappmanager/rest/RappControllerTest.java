package com.oransc.rappmanager.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oransc.rappmanager.acm.service.AcmDeployer;
import com.oransc.rappmanager.models.rapp.PrimeOrder;
import com.oransc.rappmanager.models.rapp.Rapp;
import com.oransc.rappmanager.models.rapp.RappPrimeOrder;
import com.oransc.rappmanager.models.rapp.RappState;
import com.oransc.rappmanager.models.cache.RappCacheService;
import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class RappControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RappCacheService rappCacheService;

    @MockBean
    AcmDeployer acmDeployer;

    private final String validRappFile = "valid-rapp-package.csar";

    private final String invalidRappFile = "invalid-rapp-package.csar";
    private final String validCsarFileLocation = "src/test/resources/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGetRapps() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps")).andExpect(status().isOk());
    }

    @Test
    void testGetRapp() throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        rappCacheService.putRapp(rapp);
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}", rappId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rappId").value(rappId.toString()))
                .andExpect(jsonPath("$.state").value(RappState.COMMISSIONED.name()));
    }

    @Test
    void testGetInvalidRapp() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/rapps/{rapp_id}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateRapp() throws Exception {
        String rappCsarPath = validCsarFileLocation + File.separator + validRappFile;
        MockMultipartFile multipartFile =
                new MockMultipartFile("file", validRappFile, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}", UUID.randomUUID()).file(multipartFile))
                .andExpect(status().isAccepted());
    }

    @Test
    void testCreateInvalidRapp() throws Exception {
        String rappCsarPath = validCsarFileLocation + File.separator + invalidRappFile;
        MockMultipartFile multipartFile =
                new MockMultipartFile("file", invalidRappFile, ContentType.MULTIPART_FORM_DATA.getMimeType(),
                        new FileInputStream(rappCsarPath));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/rapps/{rapp_id}", UUID.randomUUID()).file(multipartFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPrimeRapp() throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        rappCacheService.putRapp(rapp);
        when(acmDeployer.primeRapp(any())).thenReturn(true);
        RappPrimeOrder rappPrimeOrder = new RappPrimeOrder();
        rappPrimeOrder.setPrimeOrder(PrimeOrder.PRIME);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}", rappId).contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappPrimeOrder))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = PrimeOrder.class, names = {"PRIME", "DEPRIME"})
    void testPrimeRappFailure(PrimeOrder primeOrder) throws Exception {
        RappPrimeOrder rappPrimeOrder = new RappPrimeOrder();
        rappPrimeOrder.setPrimeOrder(primeOrder);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappPrimeOrder)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeprimeRapp() throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.PRIMED).build();
        rappCacheService.putRapp(rapp);
        when(acmDeployer.deprimeRapp(any())).thenReturn(true);
        RappPrimeOrder rappPrimeOrder = new RappPrimeOrder();
        rappPrimeOrder.setPrimeOrder(PrimeOrder.DEPRIME);
        mockMvc.perform(MockMvcRequestBuilders.put("/rapps/{rapp_id}", rappId).contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rappPrimeOrder))).andExpect(status().isOk());
    }

    @Test
    void testDeleteRapp() throws Exception {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).packageName(validRappFile)
                            .packageLocation(validCsarFileLocation).state(RappState.COMMISSIONED).build();
        rappCacheService.putRapp(rapp);
        mockMvc.perform(MockMvcRequestBuilders.delete("/rapps/{rapp_id}", rappId)).andExpect(status().isOk());
    }

    @Test
    void testDeleteRappFailure() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/rapps/{rapp_id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

package com.oransc.rappmanager.models.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.oransc.rappmanager.models.rapp.Rapp;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {BeanTestConfiguration.class, RappCacheService.class})
class RappCacheServiceTest {

    @Autowired
    RappCacheService rappCacheService;


    @Test
    void testPutRapp() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).build();
        rappCacheService.putRapp(rapp);
        assertNotNull(rappCacheService.getRapp(String.valueOf(rappId)).get());
        assertEquals(rappCacheService.getRapp(String.valueOf(rappId)).get().getRappId(), rappId);
        rappCacheService.deleteRapp(rapp);
    }

    @Test
    void testGetRapps() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).build();
        rappCacheService.putRapp(rapp);
        assertNotNull(rappCacheService.getAllRapp());
        assertThat(rappCacheService.getAllRapp()).hasSize(1);
        rappCacheService.deleteRapp(rapp);
    }

    @Test
    void testGetRappsEmpty() {
        assertNotNull(rappCacheService.getAllRapp());
        assertThat(rappCacheService.getAllRapp()).isEmpty();
    }

    @Test
    void testDeleteRapp() {
        UUID rappId = UUID.randomUUID();
        Rapp rapp = Rapp.builder().rappId(rappId).name(String.valueOf(rappId)).build();
        rappCacheService.putRapp(rapp);
        assertEquals(rappCacheService.getRapp(String.valueOf(rappId)).get().getRappId(), rappId);
        rappCacheService.deleteRapp(rapp);
        assertThat(rappCacheService.getRapp(String.valueOf(rappId))).isEmpty();
    }
}

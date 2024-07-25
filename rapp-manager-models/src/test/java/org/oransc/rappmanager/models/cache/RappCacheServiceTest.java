/*
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023-2024 OpenInfra Foundation Europe. All rights reserved.
 * ===============================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END========================================================================
 */

package org.oransc.rappmanager.models.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.oransc.rappmanager.models.BeanTestConfiguration;
import org.oransc.rappmanager.models.rapp.Rapp;
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

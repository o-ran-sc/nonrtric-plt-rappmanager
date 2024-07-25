/*-
 * ============LICENSE_START======================================================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
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

import org.oransc.rappmanager.models.rapp.Rapp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RappCacheService {

    private static final String RAPP_CACHE = "rapp-cache";
    private final CacheManager cacheManager;

    public Collection<Rapp> getAllRapp() {
        Cache cache = cacheManager.getCache(RAPP_CACHE);
        if(cache != null) {
            Map<String, Rapp> nativeCache = (Map<String, Rapp>) cache.getNativeCache();
            return nativeCache.values();
        }
        return List.of();
    }

    public Optional<Rapp> getRapp(String rappId) {
        return Optional.ofNullable(cacheManager.getCache(RAPP_CACHE)).map(cache -> cache.get(rappId, Rapp.class));
    }

    public void putRapp(Rapp rapp) {
        final Cache cache = cacheManager.getCache(RAPP_CACHE);
        if (cache != null) {
            cache.put(rapp.getName(), rapp);
        }
    }

    public void deleteRapp(Rapp rapp) {
        final Cache cache = cacheManager.getCache(RAPP_CACHE);
        if (cache != null) {
            cache.evict(rapp.getName());
        }
    }
}

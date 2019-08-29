/*
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
 */

package com.datasphere.server.domain.notebook.connector;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.util.Optional;

/**
 * Created by aladin on 2019. 8. 22..
 */
public interface HttpRepositoryAction {
    <T> Optional<T> call(String url, HttpMethod method, HttpEntity<?> entity, Class<T> clazz, boolean isTransient);
    <T> Optional<T> call(String url, HttpMethod method, HttpEntity<?> entity, Class<T> clazz);
}
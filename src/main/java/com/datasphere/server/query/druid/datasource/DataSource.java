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

package com.datasphere.server.query.druid.datasource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by aladin on 2019. 5. 22..
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TableDataSource.class, name = "table"),
    @JsonSubTypes.Type(value = QueryDataSource.class, name = "query"),
    @JsonSubTypes.Type(value = UnionDataSource.class, name = "union"),
    @JsonSubTypes.Type(value = RegexDataSource.class, name = "regex"),
    @JsonSubTypes.Type(value = ViewDataSource.class, name = "view")
})
public interface DataSource {
}


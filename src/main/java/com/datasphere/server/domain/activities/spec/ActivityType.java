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

package com.datasphere.server.domain.activities.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * https://www.w3.org/TR/activitystreams-vocabulary/#activity-types
 */
public enum ActivityType {
  @JsonProperty("Login")
  LOGIN,
  @JsonProperty("View")
  VIEW,
  @JsonProperty("Join")
  JOIN,
  @JsonProperty("Create")
  CREATE,
  @JsonProperty("Update")
  UPDATE,
  @JsonProperty("Delete")
  DELETE,
  @JsonProperty("Link")
  LINK,
  @JsonProperty("Accept")
  ACCEPT,
  @JsonProperty("Block")
  BLOCK,
  @JsonProperty("None")
  NONE
}

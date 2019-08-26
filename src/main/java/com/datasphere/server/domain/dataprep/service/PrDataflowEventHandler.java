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

package com.datasphere.server.domain.dataprep.service;

import com.datasphere.server.domain.dataprep.entity.PrDataflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.core.annotation.*;

@RepositoryEventHandler(PrDataflow.class)
public class PrDataflowEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrDataflowEventHandler.class);

    @HandleBeforeCreate
    public void beforeCreate(PrDataflow dataflow) {
        // LOGGER.debug(dataflow.toString());
    }

    @HandleAfterCreate
    public void afterCreate(PrDataflow dataflow) {
        // LOGGER.debug(dataflow.toString());
    }

    @HandleBeforeSave
    public void beforeSave(PrDataflow dataflow) {
        // LOGGER.debug(dataflow.toString());
    }

    @HandleAfterSave
    public void afterSave(PrDataflow dataflow) {
        // LOGGER.debug(dataflow.toString());
    }
}


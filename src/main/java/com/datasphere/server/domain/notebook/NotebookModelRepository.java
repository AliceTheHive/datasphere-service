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

package com.datasphere.server.domain.notebook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Created by james on 2017. 7. 13..
 */
@RepositoryRestResource(path = "nbmodels", excerptProjection = NotebookModelProjections.DefaultProjection.class)
public interface NotebookModelRepository extends JpaRepository<NotebookModel, String>,
        JpaSpecificationExecutor<NotebookModel>,
        QueryDslPredicateExecutor<NotebookModel> {
//
//    @Transactional
//    void deleteByIdIn(List<String> ids);
//
//    List<NotebookModel> findByIdIn(List<String> ids);
}

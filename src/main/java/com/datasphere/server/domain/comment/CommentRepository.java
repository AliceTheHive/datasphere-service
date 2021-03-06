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

package com.datasphere.server.domain.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.datasphere.server.common.entity.DomainType;

/**
 * Created by aladin on 2019. 12. 28..
 */
@RepositoryRestResource(exported = false, path = "comments", itemResourceRel = "comment"
    , collectionResourceRel = "comments", excerptProjection = CommentProjections.DefaultProjection.class)
public interface CommentRepository extends JpaRepository<Comment, Long>,
                                          QuerydslPredicateExecutor<Comment> {

  Page<Comment> findByDomainTypeAndDomainIdOrderByCreatedTimeDesc(DomainType domainType,
                                                                  String domainId,
                                                                  Pageable pageable);

  Long countByDomainTypeAndDomainId(DomainType domainType, String domainId);
}

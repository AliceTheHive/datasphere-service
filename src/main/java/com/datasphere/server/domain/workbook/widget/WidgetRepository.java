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

package com.datasphere.server.domain.workbook.widget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import com.datasphere.server.domain.workbook.DashBoard;

/**
 * Created by kyungtaak on 2016. 1. 7..
 */
@RepositoryRestResource(path = "widgets", collectionResourceRel = "widgets", itemResourceRel = "widget")
public interface WidgetRepository extends JpaRepository<Widget, String>,
                                          QueryDslPredicateExecutor<Widget> {

  @RestResource(exported = false)
  Page<Widget> findByDashBoardEquals(DashBoard dashBoard, Pageable pageable);
}

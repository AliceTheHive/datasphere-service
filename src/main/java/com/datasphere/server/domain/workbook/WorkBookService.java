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

package com.datasphere.server.domain.workbook;

import com.google.common.collect.Lists;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.datasphere.engine.datasource.DataSource;
import com.datasphere.engine.datasource.DataSourceRepository;
import com.datasphere.engine.datasource.QDataSource;
import com.datasphere.server.domain.workspace.BookRepository;
import com.datasphere.server.domain.workspace.WorkspaceRepository;
import com.datasphere.server.domain.workspace.WorkspaceService;


@Component
@Transactional(readOnly = true)
public class WorkBookService {

  @Autowired
  BookRepository bookRepository;
  // 工作表容器
  @Autowired
  WorkBookRepository workBookRepository;
  // 数据源容器
  @Autowired
  DataSourceRepository dataSourceRepository;
  // 工作空间容器
  @Autowired
  WorkspaceRepository workspaceRepository;
  // 仪表盘服务
  @Autowired
  DashBoardService dashBoardService;
  // 工作空间服务
  @Autowired
  WorkspaceService workspaceService;

  public List<DataSource> getAllDataSourceInDashboard(String workbookId) {

    QDataSource dataSource = QDataSource.dataSource;

    QWorkBook workBook = QWorkBook.workBook;

    BooleanExpression dataSourceIn = dataSource.id
        .in(JPAExpressions.select(workBook.dashBoards.any().dataSources.any().id)
                          .from(workBook)
                          .innerJoin(workBook.dashBoards)
                          .where(workBook.id.eq(workbookId)));

    BooleanBuilder booleanBuilder = new BooleanBuilder();
    booleanBuilder.and(dataSourceIn);

    return Lists.newArrayList(dataSourceRepository.findAll(booleanBuilder,
                                                           new Sort(new Sort.Order(Sort.Direction.ASC, "name"))));
  }
}

/*
 * Copyright 2019, Huahuidata, Inc.
 * DataSphere is licensed under the Mulan PSL v1.
 * You can use this software according to the terms and conditions of the Mulan PSL v1.
 * You may obtain a copy of Mulan PSL v1 at:
 * http://license.coscl.org.cn/MulanPSL
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 * See the Mulan PSL v1 for more details.
 */

package com.datasphere.server.domain.workbook;

import static com.datasphere.server.config.ApiResourceConfig.REDIRECT_PATH_URL;

import java.util.ArrayList;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.WebUtils;

import com.datasphere.server.common.exception.MetatronException;
import com.datasphere.server.datasource.data.DataSourceValidator;
import com.datasphere.server.datasource.data.SearchQueryRequest;
import com.datasphere.server.datasource.data.result.ObjectResultFormat;
import com.datasphere.server.domain.engine.EngineQueryService;
import com.datasphere.server.domain.workbook.configurations.BoardConfiguration;
import com.datasphere.server.domain.workbook.configurations.Limit;
import com.datasphere.server.domain.workbook.configurations.datasource.DataSource;
import com.datasphere.server.domain.workbook.configurations.datasource.MultiDataSource;
import com.datasphere.server.domain.workbook.widget.QWidget;
import com.datasphere.server.domain.workbook.widget.Widget;
import com.datasphere.server.domain.workbook.widget.WidgetRepository;
import com.datasphere.server.util.AuthUtils;
import com.querydsl.core.BooleanBuilder;

@RepositoryRestController
public class DashBoardController {

  @Autowired
  DashboardRepository dashboardRepository;

  @Autowired
  WidgetRepository widgetRepository;

  @Autowired
  DashBoardService dashBoardService;

  @Autowired
  EngineQueryService engineQueryService;

  @Autowired
  DataSourceValidator dataSourceValidator;

  @Autowired
  PagedResourcesAssembler pagedResourcesAssembler;

  @RequestMapping(path = "/dashboards/{dashboardId}/widgets", method = RequestMethod.GET)
  public @ResponseBody ResponseEntity<?> findByWidgetInDashboard(@PathVariable("dashboardId") String dashboardId,
                                                       @RequestParam(value = "widgetType", required = false) String widgetType,
                                                       Pageable pageable,
                                                       PersistentEntityResourceAssembler resourceAssembler) {

    if(StringUtils.isNotEmpty(widgetType) && !Widget.SEARCHABLE_WIDGETS.contains(widgetType.toLowerCase())) {
      throw new IllegalArgumentException("Invalid widget type. choose " + Widget.SEARCHABLE_WIDGETS);
    }

    DashBoard dashBoard = dashboardRepository.findById(dashboardId).get();
    if(dashBoard == null) {
      throw new ResourceNotFoundException("Dashboard(" + dashboardId + ") not found");
    }

    QWidget qWidget = QWidget.widget;
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(qWidget.dashBoard.id.eq(dashboardId));

    if(StringUtils.isNotEmpty(widgetType)) {
      builder.and(qWidget.type.eq(widgetType));
    }

    Page<Widget> widgets = widgetRepository.findAll(builder, pageable);

    return ResponseEntity.ok(pagedResourcesAssembler.toResource(widgets, resourceAssembler));
  }

  @RequestMapping(path = "/dashboards/{dashboardId}/copy", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> copyDashboard(@PathVariable("dashboardId") String dashboardId,
                                                       PersistentEntityResourceAssembler resourceAssembler) {

    DashBoard dashBoard = dashboardRepository.findById(dashboardId).get();
    if(dashBoard == null) {
      throw new ResourceNotFoundException("Dashboard(" + dashboardId + ") not found");
    }

    DashBoard copiedDashboard = dashBoardService.copy(dashBoard, dashBoard.getWorkBook(), true);

    return ResponseEntity.ok(resourceAssembler.toResource(copiedDashboard));
  }


  /**
   * Query mapped data in dashboards, within NoteBook module
   *
   * @param id
   * @param request
   * @return
   */
  @RequestMapping(path = "/dashboards/{id}/data", method = RequestMethod.POST)
  public @ResponseBody
  ResponseEntity<?> getDataFromDatasource(@PathVariable("id") String id,
                                          @RequestParam(value = "limit", required = false) Integer limit,
                                          @RequestBody(required = false) SearchQueryRequest request) {

    DashBoard dashBoard = dashboardRepository.findById(id).get();
    if (dashBoard == null) {
      throw new ResourceNotFoundException(id);
    }

    if(request == null) {
      request = new SearchQueryRequest();
    }

    BoardConfiguration configuration = dashBoard.getConfigurationObject();
    if(configuration == null || configuration.getDataSource() == null) {
      throw new MetatronException("Configuration empty.");
    }

    // For multidata sources, process the first data source
    DataSource targetDataSource = null;
    if(configuration.getDataSource() instanceof MultiDataSource) {
      targetDataSource = ((MultiDataSource) configuration.getDataSource()).getDataSources().get(0);
    } else {
      targetDataSource = configuration.getDataSource();
    }

    request.setDataSource(targetDataSource);
    request.setUserFields(configuration.getUserDefinedFields());

    if(request.getResultFormat() == null) {
      ObjectResultFormat resultFormat = new ObjectResultFormat();
      resultFormat.setRequest(request);
      request.setResultFormat(resultFormat);
    }

    if(CollectionUtils.isEmpty(request.getProjections())) {
      request.setProjections(new ArrayList<>());
    }

    if(request.getLimits() == null) {
      if(limit == null) {
        limit = 1000;
      } else if(limit > 1000000) {
        limit = 1000000;
      }
      request.setLimits(new Limit(limit));
    }

    dataSourceValidator.validateQuery(request.getDataSource());

    return ResponseEntity.ok(engineQueryService.search(request));
  }

  /**
   * Screen Pass for Dashboard Embed
   *
   * @param id
   * @return
   */
  @RequestMapping(path = "/dashboards/{id}/embed", method = RequestMethod.GET, produces = { MediaType.TEXT_HTML_VALUE })
  public String getEmbedDashBoardView(@PathVariable("id") String id,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {

    DashBoard dashBoard = dashboardRepository.findById(id).get();
    if (dashBoard == null) {
      throw new ResourceNotFoundException(id);
    }

    if(WebUtils.getCookie(request, "LOGIN_TOKEN") == null) {

      String authorization = request.getHeader("Authorization");
      String[] splitedAuth = StringUtils.split(authorization, " ");

      Cookie cookie = new Cookie("LOGIN_TOKEN", splitedAuth[1]);
      cookie.setPath("/");
      cookie.setMaxAge(60*60*24) ;
      response.addCookie(cookie);

      cookie = new Cookie("LOGIN_TOKEN_TYPE", splitedAuth[0]);
      cookie.setPath("/");
      cookie.setMaxAge(60*60*24) ;
      response.addCookie(cookie);

      cookie = new Cookie("REFRESH_LOGIN_TOKEN", "");
      cookie.setPath("/");
      cookie.setMaxAge(60*60*24);
      response.addCookie(cookie);

      cookie = new Cookie("LOGIN_USER_ID", AuthUtils.getAuthUserName());
      cookie.setPath("/");
      cookie.setMaxAge(60*60*24) ;
      response.addCookie(cookie);
    }

    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(REDIRECT_PATH_URL);
    pathBuilder.append("/dashboard/").append(id);

    return pathBuilder.toString();
  }
}

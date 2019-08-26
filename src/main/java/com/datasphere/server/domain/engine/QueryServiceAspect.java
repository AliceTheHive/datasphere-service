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

package com.datasphere.server.domain.engine;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import com.datasphere.server.common.CommonLocalVariable;
import com.datasphere.server.common.IdGenerator;
import com.datasphere.server.domain.datasource.DataSourceQueryHistory;
import com.datasphere.server.domain.datasource.DataSourceQueryHistoryRepository;
import com.datasphere.server.domain.datasource.QueryHistoryTeller;
import com.datasphere.server.domain.datasource.data.QueryRequest;

/**
 * Created by kyungtaak on 2016. 8. 30..
 */
@Aspect
@Order(9)
@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class QueryServiceAspect {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryServiceAspect.class);

  @Autowired
  DataSourceQueryHistoryRepository repository;

  @Pointcut("within(com.datasphere.server.domain.engine.QueryService+)")
  public void service() {
  }

  @Pointcut("args(queryRequest)")
  public void serviceArgs(QueryRequest queryRequest) {
  }

  @Pointcut("execution(public * com.datasphere.server.domain.engine.DruidEngineRepository.*(..))")
  public void repository() {
  }

  @Around("service() && serviceArgs(queryRequest)")
  public Object queryProcessing(ProceedingJoinPoint pjp, QueryRequest queryRequest) throws Throwable {

    // Init QueryHistory Entity
    DataSourceQueryHistory queryHistory = QueryHistoryTeller.getHistory();
    // Generate queryId

    // Set Query Id
    String queryId = queryRequest.getContextValue(QueryRequest.CONTEXT_QUERY_ID);
    if(StringUtils.isEmpty(queryId)) {
      queryId = IdGenerator.queryId();
    }

    LOGGER.info("[{}] Starting query process from {}", queryId,
                queryRequest.getContextValue(QueryRequest.CONTEXT_ROUTE_URI));

//    QueryRequest queryRequest = (QueryRequest) pjp.getArgs()[0];
    queryHistory.initRequest(queryRequest);

    CommonLocalVariable.setQueryId(queryId);
    queryHistory.setEngineQueryId(queryId);

    StopWatch stopWatch = new StopWatch();
    stopWatch.start("Query Time");
    Object retVal;
    try {
      retVal = pjp.proceed();
      queryHistory.setSucceed(true);
    } catch (Throwable ex) {
      queryHistory.setSucceed(false);
      queryHistory.setMessage(ex.getMessage());
      throw ex;
    } finally {
      stopWatch.stop();
      queryHistory.setElapsedTime(stopWatch.getTotalTimeMillis());

      repository.save(QueryHistoryTeller.getHistory());
      LOGGER.info("[{}] Saved history - Succeed: {}, {}", queryId, queryHistory.getSucceed(), queryHistory.toString());

      // for MultiThread
      QueryHistoryTeller.remove();
    }

    return retVal;
  }

  @Around("repository()")
  public Object engineQueryProcessing(ProceedingJoinPoint pjp) throws Throwable {

    String queryId = CommonLocalVariable.getQueryId();

    StopWatch stopWatch = new StopWatch();
    stopWatch.start("Engine Query Time");
    Object retVal;
    try {
      retVal = pjp.proceed();
    } catch (Throwable ex) {
      throw ex;
    } finally {
      stopWatch.stop();

      if(queryId != null) {
        QueryHistoryTeller.setEngineElapsedTime(stopWatch.getTotalTimeMillis());
      } else {
        queryId = "UNKNOWN";
      }

      LOGGER.info("[{}] Elapsed Engine Query Time : {}", queryId, stopWatch.getTotalTimeMillis());
    }

    return retVal;
  }


//  @AfterReturning(pointcut = "service()", returning = "result")
//  public void afterReturning(JoinPoint joinPoint, Object result) {
//    DataSourceQueryHistory history = QueryHistoryTeller.getHistory();
//    history.setSucceed(true);
//
//    repository.save(history);
//    LOGGER.info("Saved history - Succeed query: {}", history.toString());
//
//    // for MultiThread
//    QueryHistoryTeller.remove();
//  }
//
//  @AfterThrowing(pointcut = "service()", throwing = "ex")
//  public void afterThrowing(JoinPoint joinPoint, Throwable ex) {
//    DataSourceQueryHistory history = QueryHistoryTeller.getHistory();
//    history.setSucceed(false);
//    history.setMessage(ex.getMessage());
//
//    repository.save(history);
//    LOGGER.info("Save history - Failed query: {}", history.toString());
//
//    QueryHistoryTeller.remove();
//  }
}

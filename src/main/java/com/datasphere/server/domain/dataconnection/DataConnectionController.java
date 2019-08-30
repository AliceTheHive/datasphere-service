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

package com.datasphere.server.domain.dataconnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.datasphere.server.common.criteria.ListCriterion;
import com.datasphere.server.common.criteria.ListFilter;
import com.datasphere.server.common.entity.SearchParamValidator;
import com.datasphere.server.common.exception.BadRequestException;
import com.datasphere.server.common.exception.ResourceNotFoundException;
import com.datasphere.server.connections.jdbc.exception.JdbcDataConnectionErrorCodes;
import com.datasphere.server.connections.jdbc.exception.JdbcDataConnectionException;
import com.datasphere.server.datasource.DataSourceProperties;
import com.datasphere.server.datasource.Field;
import com.datasphere.server.datasource.connection.jdbc.HiveTableInformation;
import com.datasphere.server.datasource.connection.jdbc.JdbcConnectionService;
import com.datasphere.server.datasource.connection.jdbc.JdbcQueryResultResponse;
import com.datasphere.server.datasource.ingestion.file.FileFormat;
import com.datasphere.server.datasource.ingestion.jdbc.JdbcIngestionInfo;
import com.datasphere.server.domain.dataconnection.accessor.HiveDataAccessor;
import com.datasphere.server.domain.dataconnection.accessor.HiveDataAccessorUsingMetastore;
import com.datasphere.server.domain.dataconnection.dialect.HiveDialect;
import com.datasphere.server.domain.engine.EngineProperties;
import com.datasphere.server.domain.mdm.Metadata;
import com.datasphere.server.domain.mdm.source.MetadataSource;
import com.datasphere.server.domain.mdm.source.MetadataSourceRepository;
import com.datasphere.server.domain.storage.StorageProperties;
import com.datasphere.server.domain.workbench.Workbench;
import com.datasphere.server.domain.workbench.WorkbenchRepository;
import com.datasphere.server.domain.workbench.util.WorkbenchDataSourceManager;
import com.datasphere.server.util.PolarisUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.querydsl.core.types.Predicate;

/**
 * Created by aladin on 2019. 6. 10..
 */
@RepositoryRestController
public class DataConnectionController {

  private static Logger LOGGER = LoggerFactory.getLogger(DataConnectionController.class);

  @Autowired
  JdbcConnectionService connectionService;

  @Autowired
  DataConnectionFilterService connectionFilterService;

  @Autowired
  DataConnectionRepository connectionRepository;

  @Autowired
  WorkbenchRepository workbenchRepository;

  @Autowired
  DataSourceProperties dataSourceProperties;

  @Autowired
  EngineProperties engineProperties;

  @Autowired
  PagedResourcesAssembler pagedResourcesAssembler;

  @Autowired
  MetadataSourceRepository metadataSourceRepository;
  
  @Autowired(required = false)
  StorageProperties storageProperties;

  @Autowired
  WorkbenchDataSourceManager workbenchDataSourceManager;

  /**
   * 서비스에서 이용가능한 JDBC 종류 전달
   *
   * @return
   */
  @RequestMapping(value = "/connections/available", method = RequestMethod.GET,  produces = "application/json")
  public @ResponseBody ResponseEntity<?> available() {
    return ResponseEntity.ok(dataSourceProperties.getConnections());
  }

  /**
   * Connection 목록 조회
   * @param name
   * @param implementor
   * @param searchDateBy
   * @param from
   * @param to
   * @param pageable
   * @param resourceAssembler
   * @return
   */
  @RequestMapping(value = "/connections", method = RequestMethod.GET,  produces = "application/json")
  public @ResponseBody ResponseEntity<?> findConnections(
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "implementor", required = false) String implementor,
      @RequestParam(value = "authenticationType", required = false) String authenticationType,
      @RequestParam(value = "searchDateBy", required = false) String searchDateBy,
      @RequestParam(value = "from", required = false)
      @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) DateTime from,
      @RequestParam(value = "to", required = false)
      @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) DateTime to,
      Pageable pageable, PersistentEntityResourceAssembler resourceAssembler) {

    LOGGER.debug("name = {}", name);
    LOGGER.debug("implementor = {}", implementor);
    LOGGER.debug("authenticationType = {}", authenticationType);
    LOGGER.debug("searchDateBy = {}", searchDateBy);
    LOGGER.debug("from = {}", from);
    LOGGER.debug("to = {}", to);
    LOGGER.debug("pageable = {}", pageable);

    // Validate UsageScope
    DataConnection.AuthenticationType authenticationTypeValue = null;
    if(StringUtils.isNotEmpty(authenticationType)){
      authenticationTypeValue = SearchParamValidator
              .enumUpperValue(DataConnection.AuthenticationType.class, authenticationType, "authenticationType");
    }

    // Validate searchByTime
    SearchParamValidator.range(searchDateBy, from, to);

    // Get Predicate
    Predicate searchPredicated = DataConnectionPredicate
        .searchList(name, implementor, searchDateBy, from, to, authenticationTypeValue);

    // 기본 정렬 조건 셋팅
    if(pageable.getSort() == null || !pageable.getSort().iterator().hasNext()) {
      pageable = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(),
                                 new Sort(Sort.Direction.ASC, "createdTime", "name"));
    }
    Page<DataConnection> connections = connectionRepository.findAll(searchPredicated, pageable);

    return ResponseEntity.ok(this.pagedResourcesAssembler.toResource(connections, resourceAssembler));
  }

  @RequestMapping(value = "/connections/query/check", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> queryForConnection(@RequestBody ConnectionRequest checkRequest) throws JdbcDataConnectionException {

    // 추가 유효성 체크
    Map<String, Object> resultMap = connectionService.checkConnection(checkRequest.getConnection());

    return ResponseEntity.ok(resultMap);
  }

  @RequestMapping(value = "/connections/query/databases", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> queryForListOfDatabases(@RequestBody ConnectionRequest checkRequest,
                                                                 Pageable pageable) throws SQLException {
    return ResponseEntity.ok(
        connectionService.getDatabases(checkRequest.getConnection(), null, pageable)
    );
  }

  @RequestMapping(value = "/connections/query/hive/databases", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> queryForListOfHiveDatabases(Pageable pageable) throws SQLException, ResourceNotFoundException {

    StorageProperties.StageDBConnection stageDBConnection = getStageDBConnection(storageProperties);

    DataConnection hiveConnection = stageDBConnection.getJdbcDataConnection();

    return ResponseEntity.ok(
            connectionService.getDatabases(hiveConnection, null, pageable)
    );
  }

  @RequestMapping(value = "/connections/query/tables", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> queryForListOfTables(@RequestBody ConnectionRequest checkRequest,
                                                              Pageable pageable) throws SQLException {

    return ResponseEntity.ok(
        connectionService.getTableNames(checkRequest.getConnection(), checkRequest.getDatabase(), checkRequest.getTable(), pageable)
    );
  }

  @RequestMapping(value = "/connections/query/hive/tables", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> queryForListOfHiveTables(@RequestBody ConnectionRequest checkRequest,
                                                              Pageable pageable) throws BadRequestException, SQLException, ResourceNotFoundException {

    //유효성 체크
    SearchParamValidator.checkNull(checkRequest.getDatabase(), "database");

    StorageProperties.StageDBConnection stageDBConnection = getStageDBConnection(storageProperties);

    DataConnection hiveConnection = stageDBConnection.getJdbcDataConnection();

    return ResponseEntity.ok(
            connectionService.getTableNames(hiveConnection, checkRequest.getDatabase(), pageable)
    );
  }

  @RequestMapping(value = "/connections/query/data", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> queryBySelect(@RequestBody ConnectionRequest checkRequest,
                                                       @RequestParam(required = false, defaultValue = "50") int limit,
                                                       @RequestParam(required = false) boolean extractColumnName) throws BadRequestException, JdbcDataConnectionException {

    // 추가 유효성 체크
    SearchParamValidator.checkNull(checkRequest.getType(), "type");
    SearchParamValidator.checkNull(checkRequest.getQuery(), "query");

    JdbcQueryResultResponse resultSet =
        connectionService.selectQueryForIngestion(checkRequest.getConnection(), checkRequest.getDatabase(),
                checkRequest.getType(),checkRequest.getQuery(), limit, extractColumnName);

    return ResponseEntity.ok(resultSet);
  }

  @RequestMapping(value = "/connections/query/hive/data", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> queryBySelectForHiveIngestion(@RequestBody ConnectionRequest checkRequest,
                                                                       @RequestParam(required = false, defaultValue = "50") int limit,
                                                                       @RequestParam(required = false) boolean extractColumnName) throws BadRequestException, JdbcDataConnectionException, ResourceNotFoundException {

    // 추가 유효성 체크
    SearchParamValidator.checkNull(checkRequest.getType(), "type");
    SearchParamValidator.checkNull(checkRequest.getQuery(), "query");
    SearchParamValidator.checkNull(checkRequest.getDatabase(), "database");

    StorageProperties.StageDBConnection stageDBConnection = getStageDBConnection(storageProperties);

    DataConnection hiveConnection = stageDBConnection.getJdbcDataConnection();

    if(checkRequest.getDatabase() != null && checkRequest.getType() == JdbcIngestionInfo.DataType.QUERY) {
      hiveConnection.setDatabase(checkRequest.getDatabase());
    }

    List<Field> partitionFieldsList = null;
    FileFormat fileFormat = null;
    //Partition 정보 ..
    if(checkRequest.getType() == JdbcIngestionInfo.DataType.TABLE){
      HiveDataAccessor jdbcDataAccessor = (HiveDataAccessor) DataConnectionHelper.getAccessor(hiveConnection);
      HiveTableInformation hiveTableInformation
          = jdbcDataAccessor.showHiveTableDescription(hiveConnection, null,
                                                      checkRequest.getDatabase(), checkRequest.getQuery(),
                                                      false);

      //Partition Field
      partitionFieldsList = hiveTableInformation.getPartitionFields();

      //File Format
      fileFormat = hiveTableInformation.getFileFormat();

      //when strict mode, requires hive metastore connection info
      if(stageDBConnection.isStrictMode() && !partitionFieldsList.isEmpty()){

        if(!HiveDialect.includeMetastoreInfo(hiveConnection)){
          throw new ResourceNotFoundException("StorageProperties.StageDBConnection's MetaStoreInfo");
        }

        //getting recent partition
        List<Map<String, Object>> partitionList
            = ((HiveDataAccessorUsingMetastore) jdbcDataAccessor).getPartitionList(checkRequest.getDatabase(),
                                                                                    checkRequest.getQuery());
        if(partitionList == null || partitionList.isEmpty()){
          throw new JdbcDataConnectionException(JdbcDataConnectionErrorCodes.STAGEDB_PREVIEW_TABLE_SQL_ERROR,
                                                "There is no partitions in table(" + checkRequest.getQuery() + ").");
        }

        Map<String, Object> recentPartition
                = PolarisUtils.partitionStringToMap(partitionList.get(0).get("PART_NAME").toString());
        checkRequest.setPartitions(Lists.newArrayList(recentPartition));
      }
    }

    JdbcQueryResultResponse resultSet =
            connectionService.selectQueryForIngestion(hiveConnection, checkRequest.getDatabase(),
                    checkRequest.getType(), checkRequest.getQuery(), checkRequest.getPartitions(), limit, extractColumnName);

    //Partition 정보 ..
    if(checkRequest.getType() == JdbcIngestionInfo.DataType.TABLE){

      //Partition Field
      resultSet.setPartitionFields(partitionFieldsList);

      //File Format
      resultSet.setFileFormat(fileFormat);
    }

    return ResponseEntity.ok(resultSet);
  }

  @RequestMapping(value = "/connections/query/information", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> queryForTableInfo(@RequestBody ConnectionRequest checkRequest) throws BadRequestException, SQLException {

    // 추가 유효성 체크
    SearchParamValidator.checkNull(checkRequest.getTable(), "table");
    SearchParamValidator.checkNull(checkRequest.getDatabase(), "database");

    DataConnection dataConnection = checkRequest.getConnection();

    Map<String, Object> tableInfoMap = connectionService.showTableDescription(dataConnection, null,
            checkRequest.getDatabase(), checkRequest.getTable());

    return ResponseEntity.ok(tableInfoMap);
  }

  @RequestMapping(value = "/connections/{connectionId}/databases", method = RequestMethod.GET)
  public @ResponseBody ResponseEntity<?> queryForListOfDatabasesByConnectionId(
          @PathVariable("connectionId") String connectionId,
          @RequestParam(required = false) String databaseName,
          @RequestParam(required = false) String webSocketId,
          @RequestParam(required = false) String loginUserId,
          Pageable pageable) throws ResourceNotFoundException, BadRequestException, SQLException {

    Optional<DataConnection> connectionInfo = connectionRepository.findById(connectionId);

    if(connectionInfo == null) {
      throw new ResourceNotFoundException(connectionId);
    }

    //userinfo, dialog required webSocketId
    if(connectionInfo.get().getAuthenticationType() != DataConnection.AuthenticationType.MANUAL){
      SearchParamValidator.checkNull(webSocketId, "webSocketId");
    }

    Connection connection = null;
    if(StringUtils.isNotEmpty(webSocketId)){
      connection = workbenchDataSourceManager.findDataSourceInfo(webSocketId).getPrimaryConnection();
    }

    Map<String, Object> findDatabases = connectionService.getDatabases(connectionInfo.get(), connection, databaseName, pageable);

    //moved filter for personal database logic to HiveDataAccessor..
    return ResponseEntity.ok(findDatabases);
  }

  @RequestMapping(value = "/connections/{connectionId}/databases/{databaseName}/tables", method = RequestMethod.GET,
                  produces = "application/json")
  public @ResponseBody ResponseEntity<?> queryForListOfTablesByConnectionIdAndDatabaseName(
          @PathVariable("connectionId") String connectionId,
          @PathVariable("databaseName") String databaseName,
          @RequestParam(required = false) String tableName,
          @RequestParam(required = false) String webSocketId,
          Pageable pageable) throws ResourceNotFoundException, BadRequestException, SQLException {

    Optional<DataConnection> dataConnection = connectionRepository.findById(connectionId);
    if(dataConnection == null) {
      throw new ResourceNotFoundException(connectionId);
    }

    //userinfo, dialog required webSocketId
    if(dataConnection.get().getAuthenticationType() != DataConnection.AuthenticationType.MANUAL){
      SearchParamValidator.checkNull(webSocketId, "webSocketId");
    }

    Connection connection = null;
    if(StringUtils.isNotEmpty(webSocketId)){
      connection = workbenchDataSourceManager.findDataSourceInfo(webSocketId).getPrimaryConnection();
    }

    return ResponseEntity.ok(
        connectionService.getTableNames(dataConnection.get(), databaseName, tableName, connection, pageable)
    );
  }

  @RequestMapping(value = "/connections/{connectionId}/databases/{databaseName}/tables/{tableName}/columns",
          method = RequestMethod.GET,  produces = "application/json")
  public @ResponseBody ResponseEntity<?> queryForListOfDatabasesByConnectionid(
          @PathVariable("connectionId") String connectionId,
          @PathVariable("databaseName") String databaseName,
          @PathVariable("tableName") String tableName,
          @RequestParam(required = false) String webSocketId,
          @RequestParam(required = false) String columnNamePattern,
          Pageable pageable) throws ResourceNotFoundException, BadRequestException, JdbcDataConnectionException {

    Optional<DataConnection> dataConnection = connectionRepository.findById(connectionId);
    if(dataConnection == null) {
      throw new ResourceNotFoundException("DataConnection(" + connectionId + ")");
    }

    //userinfo, dialog required webSocketId
    if(dataConnection.get().getAuthenticationType() != DataConnection.AuthenticationType.MANUAL){
      SearchParamValidator.checkNull(webSocketId, "webSocketId");
    }

    Connection connection = null;
    if(StringUtils.isNotEmpty(webSocketId)){
      connection = workbenchDataSourceManager.findDataSourceInfo(webSocketId).getPrimaryConnection();
    }

    Map<String, Object> columnsMap = connectionService.getTableColumns(dataConnection.get(), connection, databaseName,
                                                                        tableName, columnNamePattern, pageable);

    return ResponseEntity.ok(columnsMap);
  }

  @RequestMapping(value = "/connections/{connectionId}/databases/{databaseName}/tables/{tableName}/information",
          method = RequestMethod.GET,  produces = "application/json")
  public @ResponseBody ResponseEntity<?> getTableInformation(
          @PathVariable("connectionId") String connectionId,
          @PathVariable("databaseName") String databaseName,
          @PathVariable("tableName") String tableName,
          @RequestParam(required = false) String webSocketId) throws ResourceNotFoundException, BadRequestException, SQLException {

    Optional<DataConnection> dataConnection = connectionRepository.findById(connectionId);
    if(dataConnection == null) {
      throw new ResourceNotFoundException("DataConnection(" + connectionId + ")");
    }

    //userinfo, dialog required webSocketId
    if(dataConnection.get().getAuthenticationType() != DataConnection.AuthenticationType.MANUAL){
      SearchParamValidator.checkNull(webSocketId, "webSocketId");
    }

    Connection connection = null;
    if(StringUtils.isNotEmpty(webSocketId)){
      connection = workbenchDataSourceManager.findDataSourceInfo(webSocketId).getPrimaryConnection();
    }

    Map<String, Object> tableInfoMap = connectionService.showTableDescription(dataConnection.get(), connection,
                                                                              databaseName, tableName);

    return ResponseEntity.ok(tableInfoMap);
  }

  @RequestMapping(value = "/connections/{connectionId}/databases/{databaseName}/change",
          method = RequestMethod.POST,  produces = "application/json")
  public @ResponseBody ResponseEntity<?> changeDatabase(
          @PathVariable("connectionId") String connectionId,
          @PathVariable("databaseName") String databaseName,
          @RequestBody Map<String, String> requestBodyMap) throws ResourceNotFoundException, JdbcDataConnectionException {

    Optional<DataConnection> dataConnection = connectionRepository.findById(connectionId);
    if(dataConnection == null) {
      throw new ResourceNotFoundException("DataConnection(" + connectionId + ")");
    }

    String webSocketId = requestBodyMap.get("webSocketId");
    Connection connection = null;
    if(StringUtils.isNotEmpty(webSocketId)){
      connection = workbenchDataSourceManager.findDataSourceInfo(webSocketId).getPrimaryConnection();
    }

    connectionService.changeDatabase(dataConnection.get(), databaseName, connection);

    //Workbench의 선택된 Database 속성 업데이트
    String workbenchId = requestBodyMap.get("workbenchId");
    if(StringUtils.isNotEmpty(workbenchId)){
      Optional<Workbench> workbench = workbenchRepository.findById(workbenchId);
      if(workbench == null){
        throw new ResourceNotFoundException("Workbench(" + workbenchId + ") is not found.");
      }
      workbench.get().setDatabaseName(databaseName);
      workbenchRepository.saveAndFlush(workbench.get());
    }

    return ResponseEntity.ok().build();
  }

  @RequestMapping(value = "/connections/{connectionId}/datasource",
          method = RequestMethod.POST,  produces = "application/json")
  public @ResponseBody ResponseEntity<?> createDataSource(
          @PathVariable("connectionId") String connectionId,
          @RequestBody Map<String, String> requestBodyMap) throws ResourceNotFoundException, JdbcDataConnectionException {

    Optional<DataConnection> dataConnection = connectionRepository.findById(connectionId);
    if(dataConnection == null) {
      throw new ResourceNotFoundException("DataConnection(" + connectionId + ")");
    }
    String webSocketId = requestBodyMap.get("webSocketId");
    workbenchDataSourceManager.createDataSourceInfo(dataConnection.get(), webSocketId);

    return ResponseEntity.ok().build();
  }

  @RequestMapping(value = "/connections/metadata/tables/jdbc", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> listOfTablesForMdm(@RequestBody ConnectionRequest checkRequest) throws SQLException {
    //Whole Table Name List
    Map<String, Object> tables = connectionService.getTableNames(checkRequest.getConnection(), checkRequest.getDatabase(), null);
    List<String> tableNameList = (List) tables.get("tables");

    //Staging MetaDataSource List
    Set<MetadataSource> metadataSourceList = metadataSourceRepository.findMetadataSourcesByTypeAndSchemaAndSourceId(
        Metadata.SourceType.JDBC,
        checkRequest.getDatabase(),
        checkRequest.getConnection().getId()
    );

    //extract table name
    Set<String> existTableName = metadataSourceList.stream()
            .map(metadataSource -> metadataSource.getTable())
            .collect(Collectors.toSet());

    //filter already inserted table
    List<String> filteredTableNameList = tableNameList.stream()
            .filter(tableName -> !existTableName.contains(tableName))
            .collect(Collectors.toList());

    Map<String, Object> returnMap = Maps.newHashMap();
    returnMap.put("tables", filteredTableNameList);
    return ResponseEntity.ok(returnMap);
  }

  @RequestMapping(value = "/connections/metadata/tables/stage", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> listOfHiveTablesForMdm(@RequestBody ConnectionRequest checkRequest) throws BadRequestException, SQLException, ResourceNotFoundException {

    //유효성 체크
    SearchParamValidator.checkNull(checkRequest.getDatabase(), "database");

    StorageProperties.StageDBConnection stageDBConnection = getStageDBConnection(storageProperties);

    DataConnection hiveConnection = stageDBConnection.getJdbcDataConnection();

    //Whole Table Name List
    Map<String, Object> tables = connectionService.getTableNames(hiveConnection, checkRequest.getDatabase(), null);

    List<String> filteredTableNameList =
        filterTableForMdm((List) tables.get("tables"), checkRequest, Metadata.SourceType.STAGEDB);

    Map<String, Object> returnMap = Maps.newHashMap();
    returnMap.put("tables", filteredTableNameList);
    return ResponseEntity.ok(returnMap);
  }

  private static StorageProperties.StageDBConnection getStageDBConnection(StorageProperties storageProperties) throws ResourceNotFoundException {
    if (storageProperties == null || storageProperties.getStagedb() == null) {
      throw new ResourceNotFoundException("StorageProperties.StageDBConnection");
    }

    return storageProperties.getStagedb();
  }

  public List<String> filterTableForMdm(List<String> tableNameList,
                                        ConnectionRequest checkRequest,
                                        Metadata.SourceType metadataSourceType) {
    //Staging MetaDataSource List
    Set<MetadataSource> metadataSourceList = null;
    switch ( metadataSourceType){
      case JDBC:
        metadataSourceList =
                metadataSourceRepository.findMetadataSourcesByTypeAndSchemaAndSourceId(
                    Metadata.SourceType.JDBC,
                    checkRequest.getDatabase(),
                    checkRequest.getConnection().getId()
                );
        break;
      case STAGEDB:
        metadataSourceList =
                metadataSourceRepository.findMetadataSourcesByTypeAndSchema(
                    Metadata.SourceType.STAGEDB,
                    checkRequest.getDatabase()
                );
        break;
    }

    if(metadataSourceList == null || metadataSourceList.size() == 0){
      return tableNameList;
    }

    //extract table name
    Set<String> existTableName = metadataSourceList.stream()
            .map(metadataSource -> metadataSource.getTable())
            .collect(Collectors.toSet());

    //filter already inserted table
    List<String> filteredTableNameList = tableNameList.stream()
            .filter(tableName -> !existTableName.contains(tableName))
            .collect(Collectors.toList());

    return filteredTableNameList;
  }

  @Deprecated
  @RequestMapping(value = "/connections/query/hive/strict", method = RequestMethod.GET)
  public @ResponseBody ResponseEntity<?> strictModeForHiveIngestion() throws ResourceNotFoundException {
    StorageProperties.StageDBConnection stageDBConnection = getStageDBConnection(storageProperties);
    return ResponseEntity.ok(stageDBConnection.isStrictMode());
  }

  @RequestMapping(value = "/connections/query/hive/partitions/enable", method = RequestMethod.GET)
  public @ResponseBody ResponseEntity<?> enablePartitionForHiveIngestion() throws ResourceNotFoundException {
    StorageProperties.StageDBConnection stageDBConnection = getStageDBConnection(storageProperties);
    return ResponseEntity.ok(stageDBConnection.getMetastore().includeJdbc());
  }

  @RequestMapping(value = "/connections/query/hive/partitions", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> partitionInforForHiveIngestion(@RequestBody ConnectionRequest checkRequest) throws ResourceNotFoundException, BadRequestException, JdbcDataConnectionException, DataConnectionException {

    // validation checkØ
    SearchParamValidator.checkNull(checkRequest.getType(), "type");
    SearchParamValidator.checkNull(checkRequest.getQuery(), "query");
    SearchParamValidator.checkNull(checkRequest.getDatabase(), "database");

    StorageProperties.StageDBConnection stageDBConnection = getStageDBConnection(storageProperties);

    //when strict mode, requires hive metastore connection info
    if(stageDBConnection.isStrictMode()){
      DataConnection hiveConnection = stageDBConnection.getJdbcDataConnection();

      if(!HiveDialect.includeMetastoreInfo(hiveConnection)){
        throw new ResourceNotFoundException("StorageProperties.StageDBConnection's MetaStoreInfo");
      }

      HiveDataAccessorUsingMetastore jdbcDataAccessor
          = (HiveDataAccessorUsingMetastore) DataConnectionHelper.getAccessor(hiveConnection);

      List<Map<String, Object>> partitionList = jdbcDataAccessor.getPartitionList(checkRequest.getDatabase(), checkRequest.getQuery());
      return ResponseEntity.ok(partitionList);
    } else {
      throw new DataConnectionException(DataConnectionErrorCodes.NOT_SUPPORTED_API,
              "/connections/query/hive/partitions API required strict mode.");
    }
  }

  @RequestMapping(value = "/connections/query/hive/partitions/validate", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> validatePartitionInforForHiveIngestion(@RequestBody ConnectionRequest checkRequest) throws BadRequestException, ResourceNotFoundException, JdbcDataConnectionException {

    // validation check
    SearchParamValidator.checkNull(checkRequest.getType(), "type");
    SearchParamValidator.checkNull(checkRequest.getQuery(), "query");
    SearchParamValidator.checkNull(checkRequest.getDatabase(), "database");
    SearchParamValidator.checkNull(checkRequest.getPartitions(), "partitions");

    StorageProperties.StageDBConnection stageDBConnection = getStageDBConnection(storageProperties);

    //when strict mode, requires hive metastore connection info
    //if(stageDBConnection.isStrictMode()){
      DataConnection hiveConnection = stageDBConnection.getJdbcDataConnection();

      if(!HiveDialect.includeMetastoreInfo(hiveConnection)){
        throw new ResourceNotFoundException("StorageProperties.StageDBConnection's MetaStoreInfo");
      }

      HiveDataAccessorUsingMetastore jdbcDataAccessor
          = (HiveDataAccessorUsingMetastore) DataConnectionHelper.getAccessor(hiveConnection);
      List<Map<String, Object>> validatePartition = jdbcDataAccessor.validatePartition(checkRequest.getDatabase(),
                                                                                       checkRequest.getQuery(),
                                                                                       checkRequest.getPartitions());
      return ResponseEntity.ok(validatePartition);
    //} else {
    //  throw new DataConnectionException(DataConnectionErrorCodes.NOT_SUPPORTED_API,
    //          "/connections/query/hive/partitions/validate API required strict mode.");
    //}
  }

  @RequestMapping(value = "/connections/criteria", method = RequestMethod.GET)
  public ResponseEntity<?> getCriteria() {
    List<ListCriterion> listCriteria = connectionFilterService.getListCriterion();
    List<ListFilter> defaultFilter = connectionFilterService.getDefaultFilter();

    HashMap<String, Object> response = new HashMap<>();
    response.put("criteria", listCriteria);
    response.put("defaultFilters", defaultFilter);

    return ResponseEntity.ok(response);
  }

  @RequestMapping(value = "/connections/criteria/{criterionKey}", method = RequestMethod.GET)
  public ResponseEntity<?> getCriterionDetail(@PathVariable(value = "criterionKey") String criterionKey) throws ResourceNotFoundException {

    DataConnectionListCriterionKey criterionKeyEnum = DataConnectionListCriterionKey.valueOf(criterionKey);

    if(criterionKeyEnum == null){
      throw new ResourceNotFoundException("Criterion(" + criterionKey + ") is not founded.");
    }

    ListCriterion criterion = connectionFilterService.getListCriterionByKey(criterionKeyEnum);
    return ResponseEntity.ok(criterion);
  }

  @RequestMapping(value = "/connections/filter", method = RequestMethod.POST)
  public @ResponseBody ResponseEntity<?> filterDataConnection(@RequestBody DataConnectionFilterRequest request,
                                                           Pageable pageable,
                                                           PersistentEntityResourceAssembler resourceAssembler) throws BadRequestException {

    List<String> workspaces = request == null ? null : request.getWorkspace();
    List<String> createdBys = request == null ? null : request.getCreatedBy();
    List<String> userGroups = request == null ? null : request.getUserGroup();
    List<String> implementors = request == null ? null : request.getImplementor();
    List<String> authenticationTypes = request == null ? null : request.getAuthenticationType();
    DateTime createdTimeFrom = request == null ? null : request.getCreatedTimeFrom();
    DateTime createdTimeTo = request == null ? null : request.getCreatedTimeTo();
    DateTime modifiedTimeFrom = request == null ? null : request.getModifiedTimeFrom();
    DateTime modifiedTimeTo = request == null ? null : request.getModifiedTimeTo();
    String containsText = request == null ? null : request.getContainsText();
    List<Boolean> published = request == null ? null : request.getPublished();

    LOGGER.debug("Parameter (workspace) : {}", workspaces);
    LOGGER.debug("Parameter (createdBy) : {}", createdBys);
    LOGGER.debug("Parameter (userGroup) : {}", userGroups);
    LOGGER.debug("Parameter (implementors) : {}", implementors);
    LOGGER.debug("Parameter (authenticationTypes) : {}", authenticationTypes);
    LOGGER.debug("Parameter (createdTimeFrom) : {}", createdTimeFrom);
    LOGGER.debug("Parameter (createdTimeTo) : {}", createdTimeTo);
    LOGGER.debug("Parameter (modifiedTimeFrom) : {}", modifiedTimeFrom);
    LOGGER.debug("Parameter (modifiedTimeTo) : {}", modifiedTimeTo);
    LOGGER.debug("Parameter (containsText) : {}", containsText);
    LOGGER.debug("Parameter (published) : {}", published);

    // Validate authenticationTypes
    List<DataConnection.AuthenticationType> authenticationTypeEnumList
            = request.getEnumList(authenticationTypes, DataConnection.AuthenticationType.class, "authenticationType");

    // Validate createdTimeFrom, createdTimeTo
    SearchParamValidator.range(null, createdTimeFrom, createdTimeTo);

    // Validate modifiedTimeFrom, modifiedTimeTo
    SearchParamValidator.range(null, modifiedTimeFrom, modifiedTimeTo);

    // 기본 정렬 조건 셋팅
    if (pageable.getSort() == null || !pageable.getSort().iterator().hasNext()) {
      pageable = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(),
              new Sort(Sort.Direction.DESC, "createdTime", "name"));
    }

    Page<DataConnection> dataConnections = connectionFilterService.findDataConnectionByFilter(
            workspaces, createdBys, userGroups, implementors, authenticationTypeEnumList,
            createdTimeFrom, createdTimeTo, modifiedTimeFrom, modifiedTimeTo, containsText, published, pageable
    );

    return ResponseEntity.ok(this.pagedResourcesAssembler.toResource(dataConnections, resourceAssembler));
  }
}


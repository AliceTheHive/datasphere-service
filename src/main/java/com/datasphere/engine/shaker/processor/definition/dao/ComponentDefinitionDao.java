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

package com.datasphere.engine.shaker.processor.definition.dao;

import org.apache.ibatis.annotations.Param;

import com.datasphere.resource.manager.module.dictionary.domain.JusfounWord;
import com.datasphere.resource.manager.module.panel.domain.ComponentDefinitionPanel;
import com.datasphere.resource.manager.module.component.definition.domain.ComponentDefinition;

import java.util.List;


public interface ComponentDefinitionDao {

	List<ComponentDefinition> listDataSource();
	
	List<ComponentDefinitionPanel> listPanelCountGroupByComponentId(String creator);
	
	List<ComponentDefinitionPanel> listComponentDefinitionPanel(String creator);

	List<ComponentDefinition> listAllWithPager(ComponentDefinition pager);
	

	List<ComponentDefinition> listAll();

	ComponentDefinition getByCode(String code);


	/**
	 * @param name
	 * @return
	 */
//	List<JusfounWord> listGroup(@Param("group") String group, @Param("name") String name);//*****************

	List<JusfounWord> listGroupNameIsNull(@Param("creator") String creator);//*****************

	List<JusfounWord> listGroup(@Param("creator") String creator, @Param("name") String name);

	List<ComponentDefinition> listGroup2(@Param("type") String type);//*****************

	List<ComponentDefinition> listGroup1(@Param("name") String name);//*****************

	List<JusfounWord> listGroupNameIsNull2(String group);//********************

	JusfounWord myDataSouce(String name);//********************

	List<ComponentDefinition> listBy(ComponentDefinition componentDefinition);

	int delete(String id);

	Boolean exists(String id);

	int update(ComponentDefinition t);

	void insert(ComponentDefinition t);

	ComponentDefinition get(String id);

	ComponentDefinition getDFIDByName(@Param(value = "name") String name);

	List<ComponentDefinition> getDataProcessComponent(@Param(value = "creator") String creator, @Param(value = "classification") String classification);

	int getCountByName(@Param("name") String name);
}




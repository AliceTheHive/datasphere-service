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

package com.datasphere.engine.projects.controller;

import com.datasphere.engine.core.common.BaseController;
import com.datasphere.engine.core.utils.JsonWrapper;
import com.datasphere.engine.projects.model.Project;
import com.datasphere.engine.projects.service.ProjectServiceImpl;

import io.reactivex.Single;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class ProjectController extends BaseController {
	@Autowired
	ProjectServiceImpl projectServiceImpl;

	public static final String BASE_PATH = "project";
	
	/**
	 * 1.创建项目
	 * @param project
	 * @return
	 */
	@RequestMapping(value = BASE_PATH+"/createProject", method = RequestMethod.POST)
	public Single<Map<String,Object>> createProject(@RequestBody Project project, HttpServletRequest request) {
		return Single.fromCallable(() -> {
			if(!StringUtils.isBlank(project.getProjectName())){
				if(!projectServiceImpl.veriftyName(project.getProjectName())) {
					String token = request.getParameters().get("token");
					if (token == null) return JsonWrapper.failureWrapper("token不能为空！");
					return JsonWrapper.successWrapper(projectServiceImpl.create(project,token));
				} else {
					return JsonWrapper.failureWrapper("项目名称重命名");
				}
			} else {
				return JsonWrapper.failureWrapper("项目名称不能为空");
			}
		});
	}
	
	/**
	 * 2.更新项目
	 * @param project
	 * @return
	 */
	@RequestMapping(value = BASE_PATH+"/updateProject", method = RequestMethod.POST) 
	public Single<Map<String,Object>> updateProject(@RequestBody Project project, HttpServletRequest request) {
		return Single.fromCallable(() -> {
			if(!StringUtils.isBlank(project.getId())) {
				String token = request.getParameters().get("token");
				if (token == null) return JsonWrapper.failureWrapper("token不能为空！");
				int result = projectServiceImpl.update(project,token);
				if(result==1) {
					return JsonWrapper.successWrapper("项目编辑成功！");
				} else {
					return JsonWrapper.failureWrapper(result,"项目编辑失败！");
				}
			} else {
				return JsonWrapper.failureWrapper("项目id不能为空！");
			}
		});
	}
	
	/**
	 * 3.删除项目
	 * @param id 项目id
	 * @return
	 */
	@RequestMapping(value = BASE_PATH+"/deleteProject", method = RequestMethod.POST) 
	public Single<Map<String,Object>> deleteProject(@RequestParam String id, HttpServletRequest request) {
		return Single.fromCallable(() -> {
			if (!StringUtils.isBlank(id)) {
				String token = request.getParameters().get("token");
				if (token == null) return JsonWrapper.failureWrapper("token不能为空！");
				int delReturn = projectServiceImpl.delete(id,token);
				if (delReturn == 1) {
					return JsonWrapper.successWrapper("删除成功！");
				} else if (delReturn == 2) {
					return JsonWrapper.failureWrapper(delReturn, "删除失败，有面板正在运行！");
				} else {
					return JsonWrapper.failureWrapper("删除失败");
				}
			} else {
				return JsonWrapper.failureWrapper("项目id不能为空");
			}
		});
	}
	
	/**
	 * 4.根据id获得项目信息(包含面板列表信息)
	 * @param id 项目id
	 * @return
	 */
	@RequestMapping(value = BASE_PATH+"/getProjectById", method = RequestMethod.POST) 
	public Single<Map<String,Object>> getProjectById(@RequestParam String id, HttpServletRequest request) {
		return Single.fromCallable(() -> {
			if (!StringUtils.isBlank(id)) {
				String token = request.getParameters().get("token");
				if (token == null) return JsonWrapper.failureWrapper("token不能为空！");
				return JsonWrapper.successWrapper(projectServiceImpl.getProjectById(id,token));
			} else {
				return JsonWrapper.failureWrapper("项目id不能为空");
			}
		});
	}
	
	/**
	 * 5.查询全部项目（不分页）
	 * @return
	 */
	@RequestMapping(value = BASE_PATH+"/listAllProjects", method = RequestMethod.POST) 
	public Single<Map<String,Object>> listAllProjects(@RequestBody Project project) {
		return Single.fromCallable(() -> {
			List<Project> list = projectServiceImpl.getAllProjectList(project);
			return JsonWrapper.successWrapper(list);
		});
	}
	
	/**
	 * 6.带面板信息的全部项目（分页）
	 * @return
	 */
	@RequestMapping(value = BASE_PATH+"/listAllProjectsByPage", method = RequestMethod.POST) 
	public Single<Map<String,Object>> listAllProjectsByPage(@RequestBody Project project) {
		return Single.fromCallable(() -> {
			return JsonWrapper.successWrapper(projectServiceImpl.getAllWithPanelsListByPage(project));
		});
	}
	
	/**
	 * 7.根据条件查询项目列表,如按名称查询，按描述查询
	 * @return
	 */
	@RequestMapping(value = BASE_PATH+"/getProjectsByField", method = RequestMethod.POST) 
	public Single<Map<String,Object>> getProjectsByField(Project project) {
		return Single.fromCallable(() -> {
			return JsonWrapper.successWrapper(projectServiceImpl.getProjectByField(project));
		});
	}
	
	/**
	 * 8.根据项目名称条件查询项目列表
	 * @param project
	 * @return
	 */
	@RequestMapping(value = BASE_PATH+"/listProjectsByName", method = RequestMethod.POST) 
	public Single<Map<String,Object>> listProjectsByName(Project project){
		return Single.fromCallable(() -> {
			return JsonWrapper.successWrapper(projectServiceImpl.findByName(project));
		});
	}
	
	/**
	 * 验证名称，不允许重命名
	 * @param projectName
	 * @return
	 */
	@RequestMapping(value = BASE_PATH+"/veriftyProjectName", method = RequestMethod.POST) 
	public Single<Map<String,Object>> veriftyProjectName(String projectName) {
		return Single.fromCallable(() -> {
			if (!StringUtils.isBlank(projectName)) {
				return JsonWrapper.successWrapper(projectServiceImpl.veriftyName(projectName));
			} else {
				return JsonWrapper.failureWrapper("projectName参数不能为空");
			}
		});
	}
	
	/**
	 * 获取该项目面板的详细信息，包含项目列表，面板列表，以及流程信息
	 * @param projectId
	 */
	@RequestMapping(value = BASE_PATH+"/getPanelInfoByProjectId", method = RequestMethod.POST) 
	public Single<Map<String,Object>> getPanelInfoByProjectId(String projectId,String id){
		return Single.fromCallable(() -> {
			return JsonWrapper.successWrapper(projectServiceImpl.panelPageDetail(projectId, id));
		});
	}

	/**
	 * 判断项目是否存在
	 * @param id
	 * @return
	 */
	@RequestMapping(value = BASE_PATH+"/exists", method = RequestMethod.POST) 
	public Single<Map<String,Object>> exists(String id) {
		return Single.fromCallable(() -> {
			if (!StringUtils.isBlank(id)) {
				return JsonWrapper.successWrapper(projectServiceImpl.exists(id));
			} else {
				return JsonWrapper.failureWrapper("Id参数不能为空");
			}
		});
	}
}
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

package com.datasphere.server.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Repository for User domain
 */
@RepositoryRestResource(path = "users", excerptProjection = UserProjections.DefaultUserProjection.class)
public interface UserRepository extends JpaRepository<User, String>, QueryDslPredicateExecutor<User>,
                                        UserSearchRepository {

  @Override
  @RestResource(exported = false)
  User save(User entity);

  @RestResource(exported = false)
  @Override
  void delete(String id);

  @Override
  @RestResource(exported = false)
  void delete(User entity);

  @RestResource(path = "keyword")
  @Query("select u from User u where u.id= :q")  // fake!! http://stackoverflow.com/questions/25201306/implementing-custom-methods-of-spring-data-repository-and-exposing-them-through
  Page<User> searchByKeyword(@Param("q") String keywords, Pageable pageable);

  @RestResource(path = "query")
  @Query("select u from User u where u.id= :q")  // fake!!
  Page<User> searchByQuery(@Param("q") String query, Pageable pageable);

  @Override
  @PreAuthorize("hasAuthority('PERM_SYSTEM_WRITE_USER')")
  Page<User> findAll(Pageable pageable);

  @RestResource(path = "status", rel = "byStatus")
  @Query("SELECT user FROM User user WHERE user.status IN (:status)")
  @PreAuthorize("hasAuthority('PERM_SYSTEM_WRITE_USER')")
  Page<User> findByStatus(@Param("status") List<User.Status> status, Pageable pageable);

  @RestResource(exported = false)
  @Query("SELECT user FROM User user WHERE user.username = :username AND user.status NOT IN ('DELETED', 'REJECTED')")
  User findByUsername(@Param("username") String username);

  @RestResource(exported = false)
  @Query("SELECT user FROM User user WHERE user.email = :email AND user.status NOT IN ('DELETED', 'REJECTED')")
  User findByEmail(@Param("email") String email);

  /**
   * 사용자 ID 중복 확인시 활용
   *
   * @param username
   * @return
   */
  @RestResource(exported = false)
  @Query("SELECT count(user) FROM User user WHERE user.username = :username AND user.status NOT IN ('DELETED', 'REJECTED')")
  Long countByUsername(@Param("username") String username);

  /**
   * 사용자 e-mail 중복 확인시 활용
   *
   * @param email
   * @return
   */
  @RestResource(exported = false)
  @Query("SELECT count(user) FROM User user WHERE user.email = :email AND user.status NOT IN ('DELETED', 'REJECTED')")
  Long countByEmail(@Param("email") String email);

  @RestResource(exported = false)
  @Query("SELECT user FROM User user WHERE user.username IN (:usernames)")
  List<User> findByUsernames(@Param("usernames") List<String> usernames);
}

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import com.datasphere.server.domain.images.Image;
import com.datasphere.server.domain.images.ImageRepository;

/**
 * Created by kyungtaak on 2016. 5. 13..
 */
@RepositoryEventHandler(Widget.class)
public class WidgetEventHandler {

  @Autowired
  ImageRepository imageRepository;

  @HandleBeforeCreate
  @PreAuthorize("hasPermission(#widget, 'PERM_WORKSPACE_WRITE_BOOK')")
  public void checkBeforeCreate(Widget widget) {
  }

  @HandleBeforeSave
  @PreAuthorize("hasPermission(#widget, 'PERM_WORKSPACE_WRITE_BOOK')")
  public void checkBeforeUpdate(Widget widget) {

    if(widget instanceof PageWidget) {
      PageWidget pageWidget = (PageWidget) widget;
      // 이미지 처리
      if(StringUtils.isNotEmpty(pageWidget.getImageUrl())) {
        updateImages(pageWidget.getId());
      }
    }

  }

  @HandleBeforeDelete
  @PreAuthorize("hasPermission(#widget, 'PERM_WORKSPACE_WRITE_BOOK')")
  public void checkBeforeDelete(Widget widget) {

    if(widget instanceof PageWidget) {
      PageWidget pageWidget = (PageWidget) widget;
      // 이미지 처리
      if(StringUtils.isNotEmpty(pageWidget.getImageUrl())) {
        deleteImages(pageWidget.getId());
      }
    }

  }

  private void updateImages(String id) {
    List<Image> targetImages = imageRepository.findByDomainAndItemIdOrderByModifiedTimeDesc("page", id);
    if(CollectionUtils.isEmpty(targetImages)) {
      return;
    }

    if(targetImages.size() == 1) {
      targetImages.get(0).setEnabled(true);
      imageRepository.save(targetImages.get(0));
    } else {
      // 여러번 사진을 업로드한 경우
      for (int i = 0; i < targetImages.size(); i++) {
        if (i == 0) {
          targetImages.get(i).setEnabled(true);
          imageRepository.save(targetImages.get(i));
        } else {
          imageRepository.delete(targetImages.get(i));
        }
      }
    }
  }

  private void deleteImages(String id) {
    List<Image> targetImages = imageRepository.findByDomainAndItemIdOrderByModifiedTimeDesc("page", id);

    if(CollectionUtils.isEmpty(targetImages)) {
      return;
    }

    imageRepository.delete(targetImages);
  }
}

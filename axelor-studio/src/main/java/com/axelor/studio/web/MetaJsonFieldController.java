/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.studio.web;

import com.axelor.exception.service.HandleExceptionResponse;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.studio.db.repo.MetaJsonFieldRepo;
import com.axelor.studio.service.StudioMetaService;

public class MetaJsonFieldController {

  @HandleExceptionResponse
  public void trackJsonField(ActionRequest request, ActionResponse response) {
    MetaJsonField metaJsonField = request.getContext().asType(MetaJsonField.class);

    MetaJsonField jsonField =
        Beans.get(MetaJsonFieldRepo.class)
            .all()
            .filter(
                "self.name = ?1 AND self.model = ?2",
                metaJsonField.getName(),
                metaJsonField.getModel())
            .fetchOne();

    if (jsonField != null) {
      return;
    }

    Beans.get(StudioMetaService.class).trackJsonField(metaJsonField);
  }
}

/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.apps.maintenance.db.repo;

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.maintenance.db.MaintenanceRequest;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.inject.Beans;
import java.time.LocalDate;
import java.util.Optional;

public class MaintenanceRequestRepo extends MaintenanceRequestRepository {

  @Override
  public MaintenanceRequest save(MaintenanceRequest entity) {

    LocalDate doneOn = entity.getDoneOn();
    LocalDate expectedDate = entity.getExpectedDate();

    if (entity.getActionSelect() == ACTION_CORRECTIVE) {
      entity.setEndDate(doneOn != null ? doneOn.plusDays(1) : expectedDate.plusDays(1));
      entity.setStartDate(entity.getRequestDate());
    } else {
      entity.setStartDate(doneOn != null ? doneOn : expectedDate);
      entity.setEndDate(entity.getStartDate());
    }

    return super.save(entity);
  }

  @Override
  public MaintenanceRequest copy(MaintenanceRequest entity, boolean deep) {
    MaintenanceRequest copy = super.copy(entity, deep);
    LocalDate todayDate =
        Beans.get(AppBaseService.class)
            .getTodayDate(
                Optional.ofNullable(AuthUtils.getUser()).map(User::getActiveCompany).orElse(null));
    copy.setStatusSelect(MaintenanceRequestRepository.STATUS_PLANNED);
    copy.setRequestDate(todayDate);
    copy.setExpectedDate(todayDate);
    copy.setDoneOn(null);
    copy.setManufOrder(null);
    return copy;
  }
}

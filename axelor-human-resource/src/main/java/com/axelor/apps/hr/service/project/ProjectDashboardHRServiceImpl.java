/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2023 Axelor (<http://axelor.com>).
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
package com.axelor.apps.hr.service.project;

import com.axelor.apps.hr.db.TimesheetLine;
import com.axelor.apps.hr.db.repo.TimesheetLineRepository;
import com.axelor.apps.hr.service.timesheet.TimesheetLineService;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.service.ProjectDashboardServiceImpl;
import com.axelor.apps.project.service.ProjectService;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.map.HashedMap;

public class ProjectDashboardHRServiceImpl extends ProjectDashboardServiceImpl {

  @Inject TimesheetLineRepository timsheetLineRepo;
  @Inject TimesheetLineService timesheetLineService;
  @Inject ProjectService projectService;

  @Override
  public Map<String, Object> getData(Project project) {
    Map<String, Object> dataMap = super.getData(project);
    if (dataMap == null) {
      dataMap = new HashedMap<>();
    }
    dataMap.put("$spentTime", getSpentTime(project));
    dataMap.put("$isShowTimeSpent", project.getIsShowTimeSpent());
    return dataMap;
  }

  protected BigDecimal getSpentTime(Project project) {
    List<TimesheetLine> timesheetLineList =
        timsheetLineRepo
            .all()
            .filter("self.project.id IN ?1", projectService.getContextProjectIds())
            .fetch();
    BigDecimal totalDuration = BigDecimal.ZERO;
    if (CollectionUtils.isNotEmpty(timesheetLineList)) {
      for (TimesheetLine timesheetLine : timesheetLineList) {
        try {
          totalDuration =
              totalDuration.add(
                  timesheetLineService.computeHoursDuration(
                      timesheetLine.getTimesheet(), timesheetLine.getDuration(), true));
        } catch (AxelorException e) {
        }
      }
    }
    return totalDuration;
  }
}

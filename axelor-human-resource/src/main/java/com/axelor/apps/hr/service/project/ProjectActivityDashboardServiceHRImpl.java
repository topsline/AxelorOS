/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2021 Axelor (<http://axelor.com>).
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
import com.axelor.apps.hr.module.HumanResourceModule;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.service.ProjectActivityDashboardServiceImpl;
import com.axelor.mail.db.MailMessage;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

@Alternative
@Priority(HumanResourceModule.PRIORITY)
public class ProjectActivityDashboardServiceHRImpl extends ProjectActivityDashboardServiceImpl {

  @Inject protected TimesheetLineRepository timesheetLineRepo;

  @Override
  protected String getActionLink(String model) {
    if (TimesheetLine.class.getName().equals(model)) {
      return "#/ds/project.spent.time/edit/";
    }
    return super.getActionLink(model);
  }

  @Override
  protected List<String> getRelatedModels() {
    List<String> relatedModelsList = super.getRelatedModels();
    relatedModelsList.add(TimesheetLine.class.getName());
    return relatedModelsList;
  }

  @Override
  protected Project getActivityProject(
      Project contextProject, MailMessage message, Set<Long> contextProjectIdsSet) {
    if (TimesheetLine.class.getName().equals(message.getRelatedModel())) {
      TimesheetLine timesheetLine = timesheetLineRepo.find(message.getRelatedId());
      if (timesheetLine != null) {
        Project project = timesheetLine.getProject();
        if (contextProject == null
            || (project != null && contextProjectIdsSet.contains(project.getId()))) {
          return project;
        }
      }
    }
    return super.getActivityProject(contextProject, message, contextProjectIdsSet);
  }

  @Override
  protected Map<String, Object> getModelWithUtilityClass(String model) {
    Map<String, Object> dataMap = super.getModelWithUtilityClass(model);
    if (TimesheetLine.class.getName().equals(model)) {
      dataMap.put("modelName", "Timesheet line");
      dataMap.put("utilityClass", "label-important");
    }
    return dataMap;
  }
}

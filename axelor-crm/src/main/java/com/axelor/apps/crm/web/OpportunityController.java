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
package com.axelor.apps.crm.web;

import com.axelor.apps.base.exceptions.BaseExceptionMessage;
import com.axelor.apps.base.service.MapService;
import com.axelor.apps.crm.db.Opportunity;
import com.axelor.apps.crm.db.repo.OpportunityRepository;
import com.axelor.apps.crm.db.repo.OpportunityStatusRepository;
import com.axelor.apps.crm.service.OpportunityService;
import com.axelor.auth.AuthUtils;
import com.axelor.common.ObjectUtils;
import com.axelor.exception.AxelorException;
import com.axelor.exception.ResponseMessageType;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Singleton;
import java.time.LocalDate;

@Singleton
public class OpportunityController {

  public void assignToMe(ActionRequest request, ActionResponse response) {

    OpportunityService opportunityService = Beans.get(OpportunityService.class);
    OpportunityRepository opportunityRepository = Beans.get(OpportunityRepository.class);
    if (request.getContext().get("id") != null) {
      Opportunity opportunity = opportunityRepository.find((Long) request.getContext().get("id"));
      opportunity.setUser(AuthUtils.getUser());
      opportunityService.saveOpportunity(opportunity);
    } else if (ObjectUtils.notEmpty(request.getContext().get("_ids"))) {
      for (Opportunity opportunity :
          opportunityRepository
              .all()
              .filter("id in ?1", request.getContext().get("_ids"))
              .fetch()) {
        opportunity.setUser(AuthUtils.getUser());
        opportunityService.saveOpportunity(opportunity);
      }
    } else {
      response.setNotify(I18n.get(BaseExceptionMessage.RECORD_NONE_SELECTED));
      return;
    }

    response.setReload(true);
  }

  public void showOpportunitiesOnMap(ActionRequest request, ActionResponse response) {
    try {
      response.setView(
          ActionView.define(I18n.get("Opportunities"))
              .add("html", Beans.get(MapService.class).getMapURI("opportunity"))
              .map());
    } catch (Exception e) {
      TraceBackService.trace(e);
    }
  }

  public void setStageClosedWon(ActionRequest request, ActionResponse response) {
    try {
      Opportunity opportunity = request.getContext().asType(Opportunity.class);
      Beans.get(OpportunityService.class)
          .setStatusByTypeSelect(
              Beans.get(OpportunityRepository.class).find(opportunity.getId()),
              OpportunityStatusRepository.STATUS_TYPE_CLOSED_WON);
      response.setReload(true);
    } catch (AxelorException e) {
      TraceBackService.trace(response, e, ResponseMessageType.ERROR);
    }
  }

  public void setStageClosedLost(ActionRequest request, ActionResponse response) {
    try {
      Opportunity opportunity = request.getContext().asType(Opportunity.class);
      Beans.get(OpportunityService.class)
          .setStatusByTypeSelect(
              Beans.get(OpportunityRepository.class).find(opportunity.getId()),
              OpportunityStatusRepository.STATUS_TYPE_CLOSED_LOST);
      response.setReload(true);
    } catch (AxelorException e) {
      TraceBackService.trace(response, e, ResponseMessageType.ERROR);
    }
  }

  public void setNextStage(ActionRequest request, ActionResponse response) {
    Opportunity opportunity = request.getContext().asType(Opportunity.class);
    Beans.get(OpportunityService.class)
        .setOpportunityStatusNextStage(
            Beans.get(OpportunityRepository.class).find(opportunity.getId()));
    response.setReload(true);
  }

  public void fillEndDate(ActionRequest request, ActionResponse response) {
    Opportunity opportunity = request.getContext().asType(Opportunity.class);
    LocalDate recurringStartDate = opportunity.getRecurringStartDate();
    int recurringRevanue = opportunity.getExpectedDurationOfRecurringRevenue();
    if (recurringRevanue != 0 && recurringStartDate != null) {
      LocalDate newDate = recurringStartDate.plusMonths((long) recurringRevanue);
      response.setValue("recurringEndDate", newDate);
    }
  }
}

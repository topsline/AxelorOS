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
package com.axelor.apps.account.web;

import com.axelor.apps.account.db.AnalyticAccount;
import com.axelor.apps.account.db.repo.AnalyticAccountRepository;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.analytic.AnalyticAccountService;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.util.List;

public class AnalyticAccountController {

  public void setParentDomain(ActionRequest request, ActionResponse response) {
    try {
      AnalyticAccount analyticAccount = request.getContext().asType(AnalyticAccount.class);

      if (analyticAccount != null
          && analyticAccount.getAnalyticAxis() != null
          && analyticAccount.getAnalyticLevel() != null) {
        Integer level = analyticAccount.getAnalyticLevel().getNbr() + 1;
        String domain =
            "self.analyticLevel.nbr = "
                + level
                + " AND self.analyticAxis.id = "
                + analyticAccount.getAnalyticAxis().getId();
        if (analyticAccount.getCompany() != null) {
          domain = domain.concat(" AND self.company.id = " + analyticAccount.getCompany().getId());
        } else {
          domain = domain.concat(" AND self.company IS NULL");
        }
        response.setAttr("parent", "domain", domain);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void toggleStatus(ActionRequest request, ActionResponse response) {
    try {
      AnalyticAccount analyticAccount = request.getContext().asType(AnalyticAccount.class);
      analyticAccount = Beans.get(AnalyticAccountRepository.class).find(analyticAccount.getId());

      Beans.get(AnalyticAccountService.class).toggleStatusSelect(analyticAccount);

      response.setReload(true);

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void setCompanyOnAxisChange(ActionRequest request, ActionResponse response) {
    try {
      AnalyticAccount analyticAccount = request.getContext().asType(AnalyticAccount.class);
      if (analyticAccount.getAnalyticAxis() != null
          && analyticAccount.getAnalyticAxis().getCompany() != null) {
        response.setAttr("company", "readonly", true);
        response.setValue("company", analyticAccount.getAnalyticAxis().getCompany());
      } else {
        response.setAttr("company", "readonly", false);
        response.setValue("company", null);
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void checkChildrenCompany(ActionRequest request, ActionResponse response) {
    try {
      AnalyticAccount analyticAccount = request.getContext().asType(AnalyticAccount.class);
      AnalyticAccountRepository analyticAccountRepository =
          Beans.get(AnalyticAccountRepository.class);
      if (analyticAccount.getCompany() != null
          && analyticAccount.getCompany()
              != analyticAccountRepository.find(analyticAccount.getId()).getCompany()) {
        List<AnalyticAccount> childrenList =
            analyticAccountRepository.findByParent(analyticAccount).fetch();
        if (Beans.get(AnalyticAccountService.class)
            .checkChildrenAccount(analyticAccount.getCompany(), childrenList)) {
          response.setError(I18n.get(IExceptionMessage.ANALYTIC_ACCOUNT_ERROR_ON_COMPANY));
        }
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }
}

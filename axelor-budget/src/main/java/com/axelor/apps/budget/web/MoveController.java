package com.axelor.apps.budget.web;

import com.axelor.apps.account.db.Move;
import com.axelor.apps.base.ResponseMessageType;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.apps.budget.exception.IExceptionMessage;
import com.axelor.apps.budget.service.BudgetBudgetService;
import com.axelor.apps.budget.service.BudgetToolsService;
import com.axelor.apps.budget.service.move.MoveBudgetService;
import com.axelor.auth.AuthUtils;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Strings;

public class MoveController {

  public void computeBudgetDistribution(ActionRequest request, ActionResponse response) {

    try {
      Move move = request.getContext().asType(Move.class);
      MoveBudgetService moveBudgetService = Beans.get(MoveBudgetService.class);
      if (move != null
          && move.getCompany() != null
          && Beans.get(BudgetBudgetService.class).checkBudgetKeyInConfig(move.getCompany())) {
        if (!Beans.get(BudgetToolsService.class)
                .checkBudgetKeyAndRole(move.getCompany(), AuthUtils.getUser())
            && moveBudgetService.isBudgetInLines(move)) {
          response.setInfo(
              I18n.get(IExceptionMessage.BUDGET_ROLE_NOT_IN_BUDGET_DISTRIBUTION_AUTHORIZED_LIST));
          return;
        }
        String alertMessage = moveBudgetService.computeBudgetDistribution(move);

        response.setValue("budgetDistributionGenerated", moveBudgetService.isBudgetInLines(move));
        response.setValue("moveLineList", move.getMoveLineList());

        if (!Strings.isNullOrEmpty(alertMessage)) {
          response.setInfo(
              String.format(I18n.get(IExceptionMessage.BUDGET_KEY_NOT_FOUND), alertMessage));
        }
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void checkBudgetDistribution(ActionRequest request, ActionResponse response) {

    try {
      Move move = request.getContext().asType(Move.class);
      MoveBudgetService moveBudgetService = Beans.get(MoveBudgetService.class);
      if (moveBudgetService.checkMissingBudgetDistributionOnAccountedMove(move)) {
        response.setAlert(I18n.get(IExceptionMessage.NO_BUDGET_DISTRIBUTION_GENERATED));
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void validateBudgetBalance(ActionRequest request, ActionResponse response) {

    try {
      Move move = request.getContext().asType(Move.class);

      Beans.get(MoveBudgetService.class).getBudgetExceedAlert(move);

    } catch (Exception e) {
      TraceBackService.trace(response, e, ResponseMessageType.WARNING);
    }
  }
}

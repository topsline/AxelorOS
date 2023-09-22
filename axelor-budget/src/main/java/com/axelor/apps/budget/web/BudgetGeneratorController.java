package com.axelor.apps.budget.web;

import com.axelor.apps.base.ResponseMessageType;
import com.axelor.apps.base.db.Year;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.apps.budget.db.BudgetGenerator;
import com.axelor.apps.budget.db.BudgetScenarioLine;
import com.axelor.apps.budget.db.GlobalBudget;
import com.axelor.apps.budget.service.BudgetScenarioLineService;
import com.axelor.apps.budget.service.GlobalBudgetService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BudgetGeneratorController {

  public void generateGlobalBudget(ActionRequest request, ActionResponse response) {
    try {
      BudgetGenerator budgetGenerator = request.getContext().asType(BudgetGenerator.class);

      if (budgetGenerator != null) {
        GlobalBudget globalBudget =
            Beans.get(GlobalBudgetService.class).generateGlobalBudget(budgetGenerator);
        if (globalBudget != null) {
          response.setView(
              ActionView.define(I18n.get("Global budget"))
                  .model(GlobalBudget.class.getName())
                  .add("grid", "global-budget-grid")
                  .add("form", "global-budget-form")
                  .context("_showRecord", String.valueOf(globalBudget.getId()))
                  .map());
        }
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e, ResponseMessageType.ERROR);
    }
  }

  public void visualizeAmounts(ActionRequest request, ActionResponse response) {
    try {
      BudgetGenerator budgetGenerator = request.getContext().asType(BudgetGenerator.class);

      if (budgetGenerator != null) {

        List<BudgetScenarioLine> myList =
            Beans.get(GlobalBudgetService.class).visualizeVariableAmounts(budgetGenerator);

        List<Integer> fiscalYears =
            Beans.get(BudgetScenarioLineService.class)
                .getFiscalYears(budgetGenerator.getBudgetScenario());

        response.setValue("$budgetScenarioLine", myList);

        Set<Year> targetYears = budgetGenerator.getYearSet();

        List<Integer> positions = new ArrayList<>();

        for (Year targetYear : targetYears) {

          int myYear = targetYear.getFromDate().getYear();

          int position = fiscalYears.indexOf(myYear);

          positions.add(position);
        }

        if (positions.size() > 0) {
          for (int i : positions) {
            String fieldName = "$budgetScenarioLine.year" + (i + 1) + "Value";
            response.setAttr(fieldName, "hidden", false);
            response.setAttr(fieldName, "title", Integer.toString(fiscalYears.get(i)));
          }
        }
        response.setAttr("linePanel", "hidden", false);
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e, ResponseMessageType.ERROR);
    }
  }
}

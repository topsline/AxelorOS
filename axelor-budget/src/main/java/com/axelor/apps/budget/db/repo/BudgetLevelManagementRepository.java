/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.apps.budget.db.repo;

import com.axelor.apps.base.service.CurrencyScaleService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.budget.db.BudgetLevel;
import com.axelor.apps.budget.service.BudgetLevelResetToolService;
import com.axelor.apps.budget.service.BudgetToolsService;
import com.google.inject.Inject;
import java.util.Map;

public class BudgetLevelManagementRepository extends BudgetLevelRepository {

  protected AppBaseService appBaseService;
  protected BudgetLevelResetToolService budgetLevelResetToolService;
  protected CurrencyScaleService currencyScaleService;
  protected BudgetToolsService budgetToolsService;

  @Inject
  public BudgetLevelManagementRepository(
      AppBaseService appBaseService,
      BudgetLevelResetToolService budgetLevelResetToolService,
      CurrencyScaleService currencyScaleService,
      BudgetToolsService budgetToolsService) {
    this.budgetLevelResetToolService = budgetLevelResetToolService;
    this.currencyScaleService = currencyScaleService;
    this.budgetToolsService = budgetToolsService;
  }

  @Override
  public BudgetLevel copy(BudgetLevel entity, boolean deep) {
    BudgetLevel copy = super.copy(entity, deep);

    if (appBaseService.isApp("budget")) {
      budgetLevelResetToolService.resetBudgetLevel(copy);
    }
    return copy;
  }

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    json.put(
        "$currencyNumberOfDecimals",
        currencyScaleService.getCompanyScale(
            budgetToolsService.getGlobalBudgetUsingBudgetLevel(find((Long) json.get("id")))));

    return super.populate(json, context);
  }
}

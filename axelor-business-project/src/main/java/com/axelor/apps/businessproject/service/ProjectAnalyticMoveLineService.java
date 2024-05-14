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
package com.axelor.apps.businessproject.service;

import com.axelor.apps.account.db.Account;
import com.axelor.apps.account.db.AnalyticDistributionTemplate;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.TradingName;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.sale.db.SaleOrder;

public interface ProjectAnalyticMoveLineService {

  PurchaseOrder updateLines(PurchaseOrder purchaseOrder);

  SaleOrder updateLines(SaleOrder saleOrder);

  AnalyticDistributionTemplate getAnalyticDistributionTemplate(
      Project project,
      Partner partner,
      Product product,
      Company company,
      TradingName tradingName,
      Account account,
      boolean isPurchase)
      throws AxelorException;
}

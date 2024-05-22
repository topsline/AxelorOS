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
package com.axelor.apps.sale.db.repo;

import com.axelor.apps.sale.db.AdvancePayment;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.service.saleorder.SaleOrderComputeService;
import com.google.inject.Inject;
import javax.persistence.PersistenceException;

public class AdvancePaymentSaleRepository extends AdvancePaymentRepository {

  protected SaleOrderComputeService saleOrderComputeService;

  @Inject
  public AdvancePaymentSaleRepository(SaleOrderComputeService saleOrderComputeService) {
    this.saleOrderComputeService = saleOrderComputeService;
  }

  @Override
  public AdvancePayment save(AdvancePayment advancePayment) {
    try {
      SaleOrder saleOrder = advancePayment.getSaleOrder();
      saleOrderComputeService._computeSaleOrder(saleOrder);
      return super.save(advancePayment);
    } catch (Exception e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }
}

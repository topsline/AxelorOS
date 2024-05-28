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
package com.axelor.apps.supplychain.service;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.service.saleorder.SaleOrderLineService;
import java.math.BigDecimal;
import java.util.List;

public interface SaleOrderLineServiceSupplyChain extends SaleOrderLineService {

  int SALE_ORDER_LINE_NOT_INVOICED = 1;
  int SALE_ORDER_LINE_PARTIALLY_INVOICED = 2;
  int SALE_ORDER_LINE_INVOICED = 3;

  /**
   * Compute undelivered quantity.
   *
   * @param saleOrderLine
   * @return undelivered quantity
   */
  BigDecimal computeUndeliveredQty(SaleOrderLine saleOrderLine);

  /**
   * Get a list of supplier partner ids available for the product in the sale order line.
   *
   * @param saleOrderLine
   * @return List of supplier partner ids
   */
  List<Long> getSupplierPartnerList(SaleOrderLine saleOrderLine);

  /**
   * Update delivery state.
   *
   * @param saleOrderLine
   */
  void updateDeliveryState(SaleOrderLine saleOrderLine);

  /**
   * Update delivery states.
   *
   * @param saleOrderLineList
   */
  void updateDeliveryStates(List<SaleOrderLine> saleOrderLineList);

  /**
   * Create a query to find sale order line of a product of a specific/all company and a
   * specific/all stock location
   *
   * @param productId
   * @param companyId
   * @param stockLocationId
   * @return sale order line query
   */
  String getSaleOrderLineListForAProduct(Long productId, Long companyId, Long stockLocationId);

  /**
   * check qty when modifying saleOrderLine which is invoiced or delivered
   *
   * @param saleOrderLine
   * @return invoiced or delivered qty
   */
  BigDecimal checkInvoicedOrDeliveredOrderQty(SaleOrderLine saleOrderLine);

  /**
   * Update stock move lines linked to this sale order line by using estimated delivery date as date
   * used for reservation. Do nothing if the configuration is not set to use estimated delivery
   * date.
   *
   * @param saleOrderLine a sale order line managed by hibernate
   * @throws AxelorException
   */
  void updateStockMoveReservationDateTime(SaleOrderLine saleOrderLine) throws AxelorException;
}

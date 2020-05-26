/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2020 Axelor (<http://axelor.com>).
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
package com.axelor.apps.production.service.productionorder;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.SequenceRepository;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.ManufOrder;
import com.axelor.apps.production.db.ProductionOrder;
import com.axelor.apps.production.db.repo.ManufOrderRepository;
import com.axelor.apps.production.db.repo.ProductionOrderRepository;
import com.axelor.apps.production.exceptions.IExceptionMessage;
import com.axelor.apps.production.service.manuforder.ManufOrderService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
public class ProductionOrderServiceImpl implements ProductionOrderService {

  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected ManufOrderService manufOrderService;
  protected SequenceService sequenceService;
  protected ProductionOrderRepository productionOrderRepo;

  @Inject
  public ProductionOrderServiceImpl(
      ManufOrderService manufOrderService,
      SequenceService sequenceService,
      ProductionOrderRepository productionOrderRepo) {
    this.manufOrderService = manufOrderService;
    this.sequenceService = sequenceService;
    this.productionOrderRepo = productionOrderRepo;
  }

  public ProductionOrder createProductionOrder(SaleOrder saleOrder) throws AxelorException {

    ProductionOrder productionOrder = new ProductionOrder(this.getProductionOrderSeq());
    if (saleOrder != null) {
      productionOrder.setClientPartner(saleOrder.getClientPartner());
      productionOrder.setSaleOrder(saleOrder);
    }
    return productionOrder;
  }

  public String getProductionOrderSeq() throws AxelorException {

    String seq = sequenceService.getSequenceNumber(SequenceRepository.PRODUCTION_ORDER);

    if (seq == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          I18n.get(IExceptionMessage.PRODUCTION_ORDER_SEQ));
    }

    return seq;
  }

  /**
   * Generate a Production Order
   *
   * @param product Product must be passed in param because product can be different of bill of
   *     material product (Product variant)
   * @param billOfMaterial
   * @param qtyRequested
   * @param businessProject
   * @return
   * @throws AxelorException
   */
  @Transactional(rollbackOn = {Exception.class})
  public ProductionOrder generateProductionOrder(
      Product product,
      BillOfMaterial billOfMaterial,
      BigDecimal qtyRequested,
      LocalDateTime startDate)
      throws AxelorException {

    ProductionOrder productionOrder = this.createProductionOrder(null);

    this.addManufOrder(
        productionOrder,
        product,
        billOfMaterial,
        qtyRequested,
        startDate,
        null,
        null,
        ManufOrderService.ORIGIN_TYPE_OTHER);

    return productionOrderRepo.save(productionOrder);
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  public ProductionOrder addManufOrder(
      ProductionOrder productionOrder,
      Product product,
      BillOfMaterial billOfMaterial,
      BigDecimal qtyRequested,
      LocalDateTime startDate,
      LocalDateTime endDate,
      SaleOrder saleOrder,
      int originType)
      throws AxelorException {

    ManufOrder manufOrder =
        manufOrderService.generateManufOrder(
            product,
            qtyRequested,
            ManufOrderService.DEFAULT_PRIORITY,
            ManufOrderService.IS_TO_INVOICE,
            billOfMaterial,
            startDate,
            endDate,
            originType);

    if (manufOrder != null) {
      if (saleOrder != null) {
        manufOrder.setSaleOrder(saleOrder);
        manufOrder.setClientPartner(saleOrder.getClientPartner());
        manufOrder.setMoCommentFromSaleOrder(saleOrder.getProductionNote());
      }
      productionOrder.addManufOrderListItem(manufOrder);
    }
    productionOrder = Beans.get(ProductionOrderService.class).updateStatus(productionOrder);
    return productionOrderRepo.save(productionOrder);
  }

  @Override
  public ProductionOrder updateStatus(ProductionOrder productionOrder) {

    if (productionOrder == null || productionOrder.getStatusSelect() == null) {
      return productionOrder;
    }

    int statusSelect = productionOrder.getStatusSelect();

    boolean oneStarted = false;
    boolean onePlanned = false;
    boolean allCancel = true;
    boolean allCompleted = true;

    for (ManufOrder manufOrder : productionOrder.getManufOrderList()) {

      switch (manufOrder.getStatusSelect()) {
        case (ManufOrderRepository.STATUS_PLANNED):
          onePlanned = true;
          allCancel = false;
          allCompleted = false;
          break;
        case (ManufOrderRepository.STATUS_IN_PROGRESS):
        case (ManufOrderRepository.STATUS_STANDBY):
          oneStarted = true;
          allCancel = false;
          allCompleted = false;
          break;
        case (ManufOrderRepository.STATUS_FINISHED):
          allCancel = false;
          break;
        case (ManufOrderRepository.STATUS_CANCELED):
          break;
        default:
          allCompleted = false;
          break;
      }
    }

    if (allCancel) {
      statusSelect = ProductionOrderRepository.STATUS_CANCELED;
    } else if (allCompleted) {
      statusSelect = ProductionOrderRepository.STATUS_COMPLETED;
    } else if (oneStarted) {
      statusSelect = ProductionOrderRepository.STATUS_STARTED;
    } else if (onePlanned
        && (productionOrder.getStatusSelect() == ProductionOrderRepository.STATUS_DRAFT
            || productionOrder.getStatusSelect() == ProductionOrderRepository.STATUS_CANCELED
            || productionOrder.getStatusSelect() == ProductionOrderRepository.STATUS_COMPLETED)) {
      statusSelect = ProductionOrderRepository.STATUS_PLANNED;
    }

    productionOrder.setStatusSelect(statusSelect);

    return productionOrderRepo.save(productionOrder);
  }
}

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
package com.axelor.apps.production.service.productionorder;

import static com.axelor.apps.production.exceptions.ProductionExceptionMessage.YOUR_SCHEDULING_CONFIGURATION_IS_AT_THE_LATEST_YOU_NEED_TO_FILL_THE_ESTIMATED_SHIPPING_DATE;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.ManufOrder;
import com.axelor.apps.production.db.ProductionConfig;
import com.axelor.apps.production.db.ProductionOrder;
import com.axelor.apps.production.db.repo.ProductionConfigRepository;
import com.axelor.apps.production.db.repo.ProductionOrderRepository;
import com.axelor.apps.production.exceptions.ProductionExceptionMessage;
import com.axelor.apps.production.service.BillOfMaterialService;
import com.axelor.apps.production.service.app.AppProductionService;
import com.axelor.apps.production.service.config.ProductionConfigService;
import com.axelor.apps.production.service.manuforder.ManufOrderService.ManufOrderOriginTypeProduction;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.db.repo.SaleOrderLineRepository;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductionOrderSaleOrderServiceImpl implements ProductionOrderSaleOrderService {


  protected ProductionOrderService productionOrderService;
  protected ProductionOrderRepository productionOrderRepo;
  protected AppProductionService appProductionService;
  protected ProductionOrderSaleOrderMOGenerationService productionOrderSaleOrderMOGenerationService;

  @Inject
  public ProductionOrderSaleOrderServiceImpl(
      ProductionOrderService productionOrderService,
      ProductionOrderRepository productionOrderRepo,
      AppProductionService appProductionService,
      ProductionOrderSaleOrderMOGenerationService productionOrderSaleOrderMOGenerationService) {

    this.productionOrderService = productionOrderService;
    this.productionOrderRepo = productionOrderRepo;
    this.appProductionService = appProductionService;
    this.productionOrderSaleOrderMOGenerationService = productionOrderSaleOrderMOGenerationService;
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class})
  public List<Long> generateProductionOrder(SaleOrder saleOrder) throws AxelorException {

    boolean oneProdOrderPerSO = appProductionService.getAppProduction().getOneProdOrderPerSO();

    List<Long> productionOrderIdList = new ArrayList<>();
    if (saleOrder.getSaleOrderLineList() == null) {
      return productionOrderIdList;
    }

    ProductionOrder productionOrder = null;
    for (SaleOrderLine saleOrderLine : saleOrder.getSaleOrderLineList()) {

      if (productionOrder == null || !oneProdOrderPerSO) {
        productionOrder = this.createProductionOrder(saleOrder);
      }

      productionOrder = this.generateManufOrders(productionOrder, saleOrderLine);

      if (productionOrder != null && !productionOrderIdList.contains(productionOrder.getId())) {
        productionOrderIdList.add(productionOrder.getId());
      }
    }

    return productionOrderIdList;
  }

  protected ProductionOrder createProductionOrder(SaleOrder saleOrder) throws AxelorException {

    return productionOrderService.createProductionOrder(saleOrder);
  }

  @Override
  public ProductionOrder generateManufOrders(
      ProductionOrder productionOrder, SaleOrderLine saleOrderLine) throws AxelorException {

    Product product = saleOrderLine.getProduct();

    //Produce everything
    if (saleOrderLine.getSaleSupplySelect() == SaleOrderLineRepository.SALE_SUPPLY_PRODUCE
        && product != null
        && product.getProductTypeSelect().equals(ProductRepository.PRODUCT_TYPE_STORABLE)) {

      BigDecimal qtyToProduce = saleOrderLine.getQty();

      return productionOrderSaleOrderMOGenerationService.generateManufOrders(productionOrder, saleOrderLine, product, qtyToProduce);

    }
    //Produce only missing qty
    else if (saleOrderLine.getSaleSupplySelect() == SaleOrderLineRepository.SALE_SUPPLY_FROM_STOCK_AND_PRODUCE
            && product != null
            && product.getProductTypeSelect().equals(ProductRepository.PRODUCT_TYPE_STORABLE)) {


    }

    return null;
  }

}

package com.axelor.apps.production.service.operationorder;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.production.db.OperationOrder;
import com.axelor.apps.production.db.ProdProcessLine;
import com.axelor.apps.production.exceptions.ProductionExceptionMessage;
import com.axelor.apps.production.service.ProdProcessLineOutsourceService;
import com.axelor.apps.production.service.manuforder.ManufOrderOutsourceService;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.purchase.service.PurchaseOrderLineService;
import com.axelor.i18n.I18n;
import com.axelor.studio.db.AppBase;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class OperationOrderOutsourceServiceImpl implements OperationOrderOutsourceService {

  protected ProdProcessLineOutsourceService prodProcessLineOutsourceService;
  protected AppBaseService appBaseService;
  protected PurchaseOrderLineService purchaseOrderLineService;
  protected ManufOrderOutsourceService manufOrderOutsourceService;

  @Inject
  public OperationOrderOutsourceServiceImpl(
      ProdProcessLineOutsourceService prodProcessLineOutsourceService,
      ManufOrderOutsourceService manufOrderOutsourceService,
      AppBaseService appBaseService,
      PurchaseOrderLineService purchaseOrderLineService) {
    this.prodProcessLineOutsourceService = prodProcessLineOutsourceService;
    this.manufOrderOutsourceService = manufOrderOutsourceService;
    this.appBaseService = appBaseService;
    this.purchaseOrderLineService = purchaseOrderLineService;
  }

  @Override
  public Optional<Partner> getOutsourcePartner(OperationOrder operationOrder) {
    Objects.requireNonNull(operationOrder);
    Objects.requireNonNull(operationOrder.getManufOrder());

    // Fetching from manufOrder
    if (operationOrder.getOutsourcing() && operationOrder.getManufOrder().getOutsourcing()) {
      return manufOrderOutsourceService.getOutsourcePartner(operationOrder.getManufOrder());
      // Fetching from prodProcessLine or itself
    } else if (operationOrder.getOutsourcing()
        && !operationOrder.getManufOrder().getOutsourcing()) {
      ProdProcessLine prodProcessLine = operationOrder.getProdProcessLine();
      if ((prodProcessLine.getOutsourcing() || prodProcessLine.getOutsourcable())
          && operationOrder.getOutsourcingPartner() == null) {
        return prodProcessLineOutsourceService.getOutsourcePartner(prodProcessLine);
      } else {
        return Optional.ofNullable(operationOrder.getOutsourcingPartner());
      }
    }
    return Optional.empty();
  }

  @Override
  public List<PurchaseOrderLine> createPurchaseOrderLines(
      OperationOrder operationOrder, PurchaseOrder purchaseOrder) throws AxelorException {

    Objects.requireNonNull(operationOrder);
    Objects.requireNonNull(purchaseOrder);

    // Get products for purchaseOrder from prodProcessLine
    if (operationOrder.getProdProcessLine().getGeneratedPurchaseOrderProductList() != null) {
      List<PurchaseOrderLine> list = new ArrayList<>();
      for (Product product :
          operationOrder.getProdProcessLine().getGeneratedPurchaseOrderProductList()) {
        PurchaseOrderLine purchaseOrderLine =
            this.createPurchaseOrderLine(operationOrder, purchaseOrder, product).orElse(null);
        if (purchaseOrderLine != null) {
          list.add(purchaseOrderLine);
          purchaseOrder.addPurchaseOrderLineListItem(purchaseOrderLine);
        }
      }
      return list;
    }

    return Collections.emptyList();
  }

  @Override
  public Optional<PurchaseOrderLine> createPurchaseOrderLine(
      OperationOrder operationOrder, PurchaseOrder purchaseOrder, Product product)
      throws AxelorException {

    BigDecimal quantity = BigDecimal.ONE;

    Unit productUnit =
        Optional.ofNullable(product.getPurchasesUnit())
            .or(() -> Optional.ofNullable(product.getUnit()))
            .orElseThrow(
                () ->
                    new AxelorException(
                        TraceBackRepository.CATEGORY_NO_VALUE,
                        I18n.get(ProductionExceptionMessage.PURCHASE_ORDER_NO_HOURS_UNIT)));

    // If product unit is appBase.unithours or unitDay or unitMinutes
    AppBase appBase = appBaseService.getAppBase();
    if (List.of(appBase.getUnitDays(), appBase.getUnitHours(), appBase.getUnitMinutes())
        .contains(productUnit)) {
      // Quantity must be computed based on hrDurationPerCycle
      quantity = recomputeQty(operationOrder.getWorkCenter().getHrDurationPerCycle(), productUnit);
    }

    return Optional.ofNullable(
        purchaseOrderLineService.createPurchaseOrderLine(
            purchaseOrder, product, null, null, quantity, productUnit));
  }

  protected BigDecimal recomputeQty(Long hrDurationPerCycle, Unit productUnit) {
    AppBase appBase = appBaseService.getAppBase();

    // Product is in unit day
    if (productUnit.equals(appBase.getUnitDays())) {
      return new BigDecimal(hrDurationPerCycle)
          .divide(
              BigDecimal.valueOf(86400),
              appBaseService.getNbDecimalDigitForQty(),
              RoundingMode.HALF_UP);
    } else if (productUnit.equals(appBase.getUnitHours())) {
      return new BigDecimal(hrDurationPerCycle)
          .divide(
              BigDecimal.valueOf(3600),
              appBaseService.getNbDecimalDigitForQty(),
              RoundingMode.HALF_UP);
    } else if (productUnit.equals(appBase.getUnitMinutes())) {
      return new BigDecimal(hrDurationPerCycle)
          .divide(
              BigDecimal.valueOf(60),
              appBaseService.getNbDecimalDigitForQty(),
              RoundingMode.HALF_UP);
    }

    return BigDecimal.ONE;
  }
}
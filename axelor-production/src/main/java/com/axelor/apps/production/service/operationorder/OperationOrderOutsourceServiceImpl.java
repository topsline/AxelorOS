package com.axelor.apps.production.service.operationorder;

import com.axelor.apps.base.db.Partner;
import com.axelor.apps.production.db.OperationOrder;
import com.axelor.apps.production.db.ProdProcessLine;
import com.axelor.apps.production.service.ProdProcessLineOutsourceService;
import com.axelor.apps.production.service.manuforder.ManufOrderOutsourceService;
import com.google.inject.Inject;
import java.util.Objects;
import java.util.Optional;

public class OperationOrderOutsourceServiceImpl implements OperationOrderOutsourceService {

  protected ProdProcessLineOutsourceService prodProcessLineOutsourceService;
  protected ManufOrderOutsourceService manufOrderOutsourceService;

  @Inject
  public OperationOrderOutsourceServiceImpl(
      ProdProcessLineOutsourceService prodProcessLineOutsourceService,
      ManufOrderOutsourceService manufOrderOutsourceService) {
    this.prodProcessLineOutsourceService = prodProcessLineOutsourceService;
    this.manufOrderOutsourceService = manufOrderOutsourceService;
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
  public boolean getUseLineInGeneratedPO(OperationOrder operationOrder) {
    Objects.requireNonNull(operationOrder);
    Objects.requireNonNull(operationOrder.getProdProcessLine());

    ProdProcessLine prodProcessLine = operationOrder.getProdProcessLine();

    if (operationOrder.getManufOrder().getOutsourcing()
        || prodProcessLine.getOutsourcing()
        || operationOrder.getOutsourcing()
        || (prodProcessLine.getOutsourcable() && operationOrder.getOutsourcing())) {
      return prodProcessLine.getUseLineInGeneratedPurchaseOrder();
    }
    return false;
  }
}

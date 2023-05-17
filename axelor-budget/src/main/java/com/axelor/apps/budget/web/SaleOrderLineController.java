package com.axelor.apps.budget.web;

import com.axelor.apps.account.db.Account;
import com.axelor.apps.account.db.repo.AccountRepository;
import com.axelor.apps.account.db.repo.AccountTypeRepository;
import com.axelor.apps.account.service.AccountManagementAccountService;
import com.axelor.apps.base.ResponseMessageType;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.apps.budget.service.BudgetToolsService;
import com.axelor.apps.budget.service.saleorder.SaleOrderLineBudgetService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.auth.AuthUtils;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.axelor.studio.db.repo.AppBudgetRepository;
import com.axelor.utils.StringTool;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;

public class SaleOrderLineController {

  public void setProductAccount(ActionRequest request, ActionResponse response) {
    try {
      Context context = request.getContext();
      SaleOrderLine saleOrderLine = context.asType(SaleOrderLine.class);
      Account account =
          Beans.get(AccountManagementAccountService.class)
              .getProductAccount(
                  saleOrderLine.getProduct(),
                  saleOrderLine.getSaleOrder().getCompany(),
                  saleOrderLine.getSaleOrder().getFiscalPosition(),
                  false,
                  false);
      if (account.getCode().startsWith("2")
          || account.getCode().startsWith("4")
          || account.getCode().startsWith("7")) {
        response.setValue("account", account);
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e, ResponseMessageType.INFORMATION);
    }
  }

  public void setAccount(ActionRequest request, ActionResponse response) {
    Context context = request.getContext();
    SaleOrderLine saleOrderLine = context.asType(SaleOrderLine.class);
    if (context.getParent() != null) {
      Set<Account> accountsSet =
          saleOrderLine.getBudget() != null ? saleOrderLine.getBudget().getAccountSet() : null;
      response.setValue(
          "account", !CollectionUtils.isEmpty(accountsSet) ? accountsSet.iterator().next() : null);
    }
    if (!Beans.get(SaleOrderLineBudgetService.class)
        .addBudgetDistribution(saleOrderLine)
        .isEmpty()) {
      response.setValue(
          "budgetDistibutionList",
          Beans.get(SaleOrderLineBudgetService.class).addBudgetDistribution(saleOrderLine));
    }
  }

  public void fillBudgetStr(ActionRequest request, ActionResponse response) {
    try {
      SaleOrderLine saleOrderLine = request.getContext().asType(SaleOrderLine.class);
      if (saleOrderLine != null) {
        SaleOrderLineBudgetService saleOrderLineBudgetService =
            Beans.get(SaleOrderLineBudgetService.class);
        boolean multiBudget =
            Beans.get(AppBudgetRepository.class).all().fetchOne().getManageMultiBudget();

        response.setValue(
            "budgetStr",
            saleOrderLineBudgetService.searchAndFillBudgetStr(saleOrderLine, multiBudget));
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void setAccountDomain(ActionRequest request, ActionResponse response) {
    try {
      SaleOrder saleOrder = request.getContext().getParent().asType(SaleOrder.class);
      String query =
          "self.accountType.technicalTypeSelect IN ('"
              + AccountTypeRepository.TYPE_INCOME
              + "') AND self.statusSelect = "
              + AccountRepository.STATUS_ACTIVE;

      if (saleOrder != null && saleOrder.getCompany() != null) {
        query =
            query.concat(
                String.format(" AND self.company.id = %d", saleOrder.getCompany().getId()));
      }

      response.setAttr("account", "domain", query);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void getAccountDomain(ActionRequest request, ActionResponse response) {

    Context context = request.getContext();
    SaleOrderLine saleOrderLine = context.asType(SaleOrderLine.class);

    String domain = "self.id IN (0)";
    Set<Account> accountsSet =
        saleOrderLine.getBudget() != null ? saleOrderLine.getBudget().getAccountSet() : null;
    if (!CollectionUtils.isEmpty(accountsSet)) {
      domain = domain.replace("(0)", "(" + StringTool.getIdListString(accountsSet) + ")");
    }
    response.setAttr("account", "domain", domain);
  }

  public void validateBudgetLinesAmount(ActionRequest request, ActionResponse response) {
    try {
      SaleOrderLine saleOrderLine = request.getContext().asType(SaleOrderLine.class);
      Beans.get(SaleOrderLineBudgetService.class).checkAmountForSaleOrderLine(saleOrderLine);
    } catch (Exception e) {
      TraceBackService.trace(response, e, ResponseMessageType.INFORMATION);
    }
  }

  public void checkBudget(ActionRequest request, ActionResponse response) {
    try {
      SaleOrder saleOrder = request.getContext().getParent().asType(SaleOrder.class);
      if (saleOrder != null && saleOrder.getCompany() != null) {

        response.setAttr(
            "budgetDistributionPanel",
            "readonly",
            !Beans.get(BudgetToolsService.class)
                    .checkBudgetKeyAndRole(saleOrder.getCompany(), AuthUtils.getUser())
                || saleOrder.getStatusSelect() >= SaleOrderRepository.STATUS_ORDER_CONFIRMED);
        response.setAttr(
            "budget",
            "readonly",
            !Beans.get(BudgetToolsService.class)
                    .checkBudgetKeyAndRole(saleOrder.getCompany(), AuthUtils.getUser())
                || saleOrder.getStatusSelect() >= SaleOrderRepository.STATUS_ORDER_CONFIRMED);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e, ResponseMessageType.INFORMATION);
    }
  }
}

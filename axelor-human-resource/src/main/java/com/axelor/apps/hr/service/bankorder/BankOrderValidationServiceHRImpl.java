package com.axelor.apps.hr.service.bankorder;

import com.axelor.apps.account.db.repo.InvoicePaymentRepository;
import com.axelor.apps.account.db.repo.PaymentSessionRepository;
import com.axelor.apps.account.service.accountingsituation.AccountingSituationService;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.account.service.payment.invoice.payment.InvoicePaymentMoveCreateService;
import com.axelor.apps.account.service.payment.invoice.payment.InvoicePaymentToolService;
import com.axelor.apps.account.service.payment.paymentsession.PaymentSessionValidateService;
import com.axelor.apps.bankpayment.db.BankOrder;
import com.axelor.apps.bankpayment.db.repo.BankOrderRepository;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderCheckService;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderLineOriginService;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderMoveService;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderService;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderValidationServiceImpl;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.hr.db.Expense;
import com.axelor.apps.hr.db.repo.ExpenseRepository;
import com.axelor.apps.hr.service.expense.ExpensePaymentService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;

public class BankOrderValidationServiceHRImpl extends BankOrderValidationServiceImpl {

  protected ExpenseRepository expenseRepository;
  protected ExpensePaymentService expensePaymentService;

  @Inject
  public BankOrderValidationServiceHRImpl(
      AccountConfigService accountConfigService,
      InvoicePaymentToolService invoicePaymentToolService,
      InvoicePaymentMoveCreateService invoicePaymentMoveCreateService,
      InvoicePaymentRepository invoicePaymentRepository,
      AppBaseService appBaseService,
      PaymentSessionRepository paymentSessionRepository,
      BankOrderRepository bankOrderRepository,
      BankOrderCheckService bankOrderCheckService,
      BankOrderService bankOrderService,
      AccountingSituationService accountingSituationService,
      BankOrderMoveService bankOrderMoveService,
      BankOrderLineOriginService bankOrderLineOriginService,
      PaymentSessionValidateService paymentSessionValidateService,
      ExpensePaymentService expensePaymentService,
      ExpenseRepository expenseRepository) {
    super(
        accountConfigService,
        invoicePaymentToolService,
        invoicePaymentMoveCreateService,
        invoicePaymentRepository,
        appBaseService,
        paymentSessionRepository,
        bankOrderRepository,
        bankOrderCheckService,
        bankOrderService,
        accountingSituationService,
        bankOrderMoveService,
        bankOrderLineOriginService,
        paymentSessionValidateService);
    this.expenseRepository = expenseRepository;
    this.expensePaymentService = expensePaymentService;
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  public void validatePayment(BankOrder bankOrder)
      throws AxelorException, DatatypeConfigurationException, JAXBException, IOException {
    super.validatePayment(bankOrder);
    if (!appBaseService.isApp("employee")) {
      return;
    }
    List<Expense> expenseList =
        expenseRepository.all().filter("self.bankOrder.id = ?", bankOrder.getId()).fetch();
    for (Expense expense : expenseList) {
      if (expense != null && expense.getStatusSelect() != ExpenseRepository.STATUS_REIMBURSED) {
        expense.setStatusSelect(ExpenseRepository.STATUS_REIMBURSED);
        expense.setPaymentStatusSelect(InvoicePaymentRepository.STATUS_VALIDATED);
        expensePaymentService.createMoveForExpensePayment(expense);
      }
    }
  }
}

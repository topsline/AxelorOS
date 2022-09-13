package com.axelor.apps.bankpayment.service;

import com.axelor.apps.account.db.InvoiceTerm;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.db.repo.InvoiceTermRepository;
import com.axelor.apps.account.service.InvoiceVisibilityService;
import com.axelor.apps.account.service.ReconcileService;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.account.service.invoice.InvoiceTermServiceImpl;
import com.axelor.apps.account.service.invoice.InvoiceToolService;
import com.axelor.apps.account.service.payment.invoice.payment.InvoicePaymentCreateService;
import com.axelor.apps.bankpayment.db.BankOrderLineOrigin;
import com.axelor.apps.bankpayment.db.repo.BankOrderLineOriginRepository;
import com.axelor.apps.bankpayment.db.repo.BankOrderRepository;
import com.google.inject.Inject;

public class InvoiceTermBankPaymentServiceImpl extends InvoiceTermServiceImpl
    implements InvoiceTermBankPaymentService {

  protected BankOrderLineOriginRepository bankOrderLineOriginRepository;

  @Inject
  public InvoiceTermBankPaymentServiceImpl(
      InvoiceTermRepository invoiceTermRepo,
      InvoiceRepository invoiceRepo,
      AppAccountService appAccountService,
      InvoiceToolService invoiceToolService,
      InvoiceVisibilityService invoiceVisibilityService,
      AccountConfigService accountConfigService,
      ReconcileService reconcileService,
      InvoicePaymentCreateService invoicePaymentCreateService,
      BankOrderLineOriginRepository bankOrderLineOriginRepository) {
    super(
        invoiceTermRepo,
        invoiceRepo,
        appAccountService,
        invoiceToolService,
        invoiceVisibilityService,
        accountConfigService,
        reconcileService,
        invoicePaymentCreateService);
    this.bankOrderLineOriginRepository = bankOrderLineOriginRepository;
  }

  @Override
  public boolean isNotAwaitingPayment(InvoiceTerm invoiceTerm) {
    if (invoiceTerm != null && invoiceTerm.getInvoice() == null) {
      if (getAwaitingBankOrderLineOrigin(invoiceTerm) != null) {
        return false;
      }
    }
    return super.isNotAwaitingPayment(invoiceTerm);
  }

  @Override
  public BankOrderLineOrigin getAwaitingBankOrderLineOrigin(InvoiceTerm invoiceTerm) {
    return bankOrderLineOriginRepository.all().fetch().stream()
        .filter(
            origin ->
                BankOrderLineOriginRepository.RELATED_TO_INVOICE_TERM.equals(
                        origin.getRelatedToSelect())
                    && origin.getRelatedToSelectId() == invoiceTerm.getId()
                    && origin.getBankOrderLine() != null
                    && origin.getBankOrderLine().getBankOrder() != null
                    && origin.getBankOrderLine().getBankOrder().getStatusSelect()
                        == BankOrderRepository.STATUS_AWAITING_SIGNATURE
                    && origin.getBankOrderLine().getBankOrder().getOrderTypeSelect()
                        != BankOrderRepository.ORDER_TYPE_SEPA_DIRECT_DEBIT
                    && origin.getBankOrderLine().getBankOrder().getOrderTypeSelect()
                        != BankOrderRepository.ORDER_TYPE_INTERNATIONAL_DIRECT_DEBIT)
        .findAny()
        .orElse(null);
  }
}

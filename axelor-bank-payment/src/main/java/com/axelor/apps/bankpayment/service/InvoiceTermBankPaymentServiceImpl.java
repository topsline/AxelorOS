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
package com.axelor.apps.bankpayment.service;

import com.axelor.apps.account.db.InvoiceTerm;
import com.axelor.apps.account.db.repo.InvoiceTermRepository;
import com.axelor.apps.account.service.PfpService;
import com.axelor.apps.account.service.invoice.InvoiceTermFilterServiceImpl;
import com.axelor.apps.bankpayment.db.BankOrderLineOrigin;
import com.axelor.apps.bankpayment.db.repo.BankOrderLineOriginRepository;
import com.axelor.apps.bankpayment.db.repo.BankOrderRepository;
import com.google.inject.Inject;

public class InvoiceTermBankPaymentServiceImpl extends InvoiceTermFilterServiceImpl
    implements InvoiceTermBankPaymentService {

  protected BankOrderLineOriginRepository bankOrderLineOriginRepository;

  @Inject
  public InvoiceTermBankPaymentServiceImpl(
      InvoiceTermRepository invoiceTermRepository,
      BankOrderLineOriginRepository bankOrderLineOriginRepository,
      PfpService pfpService) {
    super(invoiceTermRepository, pfpService);
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
    return bankOrderLineOriginRepository.all()
        .filter(
            "self.relatedToSelect = ?1 AND self.relatedToSelectId = ?2 "
                + "AND self.bankOrderLine.bankOrder IS NOT NULL "
                + "AND (self.bankOrderLine.bankOrder.statusSelect = ?3 "
                + "OR self.bankOrderLine.bankOrder.statusSelect = ?4) "
                + "AND self.bankOrderLine.bankOrder.orderTypeSelect != ?5 "
                + "AND self.bankOrderLine.bankOrder.orderTypeSelect != ?6",
            BankOrderLineOriginRepository.RELATED_TO_INVOICE_TERM,
            invoiceTerm.getId(),
            BankOrderRepository.STATUS_DRAFT,
            BankOrderRepository.STATUS_VALIDATED,
            BankOrderRepository.ORDER_TYPE_SEPA_DIRECT_DEBIT,
            BankOrderRepository.ORDER_TYPE_INTERNATIONAL_DIRECT_DEBIT)
        .fetch().stream()
        .findAny()
        .orElse(null);
  }
}

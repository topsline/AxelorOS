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
package com.axelor.apps.account.service.batch;

import com.axelor.apps.account.db.repo.InvoicePaymentRepository;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.payment.invoice.payment.InvoicePaymentCreateService;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class BatchCreditTransferSupplierPayment extends BatchCreditTransferInvoice {

  @Inject
  public BatchCreditTransferSupplierPayment(
      AppAccountService appAccountService,
      InvoiceRepository invoiceRepo,
      InvoicePaymentCreateService invoicePaymentCreateService,
      InvoicePaymentRepository invoicePaymentRepository) {
    super(appAccountService, invoiceRepo, invoicePaymentCreateService, invoicePaymentRepository);
  }

  @Override
  protected void process() {
    processInvoices(InvoiceRepository.OPERATION_TYPE_SUPPLIER_PURCHASE);
  }
}

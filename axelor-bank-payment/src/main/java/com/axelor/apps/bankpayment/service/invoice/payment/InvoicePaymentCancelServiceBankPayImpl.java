/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2017 Axelor (<http://axelor.com>).
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
package com.axelor.apps.bankpayment.service.invoice.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.account.db.InvoicePayment;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.Reconcile;
import com.axelor.apps.account.db.repo.InvoicePaymentRepository;
import com.axelor.apps.account.db.repo.ReconcileRepository;
import com.axelor.apps.account.service.ReconcileService;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.account.service.move.MoveCancelService;
import com.axelor.apps.account.service.payment.invoice.payment.InvoicePaymentCancelServiceImpl;
import com.axelor.apps.account.service.payment.invoice.payment.InvoicePaymentToolService;
import com.axelor.apps.bankpayment.db.BankOrder;
import com.axelor.apps.bankpayment.db.repo.BankOrderRepository;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.bankpayment.service.bankorder.BankOrderService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.lang.invoke.MethodHandles;

public class InvoicePaymentCancelServiceBankPayImpl  extends InvoicePaymentCancelServiceImpl  {
	
	protected BankOrderService bankOrderService;

	
	private final Logger log = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );
	
	@Inject
	public InvoicePaymentCancelServiceBankPayImpl(
			AccountConfigService accountConfigService, InvoicePaymentRepository invoicePaymentRepository, MoveCancelService moveCancelService, 
			ReconcileService reconcileService, BankOrderService bankOrderService, InvoicePaymentToolService invoicePaymentToolService)  {
		
		super(accountConfigService, invoicePaymentRepository, moveCancelService, reconcileService, invoicePaymentToolService);
		
		this.bankOrderService = bankOrderService;
		
	}
	
	
	/**
	 * Method to cancel an invoice Payment
	 * 
	 * Cancel the eventual Move and Reconcile
	 * Compute the total amount paid on the linked invoice
  	 * Change the status to cancel
	 * 
	 * @param invoicePayment
	 * 			An invoice payment
	 * 
	 * @throws AxelorException
	 * 		
	 */
	@Override
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void cancel(InvoicePayment invoicePayment) throws AxelorException  {
		
		Move paymentMove = invoicePayment.getMove();
		BankOrder paymentBankOrder = invoicePayment.getBankOrder();
		Reconcile reconcile = invoicePayment.getReconcile();
		
		if(paymentBankOrder != null){
			if(paymentBankOrder.getStatusSelect() == BankOrderRepository.STATUS_CARRIED_OUT || paymentBankOrder.getStatusSelect() == BankOrderRepository.STATUS_REJECTED){
				throw new AxelorException(invoicePayment, IException.FUNCTIONNAL, I18n.get(IExceptionMessage.INVOICE_PAYMENT_CANCEL));
			} else {
				bankOrderService.cancelBankOrder(paymentBankOrder);
				this.updateCancelStatus(invoicePayment);
			}
		} else {
			
			log.debug("cancel : reconcile : {}", reconcile);
			
			if(reconcile != null && reconcile.getStatusSelect() == ReconcileRepository.STATUS_CONFIRMED)  {
				reconcileService.unreconcile(reconcile);
				if(accountConfigService.getAccountConfig(invoicePayment.getInvoice().getCompany()).getAllowRemovalValidatedMove())  {
					invoicePayment.setReconcile(null);
					Beans.get(ReconcileRepository.class).remove(reconcile);
				}
			}

			if(paymentMove != null && invoicePayment.getTypeSelect() == InvoicePaymentRepository.TYPE_PAYMENT)  {
				invoicePayment.setMove(null);
				moveCancelService.cancel(paymentMove);
			} else {
				this.updateCancelStatus(invoicePayment);
			}

		}	

	}
	
	
}

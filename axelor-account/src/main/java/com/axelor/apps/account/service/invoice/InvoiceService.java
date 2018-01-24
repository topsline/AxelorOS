/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
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
package com.axelor.apps.account.service.invoice;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.db.PaymentCondition;
import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.base.db.Alarm;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PriceList;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.google.inject.persist.Transactional;

/**
 * InvoiceService est une classe implémentant l'ensemble des services de
 * facturations.
 * 
 */
public interface InvoiceService {
	
	public Map<Invoice, List<Alarm>> getAlarms(Invoice... invoices);
	
	/**
	 * Lever l'ensemble des alarmes d'une facture.
	 * 
	 * @param invoice
	 * 			Une facture.
	 * 
	 * @throws Exception 
	 */
	public void raisingAlarms(Invoice invoice, String alarmEngineCode);

	
	
	/**
	 * Fonction permettant de calculer l'intégralité d'une facture :
	 * <ul>
	 * 	<li>Détermine les taxes;</li>
	 * 	<li>Détermine la TVA;</li>
	 * 	<li>Détermine les totaux.</li>
	 * </ul>
	 * (Transaction)
	 * 
	 * @param invoice
	 * 		Une facture.
	 * 
	 * @throws AxelorException
	 */
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public Invoice compute(final Invoice invoice) throws AxelorException;
	
	
	/**
	 * Validate an invoice.
	 * 
	 * @param invoice
	 * @param compute
	 * @throws AxelorException
	 */
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void validate(Invoice invoice, boolean compute) throws AxelorException;

	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void validate(Invoice invoice) throws AxelorException;

	/**
	 * Ventilation comptable d'une facture.
	 * (Transaction)
	 * 
	 * @param invoice
	 * 		Une facture.
	 * 
	 * @throws AxelorException
	 */
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void ventilate( Invoice invoice ) throws AxelorException;

	/**
	 * Annuler une facture.
	 * (Transaction)
	 * 
	 * @param invoice
	 * 		Une facture.
	 * 
	 * @throws AxelorException
	 */
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void cancel(Invoice invoice) throws AxelorException;
	

	
	/**
	 * Procédure permettant d'impacter la case à cocher "Passage à l'huissier" sur l'écriture de facture.
	 * (Transaction)
	 * 
	 * @param invoice
	 * 		Une facture
	 */
	@Transactional
	public void usherProcess(Invoice invoice);
	
	/**
	 * Créer un avoir.
	 * <p>
	 * Un avoir est une facture "inversée". Tout le montant sont opposés à la facture originale.
	 * </p>
	 * 
	 * @param invoice
	 * 
	 * @return
	 * @throws AxelorException 
	 */
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public Invoice createRefund(Invoice invoice) throws AxelorException;
	
	
	public void setDraftSequence(Invoice invoice) throws AxelorException;
	
	
	public void generateBudgetDistribution(Invoice invoice);
	
	/**
	 * Print an invoice
	 * 
	 * @param invoice the invoice to print
	 * @param toAttach whatever to attache the invoice to the object
	 * 
	 * @return ReportSettings
	 * 
	 * @throws AxelorException
	 */
	public ReportSettings printInvoice(Invoice invoice, boolean toAttach) throws AxelorException;
	
	/**
	 * Print a list of invoices in the same output
	 * 
	 * @param ids the list of invoices ids
	 * 
	 * @return ReportSettings
	 * 
	 * @throws AxelorException
	 */
	public ReportSettings printInvoices(List<Long> ids) throws AxelorException;

	public Invoice mergeInvoice(List<Invoice> invoiceList, Company company, Currency currency,
			Partner partner, Partner contactPartner, PriceList priceList,
			PaymentMode paymentMode, PaymentCondition paymentCondition) throws AxelorException  ;

	public List<InvoiceLine> getInvoiceLinesFromInvoiceList(List<Invoice> invoiceList);

	public void setInvoiceForInvoiceLines(List<InvoiceLine> invoiceLines, Invoice invoiceMerged);

	public void deleteOldInvoices(List<Invoice> invoiceList);
	
	public Invoice getInvoice(MoveLine moveLine);


	/**
	 * Create the domain for the field {@link Invoice#advancePaymentInvoiceSet}
	 * @param invoice
	 * @return
	 * @throws AxelorException
	 */
	String createAdvancePaymentInvoiceSetDomain(Invoice invoice) throws AxelorException;

	/**
	 * Return the set for the field {@link Invoice#advancePaymentInvoiceSet}
	 * @param invoice
	 * @return
	 * @throws AxelorException
	 */
	Set<Invoice> getDefaultAdvancePaymentInvoice(Invoice invoice) throws AxelorException;

	/**
	 * Return the move lines from the advance payments on sale orders
	 * @param invoice
	 * @return
	 */
	List<MoveLine> getMoveLinesFromAdvancePayments(Invoice invoice) throws AxelorException;

	/**
	 * Return the move lines from the advance payments from previous invoices
	 * @param invoice
	 * @return
	 */
	List<MoveLine> getMoveLinesFromInvoiceAdvancePayments(Invoice invoice);

	/**
	 * Return the move line from the advance payment from related sale order
	 * lines.
	 * @param invoice
	 * @return
	 */
	List<MoveLine> getMoveLinesFromSOAdvancePayments(Invoice invoice);
	/**
	 * Filter a set of advance payment invoice. If the amount of
	 * the payment is greater than the total of the invoice, we filter it.
     * If there is no remaining amount in the move lines of the advance
	 * payment invoice, we filter it too.
	 * @param invoice
	 * @param advancePaymentInvoices
	 * @throws AxelorException
	 */
	void filterAdvancePaymentInvoice(Invoice invoice,
									 Set<Invoice> advancePaymentInvoices) throws AxelorException;

	/**
     * Get the bank details from the invoice's payment schedule, the invoice itself, or the partner's default.
     * 
	 * @param invoice
	 * @return
	 * @throws AxelorException
	 */
	BankDetails getBankDetails(Invoice invoice) throws AxelorException;

	/**
	 * @param invoice
	 * @return {@link com.axelor.apps.base.db.repo.PriceListRepository#TYPE_SALE}
	 *        OR {@link com.axelor.apps.base.db.repo.PriceListRepository#TYPE_PURCHASE}
	 */
	int getPurchaseTypeOrSaleType(Invoice invoice);

}

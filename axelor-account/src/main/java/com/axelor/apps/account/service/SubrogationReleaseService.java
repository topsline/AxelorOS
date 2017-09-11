package com.axelor.apps.account.service;

import java.io.IOException;
import java.util.List;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.SubrogationRelease;
import com.axelor.apps.base.db.Company;
import com.axelor.exception.AxelorException;

public interface SubrogationReleaseService {

	/**
	 * Retrieve ventilated invoices from factorized customers.
	 * 
	 * @param company
	 * @return
	 */
	List<Invoice> retrieveInvoices(Company company);

	/**
	 * Transmit a subrogation release (generate a sequence number and change status).
	 * 
	 * @param subrogationRelease
	 */
	void transmitRelease(SubrogationRelease subrogationRelease);

	/**
	 * Generate a PDF export.
	 * 
	 * @param subrogationRelease
	 * @param name
	 * @return
	 * @throws AxelorException
	 */
	String printToPDF(SubrogationRelease subrogationRelease, String name) throws AxelorException;

	/**
	 * Generate a CSV export.
	 * 
	 * @param subrogationRelease
	 * @return
	 * @throws AxelorException
	 * @throws IOException
	 */
	String exportToCSV(SubrogationRelease subrogationRelease) throws AxelorException, IOException;

	/**
	 * Post a subrogation release (create moves).
	 * 
	 * @param subrogationRelease
	 * @throws AxelorException
	 */
	void postRelease(SubrogationRelease subrogationRelease) throws AxelorException;

}

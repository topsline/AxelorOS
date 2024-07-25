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
package com.axelor.apps.base.service;

import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Bank;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.BankDetailsTemplate;
import com.axelor.apps.base.db.BankDetailsTemplateLine;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Country;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.utils.helpers.StringHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.iban4j.CountryCode;
import org.iban4j.IbanFormatException;
import org.iban4j.IbanUtil;
import org.iban4j.InvalidCheckDigitException;
import org.iban4j.UnsupportedCountryException;

public class BankDetailsServiceImpl implements BankDetailsService {

  /**
   * This method allows to extract information from iban Update following fields :
   *
   * <ul>
   *   <li>BankCode
   *   <li>SortCode
   *   <li>AccountNbr
   *   <li>BbanKey
   *   <li>Bank
   * </ul>
   *
   * @param bankDetails
   * @return BankDetails
   */
  @Override
  public BankDetails detailsIban(BankDetails bankDetails) throws AxelorException {
    if (bankDetails.getIban() != null) {
      List<BankDetailsTemplateLine> bankDetailsTemplateLines =
          Optional.of(bankDetails)
              .map(BankDetails::getBank)
              .map(Bank::getCountry)
              .map(Country::getBankDetailsTemplate)
              .map(BankDetailsTemplate::getBankDetailsTemplateLineList)
              .orElse(null);

      // if bankDetailsTemplateLines are not set and the country is France, use the FR settings
      if ((bankDetailsTemplateLines == null || bankDetailsTemplateLines.isEmpty())
          && "FR".equals(bankDetails.getBank().getCountry().getAlpha2Code())) {
        bankDetails.setBankCode(StringHelper.extractStringFromRight(bankDetails.getIban(), 23, 5));
        bankDetails.setSortCode(StringHelper.extractStringFromRight(bankDetails.getIban(), 18, 5));
        bankDetails.setAccountNbr(
            StringHelper.extractStringFromRight(bankDetails.getIban(), 13, 11));
        bankDetails.setBbanKey(StringHelper.extractStringFromRight(bankDetails.getIban(), 2, 2));
        return bankDetails;
      }

      // country is not France and no settings in the template
      if ((bankDetailsTemplateLines == null || bankDetailsTemplateLines.isEmpty())) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_NO_VALUE,
            I18n.get(
                "No fields to use were set under the bank details template for the country in which the bank is located."));
        //        return bankDetails;
      }

      for (BankDetailsTemplateLine bankDetailsTemplateLine : bankDetailsTemplateLines) {
        int startPos = bankDetailsTemplateLine.getStartPos();
        int endPos = bankDetailsTemplateLine.getEndPos();
        String metaFieldName = bankDetailsTemplateLine.getMetaField().getName();
        if (endPos > bankDetails.getIban().length()) {
          String fieldToUse = bankDetailsTemplateLine.getTitle();
          throw new AxelorException(
              TraceBackRepository.CATEGORY_INCONSISTENCY,
              I18n.get(
                  "For the field to use \" %s \" on the bank details template, the end position is greater than the length of IBAN number."),
              fieldToUse);
        }
        switch (metaFieldName) {
          case "bankCode":
            bankDetails.setBankCode(bankDetails.getIban().substring(startPos - 1, endPos));
            break;
          case "sortCode":
            bankDetails.setSortCode(bankDetails.getIban().substring(startPos - 1, endPos));
            break;
          case "accountNbr":
            bankDetails.setAccountNbr(bankDetails.getIban().substring(startPos - 1, endPos));
            break;
          case "bbanKey":
            bankDetails.setBbanKey(bankDetails.getIban().substring(startPos - 1, endPos));
            break;
        }
      }
    }
    return bankDetails;
  }

  /**
   * Method allowing to create a bank details
   *
   * @param accountNbr
   * @param bankCode
   * @param bbanKey
   * @param bank
   * @param ownerName
   * @param partner
   * @param sortCode
   * @return
   */
  @Override
  public BankDetails createBankDetails(
      String accountNbr,
      String bankCode,
      String bbanKey,
      Bank bank,
      String ownerName,
      Partner partner,
      String sortCode) {
    BankDetails bankDetails = new BankDetails();

    bankDetails.setAccountNbr(accountNbr);
    bankDetails.setBankCode(bankCode);
    bankDetails.setBbanKey(bbanKey);
    bankDetails.setBank(bank);
    bankDetails.setOwnerName(ownerName);
    bankDetails.setPartner(partner);
    bankDetails.setSortCode(sortCode);

    return bankDetails;
  }

  /**
   * In this implementation, we do not have the O2M in paymentMode. The bank details is from the
   * company.
   *
   * @param company
   * @param paymentMode
   * @return
   * @throws AxelorException
   */
  @Override
  public String createCompanyBankDetailsDomain(
      Partner partner, Company company, PaymentMode paymentMode, Integer operationTypeSelect)
      throws AxelorException {
    if (company == null) {
      return "self.id IN (0)";
    }

    return "self.id IN ("
        + StringHelper.getIdListString(company.getBankDetailsList())
        + ") AND self.active = true";
  }

  @Override
  public BankDetails getDefaultCompanyBankDetails(
      Company company, PaymentMode paymentMode, Partner partner, Integer operationTypeSelect)
      throws AxelorException {

    BankDetails bankDetails = company.getDefaultBankDetails();
    if (bankDetails != null && bankDetails.getActive()) {
      return company.getDefaultBankDetails();
    } else {
      return null;
    }
  }

  /**
   * Get active company bank details filtered on a currency
   *
   * @param company
   * @param currency
   * @return A string field that can used as domain (Jpql WHERE clause)
   */
  public String getActiveCompanyBankDetails(Company company, Currency currency) {
    String domain = getActiveCompanyBankDetails(company);

    // filter on the currency if it is set in file format and in the bankdetails
    if (currency != null) {
      String fileFormatCurrencyId = currency.getId().toString();
      domain += " AND (self.currency IS NULL OR self.currency.id = " + fileFormatCurrencyId + ")";
    }
    return domain;
  }

  /**
   * Get active company bank details
   *
   * @param company
   * @return A string field that can used as domain (Jpql WHERE clause)
   */
  public String getActiveCompanyBankDetails(Company company) {
    String domain = "";

    if (company != null) {
      List<BankDetails> bankDetailsList = new ArrayList<>(company.getBankDetailsList());
      BankDetails defaultBankDetails = company.getDefaultBankDetails();
      if (defaultBankDetails != null) {
        bankDetailsList.add(defaultBankDetails);
      }
      Set<BankDetails> bankDetailsSet = new HashSet<>(bankDetailsList);
      String bankDetailsIds = StringHelper.getIdListString(bankDetailsSet);
      domain = "self.id IN(" + bankDetailsIds + ")";
    }
    // filter the result on active bank details
    domain += " AND self.active = true";

    return domain;
  }

  public void validateIban(String iban)
      throws IbanFormatException, InvalidCheckDigitException, UnsupportedCountryException {
    CountryCode countryCode = CountryCode.getByCode(IbanUtil.getCountryCode(iban));
    if (countryCode == null || !IbanUtil.isSupportedCountry(countryCode)) {
      throw new UnsupportedCountryException("Country code is not supported.");
    }
    if (IbanUtil.isSupportedCountry(countryCode)) {
      IbanUtil.validate(iban);
    }
  }
}

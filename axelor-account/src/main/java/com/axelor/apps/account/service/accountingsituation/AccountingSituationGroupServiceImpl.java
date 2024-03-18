package com.axelor.apps.account.service.accountingsituation;

import com.axelor.apps.account.db.AccountConfig;
import com.axelor.apps.account.db.AccountingSituation;
import com.axelor.apps.account.db.repo.AccountingSituationRepository;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Partner;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class AccountingSituationGroupServiceImpl implements AccountingSituationGroupService {

  protected AccountingSituationRecordService accountingSituationRecordService;
  protected AccountingSituationAttrsService accountingSituationAttrsService;
  protected AccountingSituationService accountingSituationService;
  protected AccountConfigService accountConfigService;

  @Inject
  public AccountingSituationGroupServiceImpl(
      AccountingSituationAttrsService accountingSituationAttrsService,
      AccountingSituationRecordService accountingSituationRecordService,
      AccountingSituationService accountingSituationService,
      AccountConfigService accountConfigService) {
    this.accountingSituationAttrsService = accountingSituationAttrsService;
    this.accountingSituationRecordService = accountingSituationRecordService;
    this.accountingSituationService = accountingSituationService;
    this.accountConfigService = accountConfigService;
  }

  @Override
  public Map<String, Object> getOnNewValuesMap(
      AccountingSituation accountingSituation, Partner partner) throws AxelorException {
    Map<String, Object> valuesMap = new HashMap<>();

    accountingSituationRecordService.setDefaultCompany(accountingSituation, partner);
    accountingSituationService.setHoldBackAccounts(accountingSituation, partner);

    valuesMap.put("company", accountingSituation.getCompany());
    valuesMap.put("holdBackCustomerAccount", accountingSituation.getHoldBackCustomerAccount());
    valuesMap.put("holdBackSupplierAccount", accountingSituation.getHoldBackSupplierAccount());
    return resetValuesMap(accountingSituation, partner, valuesMap);
  }

  @Override
  public Map<String, Object> getCompanyOnChangeValuesMap(
      AccountingSituation accountingSituation, Partner partner) throws AxelorException {
    Map<String, Object> valuesMap = new HashMap<>();

    accountingSituationService.setHoldBackAccounts(accountingSituation, partner);

    valuesMap.put("holdBackCustomerAccount", accountingSituation.getHoldBackCustomerAccount());
    valuesMap.put("holdBackSupplierAccount", accountingSituation.getHoldBackSupplierAccount());

    return resetValuesMap(accountingSituation, partner, valuesMap);
  }

  @Override
  public Map<String, Map<String, Object>> getCompanyOnChangeAttrsMap(
      AccountingSituation accountingSituation, Partner partner) throws AxelorException {
    Map<String, Map<String, Object>> attrsMap = new HashMap<>();

    accountingSituationAttrsService.managePfpValidatorUser(
        accountingSituation.getCompany(), partner, attrsMap);

    return attrsMap;
  }

  @Override
  public Map<String, Map<String, Object>> getOnNewAttrsMap(
      AccountingSituation accountingSituation, Partner partner) throws AxelorException {
    Map<String, Map<String, Object>> attrsMap = new HashMap<>();

    accountingSituationAttrsService.manageBankDetails(attrsMap);
    accountingSituationAttrsService.managePfpValidatorUser(
        accountingSituation.getCompany(), partner, attrsMap);
    accountingSituationAttrsService.hideAccountsLinkToPartner(partner, attrsMap);

    return attrsMap;
  }

  @Override
  public Map<String, Map<String, Object>> getCompanyOnSelectAttrsMap(
      AccountingSituation accountingSituation, Partner partner) {
    Map<String, Map<String, Object>> attrsMap = new HashMap<>();

    accountingSituationAttrsService.addCompanyDomain(accountingSituation, partner, attrsMap);

    return attrsMap;
  }

  @Override
  public Map<String, Map<String, Object>> getBankDetailsOnSelectAttrsMap(
      AccountingSituation accountingSituation, Partner partner, boolean isInBankDetails) {
    Map<String, Map<String, Object>> attrsMap = new HashMap<>();

    accountingSituationAttrsService.addCompanyInBankDetailsDomain(
        accountingSituation, partner, isInBankDetails, attrsMap);

    return attrsMap;
  }

  protected Map<String, Object> resetValuesMap(
      AccountingSituation accountingSituation, Partner partner, Map<String, Object> valuesMap)
      throws AxelorException {
    valuesMap.put("companyInBankDetails", null);
    valuesMap.put("companyOutBankDetails", null);
    valuesMap.put("customerAccount", null);
    valuesMap.put("supplierAccount", null);
    valuesMap.put("defaultIncomeAccount", null);
    valuesMap.put("defaultExpenseAccount", null);
    valuesMap.put("employeeAccount", null);
    valuesMap.put("pfpValidatorUser", null);
    valuesMap.put(
        "vatSystemSelect",
        partner != null && (partner.getIsSupplier() || partner.getIsCustomer())
            ? AccountingSituationRepository.VAT_COMMON_SYSTEM
            : AccountingSituationRepository.VAT_SYSTEM_DEFAULT);

    if (accountingSituation.getCompany() != null) {
      AccountConfig accountConfig =
          accountConfigService.getAccountConfig(accountingSituation.getCompany());
      valuesMap.put("invoiceAutomaticMail", accountConfig.getInvoiceAutomaticMail());
      valuesMap.put("invoiceMessageTemplate", accountConfig.getInvoiceMessageTemplate());
    }

    return valuesMap;
  }
}

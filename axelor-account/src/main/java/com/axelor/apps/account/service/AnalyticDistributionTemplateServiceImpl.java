package com.axelor.apps.account.service;

import com.axelor.apps.account.db.Account;
import com.axelor.apps.account.db.AccountConfig;
import com.axelor.apps.account.db.AnalyticAxis;
import com.axelor.apps.account.db.AnalyticAxisByCompany;
import com.axelor.apps.account.db.AnalyticDistributionLine;
import com.axelor.apps.account.db.AnalyticDistributionTemplate;
import com.axelor.apps.account.db.repo.AnalyticDistributionTemplateRepository;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.base.db.Company;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

public class AnalyticDistributionTemplateServiceImpl
    implements AnalyticDistributionTemplateService {
  protected AccountConfigService accountConfigService;
  protected AnalyticDistributionLineService analyticDistributionLineService;
  protected AnalyticDistributionTemplateRepository analyticDistributionTemplateRepository;

  @Inject
  public AnalyticDistributionTemplateServiceImpl(
      AccountConfigService accountConfigService,
      AnalyticDistributionTemplateRepository analyticDistributionTemplateRepository,
      AnalyticDistributionLineService analyticDistributionLineService) {
    this.accountConfigService = accountConfigService;
    this.analyticDistributionTemplateRepository = analyticDistributionTemplateRepository;
    this.analyticDistributionLineService = analyticDistributionLineService;
  }

  public BigDecimal getPercentage(
      AnalyticDistributionLine analyticDistributionLine, AnalyticAxis analyticAxis) {
    if (analyticDistributionLine.getAnalyticAxis() != null
        && analyticAxis != null
        && analyticDistributionLine.getAnalyticAxis() == analyticAxis) {
      return analyticDistributionLine.getPercentage();
    }
    return BigDecimal.ZERO;
  }

  public List<AnalyticAxis> getAllAxis(AnalyticDistributionTemplate analyticDistributionTemplate) {
    List<AnalyticAxis> axisList = new ArrayList<AnalyticAxis>();
    for (AnalyticDistributionLine analyticDistributionLine :
        analyticDistributionTemplate.getAnalyticDistributionLineList()) {
      if (!axisList.contains(analyticDistributionLine.getAnalyticAxis())) {
        axisList.add(analyticDistributionLine.getAnalyticAxis());
      }
    }
    return axisList;
  }

  @Override
  public boolean validateTemplatePercentages(
      AnalyticDistributionTemplate analyticDistributionTemplate) {
    List<AnalyticDistributionLine> analyticDistributionLineList =
        analyticDistributionTemplate.getAnalyticDistributionLineList();
    List<AnalyticAxis> axisList = getAllAxis(analyticDistributionTemplate);
    BigDecimal sum;
    for (AnalyticAxis analyticAxis : axisList) {
      sum = BigDecimal.ZERO;
      for (AnalyticDistributionLine analyticDistributionLine : analyticDistributionLineList) {
        sum = sum.add(getPercentage(analyticDistributionLine, analyticAxis));
      }
      if (sum.intValue() != 100) {
        return false;
      }
    }
    return true;
  }

  @Override
  @Transactional
  public AnalyticDistributionTemplate personalizeAnalyticDistributionTemplate(
      AnalyticDistributionTemplate analyticDistributionTemplate, Company company)
      throws AxelorException {
    if (analyticDistributionTemplate != null && analyticDistributionTemplate.getIsSpecific()) {
      return null;
    }
    AnalyticDistributionTemplate specificAnalyticDistributionTemplate =
        new AnalyticDistributionTemplate();
    specificAnalyticDistributionTemplate.setCompany(company);
    specificAnalyticDistributionTemplate.setName("Template - ");

    specificAnalyticDistributionTemplate.setIsSpecific(true);
    specificAnalyticDistributionTemplate =
        personalizeAnalyticTemplateFromConfig(
            analyticDistributionTemplate, specificAnalyticDistributionTemplate);
    analyticDistributionTemplateRepository.save(specificAnalyticDistributionTemplate);
    if (analyticDistributionTemplate != null) {
      specificAnalyticDistributionTemplate.setName(
          analyticDistributionTemplate.getName()
              + " - "
              + specificAnalyticDistributionTemplate.getId());
    } else {
      specificAnalyticDistributionTemplate.setName(
          "Template - " + specificAnalyticDistributionTemplate.getId());
    }

    return specificAnalyticDistributionTemplate;
  }

  @Override
  public AnalyticDistributionTemplate createDistributionTemplateFromAccount(Account account)
      throws AxelorException {
    Company company = account.getCompany();
    AccountConfig accountConfig = accountConfigService.getAccountConfig(company);
    AnalyticDistributionTemplate analyticDistributionTemplate = new AnalyticDistributionTemplate();
    analyticDistributionTemplate.setName(account.getName());
    analyticDistributionTemplate.setCompany(account.getCompany());
    analyticDistributionTemplate.setArchived(true);
    analyticDistributionTemplate.setAnalyticDistributionLineList(
        new ArrayList<AnalyticDistributionLine>());
    for (AnalyticAxisByCompany analyticAxisByCompany :
        accountConfig.getAnalyticAxisByCompanyList()) {
      analyticDistributionTemplate.addAnalyticDistributionLineListItem(
          analyticDistributionLineService.createAnalyticDistributionLine(
              analyticAxisByCompany.getAnalyticAxis(), null, null, BigDecimal.valueOf(100)));
    }
    return analyticDistributionTemplate;
  }

  @Override
  public void checkAnalyticAccounts(AnalyticDistributionTemplate analyticDistributionTemplate)
      throws AxelorException {
    if (analyticDistributionTemplate != null
        && CollectionUtils.isNotEmpty(
            analyticDistributionTemplate.getAnalyticDistributionLineList())) {
      for (AnalyticDistributionLine analyticDistributionLine :
          analyticDistributionTemplate.getAnalyticDistributionLineList()) {
        if (analyticDistributionLine.getAnalyticAccount() == null) {
          throw new AxelorException(
              TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
              I18n.get(IExceptionMessage.FIXED_ASSET_ANALYTIC_ACCOUNT_MISSING));
        }
      }
    }
  }

  protected AnalyticDistributionTemplate personalizeAnalyticTemplateFromConfig(
      AnalyticDistributionTemplate analyticDistributionTemplate,
      AnalyticDistributionTemplate newAnalyticDistributionTemplate)
      throws AxelorException {
    AccountConfig accountConfig =
        accountConfigService.getAccountConfig(analyticDistributionTemplate.getCompany());

    for (AnalyticAxisByCompany analyticAxisByCompany :
        accountConfig.getAnalyticAxisByCompanyList()) {
      boolean isIn = false;
      if (analyticDistributionTemplate != null
          && CollectionUtils.isNotEmpty(
              analyticDistributionTemplate.getAnalyticDistributionLineList())) {
        for (AnalyticDistributionLine analyticDistributionLine :
            analyticDistributionTemplate.getAnalyticDistributionLineList()) {
          if (analyticDistributionLine
              .getAnalyticAxis()
              .equals(analyticAxisByCompany.getAnalyticAxis())) {
            AnalyticDistributionLine specificAnalyticDistributionLine =
                analyticDistributionLineService.createAnalyticDistributionLine(
                    analyticAxisByCompany.getAnalyticAxis(),
                    analyticDistributionLine.getAnalyticAccount(),
                    accountConfig.getAnalyticJournal(),
                    analyticDistributionLine.getPercentage());
            specificAnalyticDistributionLine.setAnalyticDistributionTemplate(
                newAnalyticDistributionTemplate);
            newAnalyticDistributionTemplate.addAnalyticDistributionLineListItem(
                specificAnalyticDistributionLine);
            isIn = true;
          }
        }
      }
      if (!isIn) {
        AnalyticDistributionLine specificAnalyticDistributionLine =
            analyticDistributionLineService.createAnalyticDistributionLine(
                analyticAxisByCompany.getAnalyticAxis(),
                null,
                accountConfig.getAnalyticJournal(),
                new BigDecimal(100));
        specificAnalyticDistributionLine.setAnalyticDistributionTemplate(
            newAnalyticDistributionTemplate);

        newAnalyticDistributionTemplate.addAnalyticDistributionLineListItem(
            specificAnalyticDistributionLine);
      }
    }
    return newAnalyticDistributionTemplate;
  }
}

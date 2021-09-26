package com.axelor.apps.account.service;

import com.axelor.apps.account.db.AccountConfig;
import com.axelor.apps.account.db.AnalyticAxis;
import com.axelor.apps.account.db.AnalyticAxisByCompany;
import com.axelor.apps.account.db.AnalyticDistributionLine;
import com.axelor.apps.account.db.AnalyticDistributionTemplate;
import com.axelor.apps.account.db.repo.AnalyticDistributionTemplateRepository;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AnalyticDistributionTemplateServiceImpl
    implements AnalyticDistributionTemplateService {

  protected AccountConfigService accountConfigService;
  protected AnalyticDistributionTemplateRepository analyticDistributionTemplateRepository;

  @Inject
  public AnalyticDistributionTemplateServiceImpl(
      AccountConfigService accountConfigService,
      AnalyticDistributionTemplateRepository analyticDistributionTemplateRepository) {
    this.accountConfigService = accountConfigService;
    this.analyticDistributionTemplateRepository = analyticDistributionTemplateRepository;
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
      AnalyticDistributionTemplate analyticDistributionTemplate) throws AxelorException {
    if (analyticDistributionTemplate == null || analyticDistributionTemplate.getIsSpecific()) {
      return null;
    }
    AnalyticDistributionTemplate specificAnalyticDistributionTemplate =
        new AnalyticDistributionTemplate();
    specificAnalyticDistributionTemplate.setCompany(analyticDistributionTemplate.getCompany());
    specificAnalyticDistributionTemplate.setName(analyticDistributionTemplate.getName());

    specificAnalyticDistributionTemplate.setIsSpecific(true);
    AccountConfig accountConfig =
        accountConfigService.getAccountConfig(analyticDistributionTemplate.getCompany());

    for (AnalyticAxisByCompany analyticAxisByCompany :
        accountConfig.getAnalyticAxisByCompanyList()) {
      AnalyticDistributionLine specificAnalyticDistributionLine = new AnalyticDistributionLine();
      specificAnalyticDistributionLine.setAnalyticAxis(analyticAxisByCompany.getAnalyticAxis());
      specificAnalyticDistributionLine.setAnalyticDistributionTemplate(
          specificAnalyticDistributionTemplate);
      specificAnalyticDistributionLine.setAnalyticJournal(accountConfig.getAnalyticJournal());
      if (!analyticDistributionTemplate.getAnalyticDistributionLineList().isEmpty()) {
        for (AnalyticDistributionLine analyticDistributionLine :
            analyticDistributionTemplate.getAnalyticDistributionLineList()) {
          if (analyticDistributionLine
                  .getAnalyticAxis()
                  .equals(specificAnalyticDistributionLine.getAnalyticAxis())
              && analyticDistributionLine.getPercentage().compareTo(new BigDecimal(100)) == 0) {
            specificAnalyticDistributionLine.setAnalyticAccount(
                analyticDistributionLine.getAnalyticAccount());
          }
        }
      }
      specificAnalyticDistributionTemplate.addAnalyticDistributionLineListItem(
          specificAnalyticDistributionLine);
    }
    analyticDistributionTemplateRepository.save(specificAnalyticDistributionTemplate);
    specificAnalyticDistributionTemplate.setName(
        analyticDistributionTemplate.getName()
            + " - "
            + specificAnalyticDistributionTemplate.getId());
    return specificAnalyticDistributionTemplate;
  }
}

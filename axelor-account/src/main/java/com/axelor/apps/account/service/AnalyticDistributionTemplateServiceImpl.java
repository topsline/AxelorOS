package com.axelor.apps.account.service;

import com.axelor.apps.account.db.AnalyticAxis;
import com.axelor.apps.account.db.AnalyticDistributionLine;
import com.axelor.apps.account.db.AnalyticDistributionTemplate;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AnalyticDistributionTemplateServiceImpl
    implements AnalyticDistributionTemplateService {

  public AnalyticDistributionTemplateServiceImpl() {
    // TODO Auto-generated constructor stub
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
  public void validateTemplatePercentages(
      AnalyticDistributionTemplate analyticDistributionTemplate) throws AxelorException {
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
    	  throw new AxelorException(
    	          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
    	          I18n.get(IExceptionMessage.ANALYTIC_DISTRIBUTION_TEMPLATE_NOT_VALIDATED));
      }
    }
  }
}

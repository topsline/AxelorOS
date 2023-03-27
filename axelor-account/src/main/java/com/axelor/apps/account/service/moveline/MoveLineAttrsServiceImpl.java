package com.axelor.apps.account.service.moveline;

import com.axelor.apps.account.db.AccountConfig;
import com.axelor.apps.account.db.AnalyticAxis;
import com.axelor.apps.account.db.AnalyticAxisByCompany;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.account.service.PeriodServiceAccount;
import com.axelor.apps.account.service.analytic.AnalyticLineService;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.account.service.move.MoveLineControlService;
import com.axelor.auth.AuthUtils;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class MoveLineAttrsServiceImpl implements MoveLineAttrsService {
  private final int startAxisPosition = 1;
  private final int endAxisPosition = 5;

  protected AccountConfigService accountConfigService;
  protected MoveLineComputeAnalyticService moveLineComputeAnalyticService;
  protected MoveLineControlService moveLineControlService;
  protected AnalyticLineService analyticLineService;
  protected PeriodServiceAccount periodServiceAccount;

  @Inject
  public MoveLineAttrsServiceImpl(
      AccountConfigService accountConfigService,
      MoveLineComputeAnalyticService moveLineComputeAnalyticService,
      MoveLineControlService moveLineControlService,
      AnalyticLineService analyticLineService,
      PeriodServiceAccount periodServiceAccount) {
    this.accountConfigService = accountConfigService;
    this.moveLineComputeAnalyticService = moveLineComputeAnalyticService;
    this.moveLineControlService = moveLineControlService;
    this.analyticLineService = analyticLineService;
    this.periodServiceAccount = periodServiceAccount;
  }

  protected void addAttr(
      String field, String attr, Object value, Map<String, Map<String, Object>> attrsMap) {
    if (!attrsMap.containsKey(field)) {
      attrsMap.put(field, new HashMap<>());
    }

    attrsMap.get(field).put(attr, value);
  }

  @Override
  public void addAnalyticAxisAttrs(Move move, Map<String, Map<String, Object>> attrsMap)
      throws AxelorException {
    if (move != null && move.getCompany() != null) {
      AccountConfig accountConfig = accountConfigService.getAccountConfig(move.getCompany());

      if (moveLineComputeAnalyticService.checkManageAnalytic(move.getCompany())) {
        AnalyticAxis analyticAxis = null;

        for (int i = startAxisPosition; i <= endAxisPosition; i++) {
          this.addAttr(
              String.format("axis%dAnalyticAccount", i),
              "hidden",
              !(i <= accountConfig.getNbrOfAnalyticAxisSelect()),
              attrsMap);

          for (AnalyticAxisByCompany analyticAxisByCompany :
              accountConfig.getAnalyticAxisByCompanyList()) {
            if (analyticAxisByCompany.getSequence() + 1 == i) {
              analyticAxis = analyticAxisByCompany.getAnalyticAxis();
            }
          }

          if (analyticAxis != null) {
            this.addAttr(
                String.format("axis%dAnalyticAccount", i),
                "title",
                analyticAxis.getName(),
                attrsMap);

            analyticAxis = null;
          }
        }
      } else {
        this.addAttr("analyticDistributionTemplate", "hidden", true, attrsMap);
        this.addAttr("analyticMoveLineList", "hidden", true, attrsMap);

        for (int i = startAxisPosition; i <= endAxisPosition; i++) {
          this.addAttr(
              "axis".concat(Integer.toString(i)).concat("AnalyticAccount"),
              "hidden",
              true,
              attrsMap);
        }
      }
    }
  }

  @Override
  public void addAnalyticAccountRequired(
      MoveLine moveLine, Move move, Map<String, Map<String, Object>> attrsMap)
      throws AxelorException {
    for (int i = startAxisPosition; i <= endAxisPosition; i++) {
      this.addAttr(
          "axis".concat(Integer.toString(i)).concat("AnalyticAccount"),
          "required",
          analyticLineService.isAxisRequired(moveLine, move != null ? move.getCompany() : null, i),
          attrsMap);
    }
  }

  @Override
  public void addDescriptionRequired(Move move, Map<String, Map<String, Object>> attrsMap)
      throws AxelorException {
    this.addAttr(
        "$isDescriptionRequired",
        "value",
        accountConfigService.getAccountConfig(move.getCompany()).getIsDescriptionRequired(),
        attrsMap);
  }

  @Override
  public void addAnalyticDistributionTypeSelect(
      Move move, Map<String, Map<String, Object>> attrsMap) throws AxelorException {
    this.addAttr(
        "$analyticDistributionTypeSelect",
        "value",
        accountConfigService
            .getAccountConfig(move.getCompany())
            .getAnalyticDistributionTypeSelect(),
        attrsMap);
  }

  @Override
  public void addInvoiceTermListPercentageWarningText(
      MoveLine moveLine, Map<String, Map<String, Object>> attrsMap) {
    this.addAttr(
        "$invoiceTermListPercentageWarningText",
        "value",
        !moveLineControlService.displayInvoiceTermWarningMessage(moveLine),
        attrsMap);
  }

  @Override
  public void addReadonly(Move move, Map<String, Map<String, Object>> attrsMap) {
    boolean statusCondition =
        move.getStatusSelect() == MoveRepository.STATUS_DAYBOOK
            || move.getStatusSelect() == MoveRepository.STATUS_CANCELED;
    boolean singleStatusCondition = move.getStatusSelect() == MoveRepository.STATUS_CANCELED;

    this.addAttr("informationsPanel", "readonly", statusCondition, attrsMap);
    this.addAttr("analyticDistributionPanel", "readonly", statusCondition, attrsMap);
    this.addAttr("irrecoverableDetailsPanel", "readonly", singleStatusCondition, attrsMap);
    this.addAttr("currency", "readonly", singleStatusCondition, attrsMap);
    this.addAttr("otherPanel", "readonly", singleStatusCondition, attrsMap);

    if (move.getPaymentCondition() != null) {
      this.addAttr("dueDate", "readonly", move.getPaymentCondition().getIsFree(), attrsMap);
    }
  }

  @Override
  public void addShowTaxAmount(MoveLine moveLine, Map<String, Map<String, Object>> attrsMap) {
    this.addAttr(
        "taxAmount",
        "hidden",
        !moveLine.getAccount().getReconcileOk() || !moveLine.getAccount().getUseForPartnerBalance(),
        attrsMap);
  }

  @Override
  public void addShowAnalyticDistributionPanel(
      Move move, MoveLine moveLine, Map<String, Map<String, Object>> attrsMap)
      throws AxelorException {
    boolean condition =
        accountConfigService.getAccountConfig(move.getCompany()).getManageAnalyticAccounting()
            && moveLine.getAccount().getAnalyticDistributionAuthorized();

    this.addAttr("analyticDistributionPanel", "hidden", !condition, attrsMap);
  }

  @Override
  public void addValidatePeriod(Move move, Map<String, Map<String, Object>> attrsMap)
      throws AxelorException {
    this.addAttr(
        "$validatePeriod",
        "value",
        periodServiceAccount.isAuthorizedToAccountOnPeriod(move.getPeriod(), AuthUtils.getUser()),
        attrsMap);
  }
}

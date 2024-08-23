package com.axelor.apps.sale.service.loyalty;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.sale.db.LoyaltyAccount;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public class LoyaltyAccountServiceImpl implements LoyaltyAccountService {

  @Override
  public Optional<LoyaltyAccount> getLoyaltyAccount(Partner partner, Company company) {
    Optional<LoyaltyAccount> loyaltyAccount = Optional.empty();
    if (partner != null && company != null) {
      loyaltyAccount =
          partner.getLoyaltyAccountList().stream()
              .filter(account -> company.equals(account.getCompany()))
              .findFirst();
    }
    return loyaltyAccount;
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  public LoyaltyAccount acquirePoints(LoyaltyAccount loyaltyAccount, Integer delay) {
    loyaltyAccount.getHistoryLineList().stream()
        .filter(
            historyLine ->
                !historyLine.getPointsAcquired()
                    && historyLine
                        .getSaleOrder()
                        .getConfirmationDateTime()
                        .plusDays(delay)
                        .isBefore(LocalDateTime.now()))
        .forEach(
            historyLine -> {
              BigDecimal pointsBalance = historyLine.getPointsBalance();
              historyLine.setRemainingPoints(pointsBalance);
              historyLine.setAcquisitionDateTime(LocalDateTime.now());
              historyLine.setPointsAcquired(true);
              loyaltyAccount.setPointsBalance(
                  loyaltyAccount.getPointsBalance().add(historyLine.getPointsBalance()));
            });
    return loyaltyAccount;
  }
}

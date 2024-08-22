package com.axelor.apps.sale.service.loyalty;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.sale.db.LoyaltyAccount;
import java.time.LocalDateTime;
import java.util.Optional;

public interface LoyaltyAccountService {
  Optional<LoyaltyAccount> getLoyaltyAccount(Partner partner, Company company);

  LoyaltyAccount acquirePoints(LoyaltyAccount loyaltyAccount, LocalDateTime limitDateTime);
}

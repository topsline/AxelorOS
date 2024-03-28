package com.axelor.apps.account.service.payment.paymentsession;

import com.axelor.apps.account.db.InvoiceTerm;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.PaymentSession;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Partner;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public interface PaymentSessionLcrValidateService {
  @Transactional(rollbackOn = {AxelorException.class})
  StringBuilder processInvoiceTerms(PaymentSession paymentSession) throws AxelorException;

  @Transactional(rollbackOn = {Exception.class})
  int processPaymentSession(
      PaymentSession paymentSession,
      List<Pair<InvoiceTerm, Pair<InvoiceTerm, BigDecimal>>> invoiceTermLinkWithRefundList)
      throws AxelorException;

  void processLcrPlacement(
      PaymentSession paymentSession,
      InvoiceTerm invoiceTerm,
      Map<LocalDate, Map<Partner, List<Move>>> moveDateMap,
      List<Pair<InvoiceTerm, Pair<InvoiceTerm, BigDecimal>>> invoiceTermLinkWithRefundList)
      throws AxelorException;
}

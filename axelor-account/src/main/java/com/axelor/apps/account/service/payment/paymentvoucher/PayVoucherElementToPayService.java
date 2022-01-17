/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.account.service.payment.paymentvoucher;

import com.axelor.apps.account.db.AccountConfig;
import com.axelor.apps.account.db.FinancialDiscount;
import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.db.PayVoucherDueElement;
import com.axelor.apps.account.db.PayVoucherElementToPay;
import com.axelor.apps.account.db.PaymentVoucher;
import com.axelor.apps.account.db.repo.FinancialDiscountRepository;
import com.axelor.apps.account.db.repo.PayVoucherElementToPayRepository;
import com.axelor.apps.account.db.repo.PaymentVoucherRepository;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.service.CurrencyService;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayVoucherElementToPayService {

  protected CurrencyService currencyService;
  protected PayVoucherElementToPayRepository payVoucherElementToPayRepo;
  protected AccountConfigService accountConfigService;

  private final int RETURN_SCALE = 2;
  private final int CALCULATION_SCALE = 10;

  @Inject
  public PayVoucherElementToPayService(
      CurrencyService currencyService,
      PayVoucherElementToPayRepository payVoucherElementToPayRepo,
      AccountConfigService accountConfigService) {
    this.currencyService = currencyService;
    this.payVoucherElementToPayRepo = payVoucherElementToPayRepo;
    this.accountConfigService = accountConfigService;
  }

  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Generic method for creating invoice to pay lines (2nd O2M in the view)
   *
   * @param pv
   * @param seq
   * @return
   */
  public PayVoucherElementToPay createPayVoucherElementToPay(
      PaymentVoucher paymentVoucher,
      int seq,
      Invoice invoice,
      MoveLine moveLine,
      BigDecimal totalAmount,
      BigDecimal remainingAmount,
      BigDecimal amountToPay) {

    log.debug("In  createPayVoucherElementToPay....");

    if (paymentVoucher != null && moveLine != null) {
      PayVoucherElementToPay piToPay = new PayVoucherElementToPay();
      piToPay.setSequence(seq);
      piToPay.setMoveLine(moveLine);
      piToPay.setTotalAmount(totalAmount);
      piToPay.setRemainingAmount(remainingAmount);
      piToPay.setAmountToPay(amountToPay);
      piToPay.setPaymentVoucher(paymentVoucher);

      log.debug("End createPayVoucherElementToPay IF.");

      return piToPay;
    } else {
      log.debug("End createPayVoucherElementToPay ELSE.");
      return null;
    }
  }

  @Transactional(rollbackOn = AxelorException.class)
  public void updateAmountToPayCurrency(PayVoucherElementToPay elementToPay)
      throws AxelorException {
    Currency paymentVoucherCurrency = elementToPay.getPaymentVoucher().getCurrency();
    BigDecimal amountToPayCurrency =
        currencyService.getAmountCurrencyConvertedAtDate(
            elementToPay.getCurrency(),
            paymentVoucherCurrency,
            elementToPay.getAmountToPay(),
            elementToPay.getPaymentVoucher().getPaymentDate());
    elementToPay.setAmountToPayCurrency(amountToPayCurrency);
    elementToPay.setRemainingAmountAfterPayment(
        elementToPay.getRemainingAmount().subtract(elementToPay.getAmountToPay()));

    payVoucherElementToPayRepo.save(elementToPay);
  }

  @Transactional
  public PayVoucherElementToPay updateElementToPayWithFinancialDiscount(
      PayVoucherElementToPay payVoucherElementToPay,
      PayVoucherDueElement payVoucherDueElement,
      PaymentVoucher paymentVoucher)
      throws AxelorException {
    if (!payVoucherDueElement.getApplyFinancialDiscount()
        || payVoucherDueElement.getFinancialDiscount() == null) {
      return payVoucherElementToPay;
    }
    if (paymentVoucher != null) {
      payVoucherElementToPay.setPaymentVoucher(paymentVoucher);
    }
    FinancialDiscount financialDiscount = payVoucherDueElement.getFinancialDiscount();
    LocalDate financialDiscountDeadlineDate =
        payVoucherDueElement.getFinancialDiscountDeadlineDate();
    if (financialDiscountDeadlineDate.compareTo(paymentVoucher.getPaymentDate()) >= 0) {
      payVoucherElementToPay.setApplyFinancialDiscount(true);
      payVoucherElementToPay.setFinancialDiscount(financialDiscount);
      payVoucherElementToPay.setFinancialDiscountDeadlineDate(financialDiscountDeadlineDate);
      payVoucherElementToPay.setFinancialDiscountAmount(
          calculateFinancialDiscountAmount(payVoucherElementToPay));
      payVoucherElementToPay.setFinancialDiscountTaxAmount(
          calculateFinancialDiscountTaxAmount(payVoucherElementToPay));
      payVoucherElementToPay.setFinancialDiscountTotalAmount(
          calculateFinancialDiscountTotalAmount(payVoucherElementToPay));
      payVoucherElementToPay.setRemainingAmountAfterFinDiscount(
          payVoucherElementToPay
              .getAmountToPay()
              .subtract(payVoucherElementToPay.getFinancialDiscountTotalAmount()));
    }
    return payVoucherElementToPay;
  }

  public BigDecimal calculateFinancialDiscountAmount(PayVoucherElementToPay payVoucherElementToPay)
      throws AxelorException {
    return calculateFinancialDiscountAmountUnscaled(payVoucherElementToPay)
        .setScale(RETURN_SCALE, RoundingMode.HALF_UP);
  }

  protected BigDecimal calculateFinancialDiscountAmountUnscaled(
      PayVoucherElementToPay payVoucherElementToPay) throws AxelorException {
    if (payVoucherElementToPay == null
        || payVoucherElementToPay.getFinancialDiscount() == null
        || payVoucherElementToPay.getPaymentVoucher() == null) {
      return BigDecimal.ZERO;
    }

    BigDecimal baseAmount = payVoucherElementToPay.getAmountToPay();
    AccountConfig accountConfig =
        accountConfigService.getAccountConfig(
            payVoucherElementToPay.getPaymentVoucher().getCompany());

    BigDecimal baseAmountByRate =
        baseAmount.multiply(
            payVoucherElementToPay
                .getFinancialDiscount()
                .getDiscountRate()
                .divide(new BigDecimal(100), CALCULATION_SCALE, RoundingMode.HALF_UP));

    if (payVoucherElementToPay.getFinancialDiscount().getDiscountBaseSelect()
        == FinancialDiscountRepository.DISCOUNT_BASE_HT) {
      return baseAmountByRate.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
    } else if (payVoucherElementToPay.getFinancialDiscount().getDiscountBaseSelect()
            == FinancialDiscountRepository.DISCOUNT_BASE_VAT
        && (payVoucherElementToPay.getPaymentVoucher().getOperationTypeSelect()
                == PaymentVoucherRepository.OPERATION_TYPE_SUPPLIER_PURCHASE
            || payVoucherElementToPay.getPaymentVoucher().getOperationTypeSelect()
                == PaymentVoucherRepository.OPERATION_TYPE_CLIENT_SALE)
        && accountConfig.getPurchFinancialDiscountTax() != null) {
      return baseAmountByRate.divide(
          accountConfig
              .getPurchFinancialDiscountTax()
              .getActiveTaxLine()
              .getValue()
              .add(new BigDecimal(1)),
          CALCULATION_SCALE,
          RoundingMode.HALF_UP);
    } else if (payVoucherElementToPay.getFinancialDiscount().getDiscountBaseSelect()
            == FinancialDiscountRepository.DISCOUNT_BASE_VAT
        && (payVoucherElementToPay.getPaymentVoucher().getOperationTypeSelect()
                == PaymentVoucherRepository.OPERATION_TYPE_SUPPLIER_REFUND
            || payVoucherElementToPay.getPaymentVoucher().getOperationTypeSelect()
                == PaymentVoucherRepository.OPERATION_TYPE_CLIENT_REFUND)
        && accountConfig.getSaleFinancialDiscountTax() != null) {
      return baseAmountByRate.divide(
          accountConfig
              .getSaleFinancialDiscountTax()
              .getActiveTaxLine()
              .getValue()
              .add(new BigDecimal(1)),
          CALCULATION_SCALE,
          RoundingMode.HALF_UP);
    } else {
      return BigDecimal.ZERO;
    }
  }

  public BigDecimal calculateFinancialDiscountTaxAmount(
      PayVoucherElementToPay payVoucherElementToPay) throws AxelorException {
    return calculateFinancialDiscountTaxAmountUnscaled(payVoucherElementToPay)
        .setScale(RETURN_SCALE, RoundingMode.HALF_UP);
  }

  protected BigDecimal calculateFinancialDiscountTaxAmountUnscaled(
      PayVoucherElementToPay payVoucherElementToPay) throws AxelorException {
    if (payVoucherElementToPay == null
        || payVoucherElementToPay.getFinancialDiscount() == null
        || payVoucherElementToPay.getFinancialDiscount().getDiscountBaseSelect()
            != FinancialDiscountRepository.DISCOUNT_BASE_VAT) {
      return BigDecimal.ZERO;
    }

    BigDecimal financialDiscountAmount =
        calculateFinancialDiscountAmountUnscaled(payVoucherElementToPay);

    AccountConfig accountConfig =
        accountConfigService.getAccountConfig(
            payVoucherElementToPay.getPaymentVoucher().getCompany());
    if ((payVoucherElementToPay.getPaymentVoucher().getOperationTypeSelect()
                == PaymentVoucherRepository.OPERATION_TYPE_SUPPLIER_PURCHASE
            || payVoucherElementToPay.getPaymentVoucher().getOperationTypeSelect()
                == PaymentVoucherRepository.OPERATION_TYPE_CLIENT_SALE)
        && accountConfig.getPurchFinancialDiscountTax() != null) {
      return financialDiscountAmount.multiply(
          accountConfig.getPurchFinancialDiscountTax().getActiveTaxLine().getValue());
    } else if ((payVoucherElementToPay.getPaymentVoucher().getOperationTypeSelect()
                == PaymentVoucherRepository.OPERATION_TYPE_SUPPLIER_REFUND
            || payVoucherElementToPay.getPaymentVoucher().getOperationTypeSelect()
                == PaymentVoucherRepository.OPERATION_TYPE_CLIENT_REFUND)
        && accountConfig.getSaleFinancialDiscountTax() != null) {
      return financialDiscountAmount.multiply(
          accountConfig.getSaleFinancialDiscountTax().getActiveTaxLine().getValue());
    }
    return BigDecimal.ZERO;
  }

  public BigDecimal calculateFinancialDiscountTotalAmount(
      PayVoucherElementToPay payVoucherElementToPay) throws AxelorException {
    return (calculateFinancialDiscountAmountUnscaled(payVoucherElementToPay)
            .add(calculateFinancialDiscountTaxAmountUnscaled(payVoucherElementToPay)))
        .setScale(RETURN_SCALE, RoundingMode.HALF_UP);
  }
}

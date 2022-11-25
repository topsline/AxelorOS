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
package com.axelor.apps.account.web;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceTerm;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.db.PaymentSession;
import com.axelor.apps.account.db.PfpPartialReason;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.db.repo.InvoiceTermAccountRepository;
import com.axelor.apps.account.db.repo.InvoiceTermRepository;
import com.axelor.apps.account.db.repo.PaymentSessionRepository;
import com.axelor.apps.account.exception.AccountExceptionMessage;
import com.axelor.apps.account.service.PaymentSessionService;
import com.axelor.apps.account.service.invoice.InvoiceTermPfpService;
import com.axelor.apps.account.service.invoice.InvoiceTermService;
import com.axelor.apps.tool.ContextTool;
import com.axelor.auth.AuthUtils;
import com.axelor.common.ObjectUtils;
import com.axelor.exception.ResponseMessageType;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InvoiceTermController {

  @SuppressWarnings("unused")
  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void computeCustomizedAmount(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);

      if (request.getContext().getParent() != null
          && request.getContext().getParent().containsKey("_model")) {
        BigDecimal total = this.getCustomizedTotal(request.getContext().getParent(), invoiceTerm);
        BigDecimal amount =
            Beans.get(InvoiceTermService.class).getCustomizedAmount(invoiceTerm, total);

        if (amount.signum() > 0) {
          response.setValue("amount", amount);
          response.setValue("amountRemaining", amount);
        }
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void computeCustomizedPercentage(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);

      if (request.getContext().getParent() != null
          && request.getContext().getParent().containsKey("_model")) {
        InvoiceTermService invoiceTermService = Beans.get(InvoiceTermService.class);
        BigDecimal total = this.getCustomizedTotal(request.getContext().getParent(), invoiceTerm);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
          return;
        }

        BigDecimal percentage =
            invoiceTermService.computeCustomizedPercentage(invoiceTerm.getAmount(), total);

        response.setValue("percentage", percentage);
        response.setValue("amountRemaining", invoiceTerm.getAmount());
        response.setValue(
            "isCustomized",
            invoiceTerm.getPaymentConditionLine() == null
                || percentage.compareTo(
                        invoiceTerm.getPaymentConditionLine().getPaymentPercentage())
                    != 0);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  protected BigDecimal getCustomizedTotal(Context parentContext, InvoiceTerm invoiceTerm) {
    if (parentContext.get("_model").equals(Invoice.class.getName())) {
      Invoice invoice = parentContext.asType(Invoice.class);
      return invoice.getInTaxTotal();
    } else if (parentContext.get("_model").equals(MoveLine.class.getName())) {
      MoveLine moveLine = parentContext.asType(MoveLine.class);
      return Beans.get(InvoiceTermService.class).getTotalInvoiceTermsAmount(moveLine);
    } else {
      return BigDecimal.ZERO;
    }
  }

  public void initInvoiceTerm(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);
      InvoiceTermService invoiceTermService = Beans.get(InvoiceTermService.class);
      Invoice invoice = null;
      MoveLine moveLine = null;

      if (request.getContext().getParent() != null) {
        invoice = ContextTool.getContextParent(request.getContext(), Invoice.class, 1);
        if (invoice == null) {
          moveLine = ContextTool.getContextParent(request.getContext(), MoveLine.class, 1);

          if (moveLine != null) {
            Move move = ContextTool.getContextParent(request.getContext(), Move.class, 2);
            invoiceTermService.initCustomizedInvoiceTerm(moveLine, invoiceTerm, move);

            if (move != null) {
              moveLine.setMove(move);
            }
          }
        }
      } else if (request.getContext().get("_invoiceId") != null) {
        invoice =
            Beans.get(InvoiceRepository.class)
                .find(Long.valueOf((Integer) request.getContext().get("_invoiceId")));
      }

      if (invoice != null) {
        invoiceTermService.initCustomizedInvoiceTerm(invoice, invoiceTerm);
        response.setValues(invoiceTerm);
      }

      invoiceTermService.setParentFields(invoiceTerm, moveLine, invoice);
      response.setValues(invoiceTerm);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  @SuppressWarnings("unchecked")
  public void refusalToPay(ActionRequest request, ActionResponse response) {
    try {
      List<Long> invoiceTermIds = (List<Long>) request.getContext().get("_ids");
      Integer invoiceTermId = (Integer) request.getContext().get("_id");
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);
      if (ObjectUtils.notEmpty(invoiceTermId) && ObjectUtils.isEmpty(invoiceTermIds)) {

        if (invoiceTerm.getCompany() != null && invoiceTerm.getReasonOfRefusalToPay() != null) {
          Beans.get(InvoiceTermPfpService.class)
              .refusalToPay(
                  Beans.get(InvoiceTermRepository.class).find(invoiceTerm.getId()),
                  invoiceTerm.getReasonOfRefusalToPay(),
                  invoiceTerm.getReasonOfRefusalToPayStr());

          response.setCanClose(true);
        }
      } else if (ObjectUtils.isEmpty(invoiceTermId)) {
        if (ObjectUtils.isEmpty(invoiceTermIds)) {
          response.setError(
              I18n.get(AccountExceptionMessage.INVOICE_INVOICE_TERM_MASS_UPDATE_NO_RECORD));
          return;
        }
        Integer recordsSelected = invoiceTermIds.size();
        Integer recordsRefused =
            Beans.get(InvoiceTermPfpService.class)
                .massRefusePfp(
                    invoiceTermIds,
                    invoiceTerm.getReasonOfRefusalToPay(),
                    invoiceTerm.getReasonOfRefusalToPayStr());
        response.setFlash(
            String.format(
                I18n.get(AccountExceptionMessage.INVOICE_INVOICE_TERM_MASS_REFUSAL_SUCCESSFUL),
                recordsRefused,
                recordsSelected));
        response.setCanClose(true);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void setPfpValidatorUserDomain(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);

      response.setAttr(
          "pfpValidatorUser",
          "domain",
          Beans.get(InvoiceTermService.class)
              .getPfpValidatorUserDomain(invoiceTerm.getPartner(), invoiceTerm.getCompany()));
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void hideSendEmailPfpBtn(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);

      if (invoiceTerm.getPfpValidatorUser() != null) {
        response.setAttr(
            "$isSelectedPfpValidatorEqualsPartnerPfpValidator",
            "value",
            invoiceTerm
                .getPfpValidatorUser()
                .equals(
                    Beans.get(InvoiceTermService.class)
                        .getPfpValidatorUser(invoiceTerm.getPartner(), invoiceTerm.getCompany())));
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void searchEligibleTerms(ActionRequest request, ActionResponse response) {
    try {
      PaymentSession paymentSession = request.getContext().asType(PaymentSession.class);
      paymentSession = Beans.get(PaymentSessionRepository.class).find(paymentSession.getId());
      Beans.get(InvoiceTermService.class).retrieveEligibleTerms(paymentSession);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void validatePfp(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceterm =
          Beans.get(InvoiceTermRepository.class)
              .find(request.getContext().asType(InvoiceTerm.class).getId());
      Beans.get(InvoiceTermPfpService.class).validatePfp(invoiceterm, AuthUtils.getUser());
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  @SuppressWarnings("unchecked")
  public void massValidatePfp(ActionRequest request, ActionResponse response) {
    try {
      List<Long> invoiceTermIds = (List<Long>) request.getContext().get("_ids");
      if (ObjectUtils.isEmpty(invoiceTermIds)) {
        response.setError(
            I18n.get(AccountExceptionMessage.INVOICE_INVOICE_TERM_MASS_UPDATE_NO_RECORD));
        return;
      }
      Integer recordsSelected = invoiceTermIds.size();
      Integer recordsUpdated =
          Beans.get(InvoiceTermPfpService.class).massValidatePfp(invoiceTermIds);
      response.setFlash(
          String.format(
              I18n.get(AccountExceptionMessage.INVOICE_INVOICE_TERM_MASS_VALIDATION_SUCCESSFUL),
              recordsUpdated,
              recordsSelected));
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void pfpPartialReasonConfirm(ActionRequest request, ActionResponse response) {
    try {
      if (ObjectUtils.isEmpty(request.getContext().get("_id"))) {
        response.setError(I18n.get(AccountExceptionMessage.INVOICE_INVOICE_TERM_NOT_SAVED));
        return;
      }

      InvoiceTerm originalInvoiceTerm =
          Beans.get(InvoiceTermRepository.class)
              .find(Long.valueOf((Integer) request.getContext().get("_id")));

      BigDecimal grantedAmount = new BigDecimal((String) request.getContext().get("grantedAmount"));
      if (grantedAmount.signum() == 0) {
        response.setError(
            I18n.get(AccountExceptionMessage.INVOICE_INVOICE_TERM_PFP_GRANTED_AMOUNT_ZERO));
        return;
      }

      BigDecimal invoiceAmount = originalInvoiceTerm.getAmount();
      if (grantedAmount.compareTo(invoiceAmount) >= 0) {
        response.setValue("$grantedAmount", originalInvoiceTerm.getAmountRemaining());
        response.setFlash(
            I18n.get(AccountExceptionMessage.INVOICE_INVOICE_TERM_INVALID_GRANTED_AMOUNT));
        return;
      }

      PfpPartialReason partialReason =
          (PfpPartialReason) request.getContext().get("pfpPartialReason");
      if (ObjectUtils.isEmpty(partialReason)) {
        response.setError(
            I18n.get(AccountExceptionMessage.INVOICE_INVOICE_TERM_PARTIAL_REASON_EMPTY));
        return;
      }

      Beans.get(InvoiceTermPfpService.class)
          .generateInvoiceTerm(originalInvoiceTerm, invoiceAmount, grantedAmount, partialReason);
      response.setCanClose(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void selectTerm(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);
      invoiceTerm = Beans.get(InvoiceTermRepository.class).find(invoiceTerm.getId());
      Beans.get(InvoiceTermService.class).toggle(invoiceTerm, true);
      response.setReload(true);

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void selectPartnerTerm(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);
      if (invoiceTerm.getMoveLine().getPartner() != null
          && invoiceTerm.getPaymentSession() != null) {
        List<InvoiceTerm> invoiceTermList =
            Beans.get(InvoiceTermAccountRepository.class)
                .findByPaymentSessionAndPartner(
                    invoiceTerm.getPaymentSession(), invoiceTerm.getMoveLine().getPartner());
        if (!CollectionUtils.isEmpty(invoiceTermList)) {
          InvoiceTermService invoiceTermService = Beans.get(InvoiceTermService.class);
          InvoiceTermRepository invoiceTermRepository = Beans.get(InvoiceTermRepository.class);
          for (InvoiceTerm invoiceTermTemp : invoiceTermList) {
            invoiceTermTemp = invoiceTermRepository.find(invoiceTermTemp.getId());
            invoiceTermService.toggle(invoiceTermTemp, true);
          }
        }
      }
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void unselectTerm(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);
      invoiceTerm = Beans.get(InvoiceTermRepository.class).find(invoiceTerm.getId());
      Beans.get(InvoiceTermService.class).toggle(invoiceTerm, false);
      response.setReload(true);

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void unselectPartnerTerm(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);
      if (invoiceTerm.getMoveLine().getPartner() != null
          && invoiceTerm.getPaymentSession() != null) {
        List<InvoiceTerm> invoiceTermList =
            Beans.get(InvoiceTermAccountRepository.class)
                .findByPaymentSessionAndPartner(
                    invoiceTerm.getPaymentSession(), invoiceTerm.getMoveLine().getPartner());
        if (!CollectionUtils.isEmpty(invoiceTermList)) {
          InvoiceTermService invoiceTermService = Beans.get(InvoiceTermService.class);
          InvoiceTermRepository invoiceTermRepository = Beans.get(InvoiceTermRepository.class);
          for (InvoiceTerm invoiceTermTemp : invoiceTermList) {
            invoiceTermTemp = invoiceTermRepository.find(invoiceTermTemp.getId());
            invoiceTermService.toggle(invoiceTermTemp, false);
          }
        }
      }
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void computeTotalPaymentSession(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);
      Beans.get(PaymentSessionService.class)
          .computeTotalPaymentSession(invoiceTerm.getPaymentSession());
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void computeFinancialDiscount(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);
      MoveLine moveLine = invoiceTerm.getMoveLine();
      if (moveLine == null) {
        moveLine = ContextTool.getContextParent(request.getContext(), MoveLine.class, 1);

        if (moveLine == null) {
          return;
        }
      }

      Beans.get(InvoiceTermService.class)
          .computeFinancialDiscount(
              invoiceTerm,
              moveLine.getCredit().max(moveLine.getDebit()),
              moveLine.getFinancialDiscount(),
              moveLine.getFinancialDiscountTotalAmount(),
              moveLine.getRemainingAmountAfterFinDiscount());

      response.setValues(invoiceTerm);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void setPfpStatus(ActionRequest request, ActionResponse response) {
    try {
      InvoiceTerm invoiceTerm = request.getContext().asType(InvoiceTerm.class);

      if (invoiceTerm.getInvoice() == null) {
        Invoice invoice = ContextTool.getContextParent(request.getContext(), Invoice.class, 1);
        invoiceTerm.setInvoice(invoice);
      }

      if (invoiceTerm.getMoveLine() == null) {
        MoveLine moveLine = ContextTool.getContextParent(request.getContext(), MoveLine.class, 1);

        if (moveLine != null && moveLine.getMove() == null) {
          Move move = ContextTool.getContextParent(request.getContext(), Move.class, 2);
          moveLine.setMove(move);
        }

        invoiceTerm.setMoveLine(moveLine);
      }

      Beans.get(InvoiceTermService.class).setPfpStatus(invoiceTerm);
      response.setValue("pfpValidateStatusSelect", invoiceTerm.getPfpValidateStatusSelect());
    } catch (Exception e) {
      TraceBackService.trace(response, e, ResponseMessageType.ERROR);
    }
  }
}

package com.axelor.apps.bankpayment.service.bankstatementline.camt53;

import com.axelor.apps.account.db.InterbankCodeLine;
import com.axelor.apps.account.db.repo.InterbankCodeLineRepository;
import com.axelor.apps.bankpayment.db.BankStatement;
import com.axelor.apps.bankpayment.db.BankStatementLine;
import com.axelor.apps.bankpayment.db.BankStatementLineCAMT53;
import com.axelor.apps.bankpayment.db.repo.BankStatementLineCAMT53Repository;
import com.axelor.apps.bankpayment.db.repo.BankStatementRepository;
import com.axelor.apps.bankpayment.service.bankstatement.BankStatementImportService;
import com.axelor.apps.bankpayment.service.bankstatementline.BankStatementLineCreateAbstractService;
import com.axelor.apps.bankpayment.service.bankstatementline.afb120.StructuredContentLine;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.AccountIdentification4Choice;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.AccountStatement2;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.ActiveOrHistoricCurrencyAndAmount;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.BalanceType12;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.BalanceType12Code;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.BalanceType5Choice;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.BankToCustomerStatementV02;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.BankTransactionCodeStructure4;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.BankTransactionCodeStructure5;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.CashAccount20;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.CashBalance3;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.CreditDebitCode;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.CreditorReferenceInformation2;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.DateAndDateTimeChoice;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.DateTimePeriodDetails;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.Document;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.EntryDetails1;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.EntryTransaction2;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.GenericAccountIdentification1;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.Party6Choice;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.PartyIdentification32;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.ProprietaryBankTransactionCodeStructure1;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.RemittanceInformation5;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.ReportEntry2;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.StructuredRemittanceInformation7;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.TransactionParty2;
import com.axelor.apps.bankpayment.xsd.bankstatement.camt_053_001_02.TransactionReferences2;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.repo.BankDetailsRepository;
import com.axelor.apps.base.db.repo.CurrencyRepository;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.persistence.Query;
import javax.xml.datatype.XMLGregorianCalendar;

public class BankStatementLineCreateCAMT53Service extends BankStatementLineCreateAbstractService {
  public static String CREDIT_DEBIT_INDICATOR_CREDIT = "CRDT";
  public static String CREDIT_DEBIT_INDICATOR_DEBIT = "DBIT";
  public static String BALANCE_TYPE_INITIAL_BALANCE = "OPBD";
  public static String BALANCE_TYPE_FINAL_BALANCE = "CLBD";

  protected BankStatementLineCAMT53Repository bankStatementLineCAMT53Repository;

  protected InterbankCodeLineRepository interbankCodeLineRepository;

  protected BankDetailsRepository bankDetailsRepository;

  protected BankStatementLineCreationCAMT53Service bankStatementLineCreationCAMT53Service;

  @Inject
  protected BankStatementLineCreateCAMT53Service(
      BankStatementRepository bankStatementRepository,
      BankStatementImportService bankStatementService,
      BankStatementLineCAMT53Repository bankStatementLineCAMT53Repository,
      InterbankCodeLineRepository interbankCodeLineRepository,
      BankDetailsRepository bankDetailsRepository,
      BankStatementLineCreationCAMT53Service bankStatementLineCreationCAMT53Service) {
    super(bankStatementRepository, bankStatementService);
    this.bankStatementLineCAMT53Repository = bankStatementLineCAMT53Repository;
    this.interbankCodeLineRepository = interbankCodeLineRepository;
    this.bankDetailsRepository = bankDetailsRepository;
    this.bankStatementLineCreationCAMT53Service = bankStatementLineCreationCAMT53Service;
  }

  @Override
  protected List<StructuredContentLine> readFile() throws IOException, AxelorException {
    return null;
  }

  @Override
  protected BankStatementLine createBankStatementLine(
      StructuredContentLine structuredContentLine, int sequence) {
    return null;
  }

  @Override
  protected void process() throws IOException, AxelorException {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Document.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      Document document = (Document) jaxbUnmarshaller.unmarshal(file);
      if (document == null) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_NO_VALUE, I18n.get("Error: Cannot read the input file."));
      }
      BankToCustomerStatementV02 bkToCstmrStmt = document.getBkToCstmrStmt();
      if (bkToCstmrStmt == null) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_NO_VALUE, I18n.get("Error: No bank statement found."));
      }
      List<AccountStatement2> stmtList = bkToCstmrStmt.getStmt();
      if (stmtList == null || stmtList.isEmpty()) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_NO_VALUE, I18n.get("Error: No bank statement found."));
      }
      AccountStatement2 stmt = stmtList.get(0);
      if (stmt == null) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_NO_VALUE, I18n.get("Error: No bank statement found."));
      }
      DateTimePeriodDetails frToDt = stmt.getFrToDt();
      bankStatement = setBankStatementFrToDt(frToDt, bankStatement);

      CashAccount20 acct = stmt.getAcct();
      BankDetails bankDetails = null;
      if (acct != null) {
        String ibanOrOthers =
            Optional.of(acct)
                .map(CashAccount20::getId)
                .map(AccountIdentification4Choice::getIBAN)
                .orElse(null);
        if (ibanOrOthers == null) {
          // others
          ibanOrOthers =
              Optional.of(acct)
                  .map(CashAccount20::getId)
                  .map(AccountIdentification4Choice::getOthr)
                  .map(GenericAccountIdentification1::getId)
                  .orElse(null);
        }
        // find bankDetails
        bankDetails = findBankDetailsByIban(ibanOrOthers);

        if (bankDetails == null) {
          throw new AxelorException(
              TraceBackRepository.CATEGORY_NO_VALUE,
              I18n.get("Error: The bank details doesn't exist."));
        }
      }

      int sequence = 0;

      List<CashBalance3> balList = stmt.getBal();
      if (balList != null && !balList.isEmpty()) {
        for (CashBalance3 balanceEntry : balList) {
          sequence =
              createBalanceLine(bankDetails, balanceEntry, sequence, BALANCE_TYPE_INITIAL_BALANCE);
          if (sequence % 10 == 0) {
            JPA.clear();
            findBankStatement();
          }
        }
      }

      List<ReportEntry2> ntryList = stmt.getNtry();
      if (ntryList != null && !ntryList.isEmpty()) {
        for (ReportEntry2 ntry : ntryList) {
          sequence = createEntryLine(bankDetails, ntry, sequence);
          if (sequence % 10 == 0) {
            JPA.clear();
            findBankStatement();
          }
        }
      }

      if (balList != null && !balList.isEmpty()) {
        for (CashBalance3 balanceEntry : balList) {
          sequence =
              createBalanceLine(bankDetails, balanceEntry, sequence, BALANCE_TYPE_FINAL_BALANCE);
          if (sequence % 10 == 0) {
            JPA.clear();
            findBankStatement();
          }
        }
      }

    } catch (jakarta.xml.bind.JAXBException e) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          I18n.get("Error: File format unmarshalling process failed."));
    }
  }

  @Transactional
  protected int createEntryLine(BankDetails bankDetails, ReportEntry2 ntry, int sequence) {
    XMLGregorianCalendar opDate =
        Optional.of(ntry)
            .map(ReportEntry2::getBookgDt)
            .map(DateAndDateTimeChoice::getDtTm)
            .orElse(null);
    // sometimes the DtTm is null, then check Dt.
    if (opDate == null) {
      opDate =
          Optional.of(ntry)
              .map(ReportEntry2::getBookgDt)
              .map(DateAndDateTimeChoice::getDt)
              .orElse(null);
    }
    LocalDate operationDate = null;
    if (opDate != null) {
      operationDate = LocalDate.of(opDate.getYear(), opDate.getMonth(), opDate.getDay());
    }

    XMLGregorianCalendar valDate =
        Optional.of(ntry)
            .map(ReportEntry2::getValDt)
            .map(DateAndDateTimeChoice::getDtTm)
            .orElse(null);
    // sometimes the DtTm is null, then check Dt.
    if (valDate == null) {
      valDate =
          Optional.of(ntry)
              .map(ReportEntry2::getValDt)
              .map(DateAndDateTimeChoice::getDt)
              .orElse(null);
    }
    LocalDate valueDate = null;
    if (valDate != null) {
      valueDate = LocalDate.of(valDate.getYear(), valDate.getMonth(), valDate.getDay());
    }

    String description = addNtryInfoIntoDescription(ntry);

    String currencyCode =
        Optional.of(ntry)
            .map(ReportEntry2::getAmt)
            .map(ActiveOrHistoricCurrencyAndAmount::getCcy)
            .orElse(null);
    Currency currency = findCurrencyByCode(currencyCode);

    String creditOrDebit =
        Optional.of(ntry).map(ReportEntry2::getCdtDbtInd).map(CreditDebitCode::value).orElse(null);
    BigDecimal credit = BigDecimal.ZERO;
    BigDecimal debit = BigDecimal.ZERO;
    if (CREDIT_DEBIT_INDICATOR_CREDIT.equals(creditOrDebit)) {
      credit =
          Optional.of(ntry)
              .map(ReportEntry2::getAmt)
              .map(ActiveOrHistoricCurrencyAndAmount::getValue)
              .orElse(null);
    } else if (CREDIT_DEBIT_INDICATOR_DEBIT.equals(creditOrDebit)) {
      debit =
          Optional.of(ntry)
              .map(ReportEntry2::getAmt)
              .map(ActiveOrHistoricCurrencyAndAmount::getValue)
              .orElse(null);
    }

    String origin =
        Optional.ofNullable(ntry)
            .map(ReportEntry2::getNtryDtls)
            .flatMap(
                ntryDtls ->
                    ntryDtls.stream()
                        .findFirst()) // Convert to Stream and get first element if present
            .map(EntryDetails1::getTxDtls)
            .flatMap(txDtls -> txDtls.stream().findFirst()) // Same for TxDtls
            .map(EntryTransaction2::getRefs)
            .map(TransactionReferences2::getAcctSvcrRef)
            .orElse(null);

    String reference =
        Optional.ofNullable(ntry)
            .map(ReportEntry2::getNtryDtls)
            .flatMap(ntryDtls -> ntryDtls.stream().findFirst())
            .map(EntryDetails1::getTxDtls)
            .flatMap(txDtls -> txDtls.stream().findFirst())
            .map(EntryTransaction2::getRefs)
            .map(TransactionReferences2::getEndToEndId)
            .orElse(null);

    String interBankCodeLineCode = null;
    BankTransactionCodeStructure4 bkTxCd = ntry.getBkTxCd();
    BankTransactionCodeStructure5 domn = bkTxCd.getDomn();
    if (domn != null) {
      interBankCodeLineCode = domn.getCd();
    } else {
      ProprietaryBankTransactionCodeStructure1 prtry = bkTxCd.getPrtry();
      if (prtry != null) {
        interBankCodeLineCode = prtry.getCd();
      }
    }

    /* Possible interbankCode type values:
      int TYPE_OPERATION_CODE = 1;
      int TYPE_REJECT_RETURN_CODE = 2;
    */
    InterbankCodeLine interbankCodeLine = findInterBankCodeLineByCode(interBankCodeLineCode);
    InterbankCodeLine operationInterbankCodeLine = null;
    InterbankCodeLine rejectInterbankCodeLine = null;
    if (interbankCodeLine != null) {
      if (interbankCodeLine.getInterbankCode().getTypeSelect()
          == BankStatementLineCAMT53Repository.TYPE_OPERATION_CODE) {
        operationInterbankCodeLine = interbankCodeLine;
      } else {
        rejectInterbankCodeLine = interbankCodeLine;
      }
    }
    if (bankDetails != null) {
      bankDetails = bankDetailsRepository.find(bankDetails.getId());
    }
    BankStatementLineCAMT53 bankStatementLineCAMT53 =
        bankStatementLineCreationCAMT53Service.createBankStatementLine(
            findBankStatement(),
            sequence,
            bankDetails,
            debit,
            credit,
            currency,
            description,
            operationDate,
            valueDate,
            operationInterbankCodeLine,
            rejectInterbankCodeLine,
            origin,
            reference,
            BankStatementLineCAMT53Repository.LINE_TYPE_MOVEMENT);

    bankStatementLineCAMT53Repository.save(bankStatementLineCAMT53);
    return ++sequence;
  }

  /*
  TODO: In an xml file, there may be multiple balanceLines (balList.size >2), and multiple entryLines.
        For example, we have 2 init balance lines and 2 final balance lines.
        Then we need to find a way to assign sequence number correctly to all balanceLines and entryLines.
   */
  @Transactional
  protected int createBalanceLine(
      BankDetails bankDetails,
      CashBalance3 balanceEntry,
      int sequence,
      String balanceTypeRequired) {
    int lineTypeSelect = 0;
    String balanceType =
        Optional.of(balanceEntry)
            .map(CashBalance3::getTp)
            .map(BalanceType12::getCdOrPrtry)
            .map(BalanceType5Choice::getCd)
            .map(BalanceType12Code::value)
            .orElse(null);
    if (!balanceTypeRequired.equals(balanceType)) {
      return sequence;
    }
    if (BALANCE_TYPE_INITIAL_BALANCE.equals(balanceType)) {
      // Initial balance
      lineTypeSelect = 1;
    } else if (BALANCE_TYPE_FINAL_BALANCE.equals(balanceType)) {
      // Final balance
      lineTypeSelect = 3;
    }

    XMLGregorianCalendar date =
        Optional.of(balanceEntry)
                    .map(CashBalance3::getDt)
                    .map(DateAndDateTimeChoice::getDt)
                    .orElse(null)
                != null
            ? Optional.of(balanceEntry)
                .map(CashBalance3::getDt)
                .map(DateAndDateTimeChoice::getDt)
                .orElse(null)
            : Optional.of(balanceEntry)
                .map(CashBalance3::getDt)
                .map(DateAndDateTimeChoice::getDtTm)
                .orElse(null);
    LocalDate operationDate = null;
    if (date != null) {
      operationDate = LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    String currencyCode =
        Optional.of(balanceEntry)
            .map(CashBalance3::getAmt)
            .map(ActiveOrHistoricCurrencyAndAmount::getCcy)
            .orElse(null);
    Currency currency = findCurrencyByCode(currencyCode);

    // set credit or debit
    String creditOrDebit =
        Optional.of(balanceEntry)
            .map(CashBalance3::getCdtDbtInd)
            .map(CreditDebitCode::value)
            .orElse(null);
    BigDecimal credit = BigDecimal.ZERO;
    BigDecimal debit = BigDecimal.ZERO;
    if (CREDIT_DEBIT_INDICATOR_CREDIT.equals(creditOrDebit)) {
      credit =
          Optional.of(balanceEntry)
              .map(CashBalance3::getAmt)
              .map(ActiveOrHistoricCurrencyAndAmount::getValue)
              .orElse(null);

    } else if (CREDIT_DEBIT_INDICATOR_DEBIT.equals(creditOrDebit)) {
      debit =
          Optional.of(balanceEntry)
              .map(CashBalance3::getAmt)
              .map(ActiveOrHistoricCurrencyAndAmount::getValue)
              .orElse(null);
    }
    if (bankDetails != null) {
      bankDetails = bankDetailsRepository.find(bankDetails.getId());
    }
    BankStatementLineCAMT53 bankStatementLineCAMT53 =
        bankStatementLineCreationCAMT53Service.createBankStatementLine(
            findBankStatement(),
            sequence,
            bankDetails,
            debit,
            credit,
            currency,
            null,
            operationDate,
            null,
            null,
            null,
            null,
            null,
            lineTypeSelect);

    updateBankStatementDate(operationDate, lineTypeSelect);
    bankStatementLineCAMT53Repository.save(bankStatementLineCAMT53);
    return ++sequence;
  }

  /**
   * Find the interbankCodeLineCodeLine by the input code.
   *
   * @param interbankCodeLineCode
   * @return InterbankCodeLine obj
   */
  protected InterbankCodeLine findInterBankCodeLineByCode(String interbankCodeLineCode) {
    return interbankCodeLineRepository
        .all()
        .filter("self.code = :code")
        .bind("code", interbankCodeLineCode)
        .fetchOne();
  }

  protected void updateBankStatementDate(LocalDate operationDate, int lineType) {
    if (operationDate == null) {
      return;
    }

    if (ObjectUtils.notEmpty(bankStatement.getFromDate())
        && lineType == BankStatementLineCAMT53Repository.LINE_TYPE_INITIAL_BALANCE) {
      if (operationDate.isBefore(bankStatement.getFromDate()))
        bankStatement.setFromDate(operationDate);
    } else if (lineType == BankStatementLineCAMT53Repository.LINE_TYPE_INITIAL_BALANCE) {
      bankStatement.setFromDate(operationDate);
    }

    if (ObjectUtils.notEmpty(bankStatement.getToDate())
        && lineType == BankStatementLineCAMT53Repository.LINE_TYPE_FINAL_BALANCE) {
      if (operationDate.isAfter(bankStatement.getToDate())) {
        bankStatement.setToDate(operationDate);
      }
    } else {
      if (lineType == BankStatementLineCAMT53Repository.LINE_TYPE_FINAL_BALANCE) {
        bankStatement.setToDate(operationDate);
      }
    }
  }

  protected Currency findCurrencyByCode(String currencyCode) {
    Currency currency = null;
    Query query =
        JPA.em()
            .createQuery(
                "select self.id " + "from Currency as self " + "where self.code = :currencyCode")
            .setParameter("currencyCode", currencyCode);
    List resultList = query.getResultList();
    if (!resultList.isEmpty()) {
      long currencyId = (long) resultList.get(0);
      currency = Beans.get(CurrencyRepository.class).find(currencyId);
    }
    return currency;
  }

  @Transactional
  protected BankStatement setBankStatementFrToDt(
      DateTimePeriodDetails frToDt, BankStatement bankStatement) {
    // Bank Statement From Date
    if (frToDt == null) {
      return bankStatement;
    }
    XMLGregorianCalendar frDtTm = frToDt.getFrDtTm();
    if (frDtTm == null) {
      return bankStatement;
    }
    LocalDate fromDate = LocalDate.of(frDtTm.getYear(), frDtTm.getMonth(), frDtTm.getDay());
    // Bank Statement To Date
    XMLGregorianCalendar toDtTm = frToDt.getToDtTm();
    if (toDtTm == null) {
      return bankStatement;
    }
    LocalDate toDate = LocalDate.of(toDtTm.getYear(), toDtTm.getMonth(), toDtTm.getDay());
    bankStatement.setFromDate(fromDate);
    bankStatement.setToDate(toDate);
    bankStatementRepository.save(bankStatement);
    return bankStatement;
  }

  protected BankDetails findBankDetailsByIban(String ibanOrOthers) {
    return bankDetailsRepository.all().filter("self.iban = ?1", ibanOrOthers).fetchOne();
  }

  protected String addNtryInfoIntoDescription(ReportEntry2 ntry) {
    // <Ntry> -> <NtryDtls> -> <TxDtls>
    EntryTransaction2 txDtl =
        Optional.of(ntry)
            .map(ReportEntry2::getNtryDtls)
            .flatMap(
                ntryDtls ->
                    ntryDtls.stream()
                        .findFirst()) // Convert to Stream and get first element if present
            .map(EntryDetails1::getTxDtls)
            .flatMap(txDtls -> txDtls.stream().findFirst())
            .orElse(null);

    StringBuilder descriptionSb = new StringBuilder();
    // <Ntry> -> <BkTxCd> -> <Prtry> -> <Issr>
    String issr =
        Optional.of(ntry)
            .map(ReportEntry2::getBkTxCd)
            .map(BankTransactionCodeStructure4::getPrtry)
            .map(ProprietaryBankTransactionCodeStructure1::getIssr)
            .orElse(null);
    if (issr != null && !issr.isEmpty()) {
      descriptionSb.append("Ntry.BkTxCd.Prtry.Issr=");
      descriptionSb.append(issr);
      descriptionSb.append(";");
    }
    if (txDtl == null) {
      if (descriptionSb.length() >= 1 && descriptionSb.charAt(descriptionSb.length() - 1) == ';') {
        descriptionSb.deleteCharAt(descriptionSb.length() - 1);
      }
      return descriptionSb.toString();
    } else {
      // <TxDtls> -> <Refs> -> <ChqNb>
      String cheqNb =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRefs)
              .map(TransactionReferences2::getChqNb)
              .orElse(null);
      if (cheqNb != null && !cheqNb.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.Refs.ChqNb=");
        descriptionSb.append(cheqNb);
        descriptionSb.append(";");
      }

      // <TxDtls> -> <Refs> -> <InstrId>
      String instrId =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRefs)
              .map(TransactionReferences2::getInstrId)
              .orElse(null);
      if (instrId != null && !instrId.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.Refs.InstrId=");
        descriptionSb.append(instrId);
        descriptionSb.append(";");
      }

      // <TxDtls> -> <Refs> -> <MndtId>
      String mndtId =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRefs)
              .map(TransactionReferences2::getMndtId)
              .orElse(null);
      if (mndtId != null && !mndtId.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.Refs.MndtId=");
        descriptionSb.append(mndtId);
        descriptionSb.append(";");
      }

      // <TxDtls> -> <RltdPties> -> <Cdtr> -> <Nm>
      String cdtrNm =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRltdPties)
              .map(TransactionParty2::getCdtr)
              .map(PartyIdentification32::getNm)
              .orElse(null);
      if (cdtrNm != null && !cdtrNm.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.RltdPties.Cdtr.Nm=");
        descriptionSb.append(cdtrNm);
        descriptionSb.append(";");
      }

      /*
      TODO: To be discussed:
            The "cdtrId" is still an object, but it doesn't have a toString method.
            It has the following sub-structure:
              <Id> ---
                  <OrgId>
                      ...
                  <PrvtId>
                      ...
            Check Payments_Maintenance_2009.pdf page 1008.
       */
      // <TxDtls> -> <RltdPties> -> <Cdtr> -> <Id>
      String cdtrIdString = null;
      Party6Choice cdtrId =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRltdPties)
              .map(TransactionParty2::getCdtr)
              .map(PartyIdentification32::getId)
              .orElse(null);
      if (cdtrId != null) {
        cdtrIdString = cdtrId.toString();
      }
      if (cdtrIdString != null && !cdtrIdString.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.RltdPties.Cdtr.Id=");
        descriptionSb.append(cdtrIdString);
        descriptionSb.append(";");
      }

      // <TxDtls> -> <RltdPties> -> <UltmtCdtr> -> <Nm>
      String ultmtCdtrNm =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRltdPties)
              .map(TransactionParty2::getUltmtCdtr)
              .map(PartyIdentification32::getNm)
              .orElse(null);
      if (ultmtCdtrNm != null && !ultmtCdtrNm.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.RltdPties.UltmtCdtr.Nm=");
        descriptionSb.append(ultmtCdtrNm);
        descriptionSb.append(";");
      }

      /*
      TODO: It has the same issue as the "cdtrId".
       */
      // <TxDtls> -> <RltdPties> -> <UltmtCdtr> -> <Id>
      Party6Choice ultmtCdtrId =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRltdPties)
              .map(TransactionParty2::getUltmtCdtr)
              .map(PartyIdentification32::getId)
              .orElse(null);
      String ultmtCdtrIdString = null;
      if (ultmtCdtrId != null) {
        ultmtCdtrIdString = ultmtCdtrId.toString();
      }
      if (ultmtCdtrIdString != null && !ultmtCdtrIdString.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.RltdPties.UltmtCdtr.Id=");
        descriptionSb.append(ultmtCdtrIdString);
        descriptionSb.append(";");
      }

      // <TxDtls> -> <RltdPties> -> <Dbtr> -> <Nm>
      String dbtrNm =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRltdPties)
              .map(TransactionParty2::getDbtr)
              .map(PartyIdentification32::getNm)
              .orElse(null);
      if (dbtrNm != null && !dbtrNm.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.RltdPties.Dbtr.Nm=");
        descriptionSb.append(dbtrNm);
        descriptionSb.append(";");
      }

      /*
      TODO: It has the same issue as the "cdtrId".
       */
      // <TxDtls> -> <RltdPties> -> <Dbtr> -> <Id>
      Party6Choice dbtrId =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRltdPties)
              .map(TransactionParty2::getDbtr)
              .map(PartyIdentification32::getId)
              .orElse(null);
      String dbtrIdString = null;
      if (dbtrId != null) {
        dbtrIdString = dbtrId.toString();
      }
      if (dbtrIdString != null && !dbtrIdString.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.RltdPties.Dbtr.Id=");
        descriptionSb.append(dbtrId);
        descriptionSb.append(";");
      }

      // <TxDtls> -> <RmtInf> -> <Ustrd>
      List<String> unstructuredLines =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRmtInf)
              .map(RemittanceInformation5::getUstrd)
              .orElse(null);
      if (unstructuredLines != null && !unstructuredLines.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.RmtInf.Ustrd=");
        for (String unstructuredLine : unstructuredLines) {
          descriptionSb.append(unstructuredLine);
          descriptionSb.append(",");
        }
        if (descriptionSb.length() >= 1
            && descriptionSb.charAt(descriptionSb.length() - 1) == ',') {
          descriptionSb.deleteCharAt(descriptionSb.length() - 1);
        }
        descriptionSb.append(";");
      }

      // <TxDtls> -> <Rmtlnf> -> <Strd> -> <CdtrReflnf> -><Ref>
      String strdCdtrRefInfref =
          Optional.of(txDtl)
              .map(EntryTransaction2::getRmtInf)
              .map(RemittanceInformation5::getStrd)
              .flatMap(strds -> strds.stream().findFirst())
              .map(StructuredRemittanceInformation7::getCdtrRefInf)
              .map(CreditorReferenceInformation2::getRef)
              .orElse(null);
      if (strdCdtrRefInfref != null && !strdCdtrRefInfref.isEmpty()) {
        descriptionSb.append("Ntry.TxDtls.RmtInf.Strd.CdtrRefInf.Ref=");
        descriptionSb.append(strdCdtrRefInfref);
        descriptionSb.append(";");
      }

      // <TxDtls> -> <AddtlTxInf>
      String addtlTxInf = Optional.of(txDtl).map(EntryTransaction2::getAddtlTxInf).orElse(null);
      if (addtlTxInf != null && !addtlTxInf.isEmpty()) {
        descriptionSb.append("RmtInf.AddtlTxInf=");
        descriptionSb.append(addtlTxInf);
      }
    }

    if (descriptionSb.length() >= 1 && descriptionSb.charAt(descriptionSb.length() - 1) == ';') {
      descriptionSb.deleteCharAt(descriptionSb.length() - 1);
    }
    return descriptionSb.toString();
  }
}

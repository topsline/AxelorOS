package com.axelor.apps.hr.service.expense;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.Expense;
import com.axelor.apps.hr.db.ExpenseLine;
import com.axelor.apps.hr.db.KilometricAllowParam;
import com.axelor.apps.hr.db.repo.ExpenseLineRepository;
import com.axelor.apps.hr.db.repo.ExpenseRepository;
import com.axelor.apps.hr.exception.HumanResourceExceptionMessage;
import com.axelor.apps.project.db.Project;
import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseLineUpdateServiceImpl implements ExpenseLineUpdateService {

  protected ExpenseToolService expenseToolService;
  protected ExpenseComputationService expenseComputationService;
  protected ExpenseLineToolService expenseLineToolService;
  protected ExpenseLineRepository expenseLineRepository;
  protected ExpenseRepository expenseRepository;

  @Inject
  public ExpenseLineUpdateServiceImpl(
      ExpenseToolService expenseToolService,
      ExpenseComputationService expenseComputationService,
      ExpenseLineToolService expenseLineToolService,
      ExpenseLineRepository expenseLineRepository,
      ExpenseRepository expenseRepository) {
    this.expenseToolService = expenseToolService;
    this.expenseComputationService = expenseComputationService;
    this.expenseLineToolService = expenseLineToolService;
    this.expenseLineRepository = expenseLineRepository;
    this.expenseRepository = expenseRepository;
  }

  @Transactional(rollbackOn = {Exception.class})
  @Override
  public ExpenseLine updateExpenseLine(
      ExpenseLine expenseLine,
      Project project,
      Product expenseProduct,
      LocalDate expenseDate,
      KilometricAllowParam kilometricAllowParam,
      Integer kilometricType,
      BigDecimal distance,
      String fromCity,
      String toCity,
      BigDecimal totalAmount,
      BigDecimal totalTax,
      MetaFile justificationMetaFile,
      String comments,
      Employee employee,
      Currency currency,
      Boolean toInvoice,
      Expense newExpense)
      throws AxelorException {
    if (expenseLineToolService.isKilometricExpenseLine(expenseLine)) {
      updateKilometricExpenseLine(
          expenseLine,
          project,
          expenseDate,
          kilometricAllowParam,
          kilometricType,
          distance,
          fromCity,
          toCity,
          comments,
          employee,
          currency,
          toInvoice,
          expenseProduct);
    } else {
      updateGeneralExpenseLine(
          expenseLine,
          project,
          expenseProduct,
          expenseDate,
          totalAmount,
          totalTax,
          justificationMetaFile,
          comments,
          employee,
          currency,
          toInvoice);
    }

    if (newExpense != null) {
      changeLineParentExpense(expenseLine, newExpense);
      return expenseLine;
    }

    Expense expense = expenseLine.getExpense();
    if (expense != null) {
      expenseComputationService.compute(expense);
    }

    return expenseLine;
  }

  @Transactional(rollbackOn = {Exception.class})
  @Override
  public void updateGeneralExpenseLine(
      ExpenseLine expenseLine,
      Project project,
      Product expenseProduct,
      LocalDate expenseDate,
      BigDecimal totalAmount,
      BigDecimal totalTax,
      MetaFile justificationMetaFile,
      String comments,
      Employee employee,
      Currency currency,
      Boolean toInvoice)
      throws AxelorException {

    checkParentStatus(expenseLine.getExpense());
    updateBasicExpenseLine(
        expenseLine, project, employee, expenseDate, comments, currency, expenseProduct, toInvoice);
    expenseLineToolService.setGeneralExpenseLineInfo(
        expenseProduct, totalAmount, totalTax, justificationMetaFile, expenseLine);
  }

  @Transactional(rollbackOn = {Exception.class})
  @Override
  public void updateKilometricExpenseLine(
      ExpenseLine expenseLine,
      Project project,
      LocalDate expenseDate,
      KilometricAllowParam kilometricAllowParam,
      Integer kilometricType,
      BigDecimal distance,
      String fromCity,
      String toCity,
      String comments,
      Employee employee,
      Currency currency,
      Boolean toInvoice,
      Product expenseProduct)
      throws AxelorException {
    checkParentStatus(expenseLine.getExpense());
    updateBasicExpenseLine(
        expenseLine, project, employee, expenseDate, comments, currency, expenseProduct, toInvoice);
    updateKilometricExpenseLineInfo(
        expenseLine, kilometricAllowParam, kilometricType, fromCity, toCity, distance);
  }

  protected void changeLineParentExpense(ExpenseLine expenseLine, Expense newExpense)
      throws AxelorException {
    Expense oldExpense = expenseLine.getExpense();
    if (oldExpense == null) {
      expenseToolService.addExpenseLineToExpenseAndCompute(newExpense, expenseLine);
    } else {
      Long oldExpenseId = oldExpense.getId();
      changeParent(expenseLine, newExpense);
      Expense oldExpenseToCompute = expenseRepository.find(oldExpenseId);
      expenseComputationService.compute(oldExpenseToCompute);
    }
  }

  @Transactional(rollbackOn = {Exception.class})
  protected void changeParent(ExpenseLine expenseLine, Expense newExpense) throws AxelorException {
    Expense oldExpense = expenseLine.getExpense();
    if (oldExpense == null) {
      return;
    }
    checkParentStatus(newExpense, oldExpense);
    expenseLine.setExpense(newExpense);
    expenseToolService.addExpenseLineToExpense(newExpense, expenseLine);
    expenseComputationService.compute(newExpense);
    expenseLineRepository.save(expenseLine);
  }

  protected void checkParentStatus(Expense newExpense, Expense oldExpense) throws AxelorException {
    if (oldExpense.getStatusSelect() != ExpenseRepository.STATUS_DRAFT) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.EXPENSE_LINE_PARENT_NOT_DRAFT));
    }

    if (newExpense.getStatusSelect() != ExpenseRepository.STATUS_DRAFT) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.EXPENSE_LINE_NEW_EXPENSE_NOT_DRAFT));
    }
  }

  protected void updateBasicExpenseLine(
      ExpenseLine expenseLine,
      Project project,
      Employee employee,
      LocalDate expenseDate,
      String comments,
      Currency currency,
      Product expenseProduct,
      Boolean toInvoice) {
    if (project != null) {
      expenseLine.setProject(project);
    }
    if (comments != null) {
      expenseLine.setComments(comments);
    }
    if (employee != null) {
      expenseLine.setEmployee(employee);
    }
    if (currency != null) {
      expenseLine.setCurrency(currency);
    }
    if (expenseDate != null) {
      expenseLine.setExpenseDate(expenseDate);
    }
    if (expenseProduct != null) {
      expenseLine.setExpenseProduct(expenseProduct);
    }
    if (toInvoice != null) {
      expenseLine.setToInvoice(toInvoice);
    }
  }

  protected void updateKilometricExpenseLineInfo(
      ExpenseLine expenseLine,
      KilometricAllowParam kilometricAllowParam,
      Integer kilometricType,
      String fromCity,
      String toCity,
      BigDecimal distance)
      throws AxelorException {
    if (kilometricType != null) {
      expenseLine.setKilometricTypeSelect(kilometricType);
    }
    if (kilometricAllowParam != null) {
      expenseLine.setKilometricAllowParam(kilometricAllowParam);
    }
    if (StringUtils.notEmpty(fromCity)) {
      expenseLine.setFromCity(fromCity);
    }
    if (StringUtils.notEmpty(toCity)) {
      expenseLine.setToCity(toCity);
    }

    expenseLineToolService.computeDistance(distance, expenseLine);
    expenseLineToolService.computeAmount(expenseLine.getEmployee(), expenseLine);

    expenseLineRepository.save(expenseLine);
  }

  void checkParentStatus(Expense expense) throws AxelorException {
    if (expense == null) {
      return;
    }
    if (expense.getStatusSelect() != ExpenseRepository.STATUS_DRAFT) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.EXPENSE_LINE_EXPENSE_NOT_DRAFT));
    }
  }
}

package com.axelor.apps.budget.exception;

public interface IExceptionMessage {

  static final String MISSING_ADVANCED_EXPORT = /*$$(*/
      "Missing required advanced export(s)." /*)*/;

  static final String BUDGET_IS_MISSING = /*$$(*/ "Please select a budget with Id." /*)*/;

  static final String MISSING_ACCOUNTS_IN_COMPANY = /*$$(*/
      "Error : Following accounts are not found %s" /*)*/;

  static final String BUDGET_DISTRIBUTION_LINE_SUM_GREATER_INVOICE = /*$$(*/
      "The budget distribution amount for budget %s exceeds the amount of the invoice line %s, please correct it" /*)*/;

  static final String BUDGET_DISTRIBUTION_LINE_SUM_GREATER_MOVE = /*$$(*/
      "The budget distribution amount for budget %s exceeds the amount of the move line %s, please correct it" /*)*/;

  static final String BUDGET_DISTRIBUTION_LINE_SUM_GREATER_PO = /*$$(*/
      "The budget distribution amount for budget %s exceeds the amount of the order line %s, please correct it" /*)*/;

  static final String ERROR_CONFIG_BUDGET_KEY = /*$$(*/
      "When budget key is enabled, you must check at least one line on analytic axis to be included in budget key computation" /*)*/;

  static final String BUDGET_ANALYTIC_EMPTY = /*$$(*/
      "The budget key is enabled in the company %s account configuration. Thus, you must fill the analytic axis and the analytic account before validating in order to ensure its generation (Budget %s)" /*)*/;

  static final String BUDGET_ACCOUNT_EMPTY = /*$$(*/
      "The budget key is enabled in the company %s account configuration. Thus, you must fill at least one accounting account before validating in order to ensure its generation (Budget %s)" /*)*/;

  static final String BUDGET_SAME_BUDGET_KEY = /*$$(*/
      "There is already a budget key using the same combination of company, dates, accounts and analytic accounts and axis than the budget line %s" /*)*/;

  static final String BUDGET_KEY_NOT_FOUND = /*$$(*/
      "No budget could be reconciled with the data entered for following lines : %s" /*)*/;

  static final String BUDGET_MISSING_BUDGET_KEY = /*$$(*/
      "The budget key is missing in budget %s. Please fill account and analytic distribution configuration in budget before validating in order to ensure its generation" /*)*/;

  static final String BUDGET_EXCEED_ORDER_LINE_AMOUNT = /*$$(*/
      "The budget distribution amount exceed the amount on the order line with product %s, please correct it" /*)*/;

  static final String BUDGET_ROLE_NOT_IN_BUDGET_DISTRIBUTION_AUTHORIZED_LIST = /*$$(*/
      "You can't compute the budget distribution because you are not authorized to." /*)*/;

  static final String NO_BUDGET_VALUES_FOUND = /*$$(*/
      "The budget distribution has not been computed yet. By launching this action, you can no longer compute the budget distribution. Do you want to proceed ?" /*)*/;

  static final String WRONG_DATES_ON_BUDGET = /*$$(*/
      "Please select valid dates for budget %s, dates needs to be in the section period" /*)*/;

  static final String WRONG_DATES_ON_BUDGET_LINE = /*$$(*/
      "Please select valid dates for budget lines in budget %s, dates need to be in the budget period" /*)*/;

  static final String BUDGET_LINES_ON_SAME_PERIOD = /*$$(*/
      "Please select valid dates for budget lines in budget %s, budget lines need to be on a separate period" /*)*/;

  static final String WRONG_DATES_ON_BUDGET_LEVEL = /*$$(*/
      "Please select valid dates for budget level %s, dates needs to be in the parent period" /*)*/;

  public static final String ADVANCED_IMPORT_IMPORT_DATA = /*$$(*/
      "Data imported successfully" /*)*/;

  public static final String NO_BUDGET_DISTRIBUTION_GENERATED = /*$$(*/
      "The budget distribution has not been computed yet. By launching this action, you can no longer compute the budget distribution. Do you want to proceed ?" /*)*/;
}

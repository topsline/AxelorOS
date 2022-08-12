package com.axelor.apps.account.service.fixedasset;

import static com.axelor.apps.account.service.fixedasset.FixedAssetServiceImpl.CALCULATION_SCALE;
import static com.axelor.apps.account.service.fixedasset.FixedAssetServiceImpl.RETURNED_SCALE;

import com.axelor.apps.account.db.FixedAsset;
import com.axelor.apps.account.db.FixedAssetLine;
import com.axelor.apps.account.db.repo.FixedAssetLineRepository;
import com.axelor.apps.account.db.repo.FixedAssetRepository;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.tool.date.DateTool;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class of FixedAssetLineComputationService. This class is not supposed to be directly
 * used. Please use {@link FixedAssetLineEconomicComputationServiceImpl} or {@link
 * FixedAssetLineFiscalComputationServiceImpl}.
 */
public abstract class AbstractFixedAssetLineComputationServiceImpl
    implements FixedAssetLineComputationService {

  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected FixedAssetFailOverControlService fixedAssetFailOverControlService;
  protected AppBaseService appBaseService;

  protected abstract LocalDate computeStartDepreciationDate(FixedAsset fixedAsset);

  protected abstract BigDecimal computeInitialDepreciationBase(FixedAsset fixedAsset);

  protected abstract List<FixedAssetLine> getFixedAssetLineList(FixedAsset fixedAsset);

  protected abstract BigDecimal getNumberOfDepreciation(FixedAsset fixedAsset);

  protected abstract BigDecimal getNumberOfPastDepreciation(FixedAsset fixedAsset);

  protected abstract BigDecimal getAlreadyDepreciatedAmount(FixedAsset fixedAsset);

  protected abstract String getComputationMethodSelect(FixedAsset fixedAsset);

  protected abstract Integer getDurationInMonth(FixedAsset fixedAsset);

  protected abstract BigDecimal getDegressiveCoef(FixedAsset fixedAsset);

  protected abstract LocalDate computeProrataTemporisFirstDepreciationDate(FixedAsset fixedAsset);

  protected abstract LocalDate computeProrataTemporisAcquisitionDate(FixedAsset fixedAsset);

  protected abstract Integer getPeriodicityInMonth(FixedAsset fixedAsset);

  protected abstract Integer getTypeSelect();

  protected abstract Boolean isProrataTemporis(FixedAsset fixedAsset);

  protected abstract BigDecimal getDepreciatedAmountCurrentYear(FixedAsset fixedAsset);

  protected abstract LocalDate getFailOverDepreciationEndDate(FixedAsset fixedAsset);

  @Inject
  public AbstractFixedAssetLineComputationServiceImpl(
      FixedAssetFailOverControlService fixedAssetFailOverControlService,
      AppBaseService appBaseService) {
    this.fixedAssetFailOverControlService = fixedAssetFailOverControlService;
    this.appBaseService = appBaseService;
  }

  @Override
  public FixedAssetLine computePlannedFixedAssetLine(
      FixedAsset fixedAsset, FixedAssetLine previousFixedAssetLine) throws AxelorException {

    BigDecimal depreciation =
        computeDepreciation(
            fixedAsset, previousFixedAssetLine, previousFixedAssetLine.getDepreciationBase());
    BigDecimal depreciationBase = computeDepreciationBase(fixedAsset, previousFixedAssetLine);
    BigDecimal cumulativeDepreciation =
        previousFixedAssetLine.getCumulativeDepreciation().add(depreciation);
    BigDecimal accountingValue = getAccountingValue(previousFixedAssetLine).subtract(depreciation);

    LocalDate depreciationDate = computeDepreciationDate(fixedAsset, previousFixedAssetLine);

    return createFixedAssetLine(
        fixedAsset,
        depreciationDate,
        depreciation,
        cumulativeDepreciation,
        accountingValue,
        depreciationBase,
        getTypeSelect(),
        FixedAssetLineRepository.STATUS_PLANNED);
  }

  @Override
  public Optional<FixedAssetLine> computeInitialPlannedFixedAssetLine(FixedAsset fixedAsset)
      throws AxelorException {

    LocalDate firstDepreciationDate;
    firstDepreciationDate = computeStartDepreciationDate(fixedAsset);
    BigDecimal depreciationBase = computeInitialDepreciationBase(fixedAsset);
    BigDecimal depreciation = BigDecimal.ZERO;
    BigDecimal accountingValue = BigDecimal.ZERO;
    if (!isAlreadyDepreciated(fixedAsset)) {
      depreciation = computeInitialDepreciation(fixedAsset, depreciationBase);
      accountingValue = depreciationBase.subtract(depreciation);
    }

    FixedAssetLine line =
        createFixedAssetLine(
            fixedAsset,
            firstDepreciationDate,
            depreciation,
            depreciation,
            accountingValue,
            depreciationBase,
            getTypeSelect(),
            FixedAssetLineRepository.STATUS_PLANNED);

    if (fixedAssetFailOverControlService.isFailOver(fixedAsset)) {
      if (isAlreadyDepreciated(fixedAsset)
          || line.getDepreciationBase().equals(getAlreadyDepreciatedAmount(fixedAsset))) {

        // Instead of producing 0 line, we will produce one line with the depreciation of the
        // current year
        // if the depreciation ended this year.
        if (getFailOverDepreciationEndDate(fixedAsset) != null
            && appBaseService.getTodayDate(fixedAsset.getCompany()).getYear()
                == getFailOverDepreciationEndDate(fixedAsset).getYear()) {
          return Optional.ofNullable(
              createFixedAssetLine(
                  fixedAsset,
                  getFailOverDepreciationEndDate(fixedAsset),
                  getDepreciatedAmountCurrentYear(fixedAsset),
                  depreciationBase,
                  BigDecimal.ZERO,
                  depreciationBase,
                  getTypeSelect(),
                  FixedAssetLineRepository.STATUS_REALIZED));
        } else {
          return Optional.ofNullable(
              createFixedAssetLine(
                  fixedAsset,
                  getFailOverDepreciationEndDate(fixedAsset),
                  BigDecimal.ZERO,
                  depreciationBase,
                  BigDecimal.ZERO,
                  depreciationBase,
                  getTypeSelect(),
                  FixedAssetLineRepository.STATUS_REALIZED));
        }
      }
      line.setCumulativeDepreciation(
          line.getCumulativeDepreciation().add(getAlreadyDepreciatedAmount(fixedAsset)));
      if (getComputationMethodSelect(fixedAsset)
          .equals(FixedAssetRepository.COMPUTATION_METHOD_LINEAR)) {
        line.setAccountingValue(
            line.getAccountingValue().subtract(getAlreadyDepreciatedAmount(fixedAsset)));
      }
      if (line.getDepreciationBase().equals(getAlreadyDepreciatedAmount(fixedAsset))) {
        return Optional.empty();
      }
    }

    return Optional.ofNullable(line);
  }

  protected boolean isAlreadyDepreciated(FixedAsset fixedAsset) {

    return getNumberOfDepreciation(fixedAsset).equals(getNumberOfPastDepreciation(fixedAsset));
  }

  @Override
  public void multiplyLineBy(FixedAssetLine line, BigDecimal prorata) {
    line.setDepreciationBase(
        prorata
            .multiply(line.getDepreciationBase())
            .setScale(RETURNED_SCALE, RoundingMode.HALF_UP));
    line.setDepreciation(
        prorata.multiply(line.getDepreciation()).setScale(RETURNED_SCALE, RoundingMode.HALF_UP));
    line.setCumulativeDepreciation(
        prorata
            .multiply(line.getCumulativeDepreciation())
            .setScale(RETURNED_SCALE, RoundingMode.HALF_UP));
    line.setAccountingValue(
        prorata.multiply(line.getAccountingValue()).setScale(RETURNED_SCALE, RoundingMode.HALF_UP));
    line.setCorrectedAccountingValue(
        prorata
            .multiply(line.getCorrectedAccountingValue())
            .setScale(RETURNED_SCALE, RoundingMode.HALF_UP));
    line.setImpairmentValue(
        prorata.multiply(line.getImpairmentValue()).setScale(RETURNED_SCALE, RoundingMode.HALF_UP));
  }

  @Override
  public void multiplyLinesBy(List<FixedAssetLine> fixedAssetLineList, BigDecimal prorata) {
    if (fixedAssetLineList != null) {
      fixedAssetLineList.forEach(line -> multiplyLineBy(line, prorata));
    }
  }

  protected BigDecimal computeInitialDepreciation(FixedAsset fixedAsset, BigDecimal baseValue) {
    // Theses cases is for when user want to depreciate in one year.
    // This case is if list is not empty when calling this method
    if (getFixedAssetLineList(fixedAsset) != null
        && numberOfDepreciationDone(fixedAsset)
            .equals(getNumberOfDepreciation(fixedAsset).subtract(BigDecimal.ONE))) {
      if (fixedAssetFailOverControlService.isFailOver(fixedAsset)
          && getComputationMethodSelect(fixedAsset)
              .equals(FixedAssetRepository.COMPUTATION_METHOD_LINEAR)) {
        return baseValue.subtract(getAlreadyDepreciatedAmount(fixedAsset));
      }
      return baseValue;
    }
    if (getFixedAssetLineList(fixedAsset) == null
        && getNumberOfDepreciation(fixedAsset)
            .subtract(numberOfDepreciationDone(fixedAsset))
            .equals(BigDecimal.ONE)) {
      return baseValue.subtract(getAlreadyDepreciatedAmount(fixedAsset));
    }
    if (getComputationMethodSelect(fixedAsset) != null
        && getComputationMethodSelect(fixedAsset)
            .equals(FixedAssetRepository.COMPUTATION_METHOD_DEGRESSIVE)) {

      return computeInitialDegressiveDepreciation(fixedAsset, baseValue);
    } else {
      return computeInitialLinearDepreciation(fixedAsset, baseValue);
    }
  }

  protected BigDecimal computeInitialLinearDepreciation(
      FixedAsset fixedAsset, BigDecimal baseValue) {
    return computeInitialDepreciationNumerator(baseValue, fixedAsset)
        .setScale(RETURNED_SCALE, RoundingMode.HALF_UP);
  }

  protected BigDecimal computeDepreciationNumerator(
      BigDecimal baseValue, BigDecimal numberOfDepreciation) {
    BigDecimal depreciationRate = computeDepreciationRate(numberOfDepreciation);
    return baseValue.multiply(depreciationRate);
  }

  protected BigDecimal computeDepreciationRate(BigDecimal numberOfDepreciation) {
    return numberOfDepreciation.signum() == 0
        ? BigDecimal.ZERO
        : BigDecimal.ONE.divide(numberOfDepreciation, CALCULATION_SCALE, RoundingMode.HALF_UP);
  }

  protected BigDecimal computeProrataTemporis(FixedAsset fixedAsset) {
    BigDecimal prorataTemporis = BigDecimal.ONE;

    LocalDate acquisitionDate = computeProrataTemporisAcquisitionDate(fixedAsset);
    LocalDate depreciationDate = computeProrataTemporisFirstDepreciationDate(fixedAsset);

    if (isProrataTemporis(fixedAsset) && !acquisitionDate.equals(depreciationDate)) {
      prorataTemporis = computeProrataBetween(fixedAsset, acquisitionDate, depreciationDate);
    }
    return prorataTemporis;
  }

  protected BigDecimal computeProrataBetween(
      FixedAsset fixedAsset, LocalDate acquisitionDate, LocalDate depreciationDate) {
    BigDecimal prorataTemporis;
    acquisitionDate = acquisitionDate.minusDays(1);
    int acquisitionYear = acquisitionDate.getYear();
    Month acquisitionMonth = acquisitionDate.getMonth();
    int acquisitionDay = acquisitionDate.getDayOfMonth();
    int depreciationYear = depreciationDate.getYear();
    Month depreciationMonth = depreciationDate.getMonth();
    int depreciationDay = depreciationDate.getDayOfMonth();

    // US way
    if (fixedAsset.getFixedAssetCategory().getIsUSProrataTemporis()) {

      if (acquisitionMonth == Month.FEBRUARY
          && depreciationMonth == Month.FEBRUARY
          && isLastDayOfFebruary(acquisitionYear, acquisitionDay)
          && isLastDayOfFebruary(depreciationYear, depreciationDay)) {
        depreciationDay = 30;
      }

      if (acquisitionMonth == Month.FEBRUARY
          && isLastDayOfFebruary(acquisitionYear, acquisitionDay)) {
        acquisitionDay = 30;
      }

      if (acquisitionDay >= 30 && depreciationDay > 30) {
        depreciationDay = 30;
      }

      if (acquisitionDay > 30) {
        acquisitionDay = 30;
      }

    } else { // European way

      if (acquisitionDay == 31) {
        acquisitionDay = 30;
      }

      if (depreciationDay == 31) {
        depreciationDay = 30;
      }
    }

    BigDecimal nbDaysBetweenAcqAndFirstDepDate =
        BigDecimal.valueOf(
                360 * (depreciationYear - acquisitionYear)
                    + 30 * (depreciationMonth.getValue() - acquisitionMonth.getValue())
                    + (depreciationDay - acquisitionDay))
            .setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
    BigDecimal nbDaysOfPeriod =
        BigDecimal.valueOf(getPeriodicityInMonthProrataTemporis(fixedAsset) * 30)
            .setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
    prorataTemporis =
        nbDaysBetweenAcqAndFirstDepDate.divide(
            nbDaysOfPeriod, CALCULATION_SCALE, RoundingMode.HALF_UP);
    return prorataTemporis;
  }

  /**
   * Method only use in method computeProrataBetween
   *
   * @return
   */
  protected Integer getPeriodicityInMonthProrataTemporis(FixedAsset fixedAsset) {
    return getPeriodicityInMonth(fixedAsset);
  }

  protected boolean isLastDayOfFebruary(int year, int day) {
    Calendar calendar = Calendar.getInstance();
    calendar.set(year, Calendar.FEBRUARY, 1);
    int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    return maxDays == day;
  }

  protected FixedAssetLine createFixedAssetLine(
      FixedAsset fixedAsset,
      LocalDate depreciationDate,
      BigDecimal depreciation,
      BigDecimal cumulativeDepreciation,
      BigDecimal accountingValue,
      BigDecimal depreciationBase,
      int typeSelect,
      int statusSelect) {
    FixedAssetLine fixedAssetLine = new FixedAssetLine();
    fixedAssetLine.setDepreciationDate(depreciationDate);
    fixedAssetLine.setDepreciation(depreciation);
    fixedAssetLine.setCumulativeDepreciation(cumulativeDepreciation);
    fixedAssetLine.setAccountingValue(accountingValue);
    fixedAssetLine.setDepreciationBase(depreciationBase);
    fixedAssetLine.setTypeSelect(typeSelect);
    fixedAssetLine.setStatusSelect(statusSelect);
    return fixedAssetLine;
  }

  protected BigDecimal computeInitialDegressiveDepreciation(
      FixedAsset fixedAsset, BigDecimal baseValue) {
    BigDecimal ddRate = getDegressiveCoef(fixedAsset);
    return computeInitialDepreciationNumerator(baseValue, fixedAsset)
        .multiply(ddRate)
        .setScale(RETURNED_SCALE, RoundingMode.HALF_UP);
  }

  protected BigDecimal computeInitialDepreciationNumerator(
      BigDecimal baseValue, FixedAsset fixedAsset) {
    BigDecimal prorataTemporis = this.computeProrataTemporis(fixedAsset);
    return computeDepreciationNumerator(baseValue, getNumberOfDepreciation(fixedAsset))
        .multiply(prorataTemporis);
  }

  protected BigDecimal computeLinearDepreciation(FixedAsset fixedAsset, BigDecimal baseValue) {
    return computeDepreciationNumerator(baseValue, getNumberOfDepreciation(fixedAsset))
        .setScale(RETURNED_SCALE, RoundingMode.HALF_UP);
  }

  protected BigDecimal computeOnGoingDegressiveDepreciation(
      FixedAsset fixedAsset, FixedAssetLine previousFixedAssetLine) {
    BigDecimal previousAccountingValue = getAccountingValue(previousFixedAssetLine);
    BigDecimal degressiveDepreciation =
        computeDegressiveDepreciation(previousAccountingValue, fixedAsset);

    BigDecimal remainingNumberOfDepreciation =
        getNumberOfDepreciation(fixedAsset).subtract(numberOfDepreciationDone(fixedAsset));
    BigDecimal linearDepreciation =
        previousAccountingValue.divide(
            remainingNumberOfDepreciation, RETURNED_SCALE, RoundingMode.HALF_UP);
    return degressiveDepreciation.max(linearDepreciation);
  }

  protected BigDecimal numberOfDepreciationDone(FixedAsset fixedAsset) {
    List<FixedAssetLine> fixedAssetLineList = getFixedAssetLineList(fixedAsset);
    if (fixedAssetFailOverControlService.isFailOver(fixedAsset)) {
      if (fixedAssetLineList == null) {
        return getNumberOfPastDepreciation(fixedAsset);
      }
      return BigDecimal.valueOf(fixedAssetLineList.size())
          .add(getNumberOfPastDepreciation(fixedAsset));
    }
    if (fixedAssetLineList == null) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(fixedAssetLineList.size());
  }

  protected BigDecimal computeDegressiveDepreciation(BigDecimal baseValue, FixedAsset fixedAsset) {
    BigDecimal ddRate = getDegressiveCoef(fixedAsset);
    return computeDepreciationNumerator(baseValue, getNumberOfDepreciation(fixedAsset))
        .multiply(ddRate)
        .setScale(RETURNED_SCALE, RoundingMode.HALF_UP);
  }

  protected long countNotCorrectedPlannedLines(List<FixedAssetLine> fixedAssetLineList) {
    return fixedAssetLineList.stream()
        .filter(
            line ->
                line.getCorrectedAccountingValue().signum() == 0
                    && line.getStatusSelect() == FixedAssetLineRepository.STATUS_PLANNED)
        .count();
  }

  protected LocalDate computeDepreciationDate(
      FixedAsset fixedAsset, FixedAssetLine previousFixedAssetLine) {
    LocalDate depreciationDate;
    // In prorataTemporis, the system will generate one additional line.
    // This check if we are generating the additional line, and in this case, the depreciation date
    // is different.
    if (isProrataTemporis(fixedAsset)
        && numberOfDepreciationDone(fixedAsset)
            .equals(getNumberOfDepreciation(fixedAsset).setScale(0, RoundingMode.DOWN))) {
      depreciationDate = computeLastProrataDepreciationDate(fixedAsset);
    } else {
      depreciationDate =
          DateTool.plusMonths(
              previousFixedAssetLine.getDepreciationDate(), getPeriodicityInMonth(fixedAsset));
    }

    return depreciationDate;
  }

  protected BigDecimal computeDepreciation(
      FixedAsset fixedAsset, FixedAssetLine previousFixedAssetLine, BigDecimal baseValue) {
    BigDecimal depreciation;
    BigDecimal previousAccountingValue = getAccountingValue(previousFixedAssetLine);
    // If we are at the last line, we depreciate the remaining amount
    if (!isProrataTemporis(fixedAsset)
        && getNumberOfDepreciation(fixedAsset)
            .equals(numberOfDepreciationDone(fixedAsset).add(BigDecimal.ONE))) {
      return previousAccountingValue;
    }
    if (getComputationMethodSelect(fixedAsset)
        .equals(FixedAssetRepository.COMPUTATION_METHOD_DEGRESSIVE)) {
      depreciation = computeOnGoingDegressiveDepreciation(fixedAsset, previousFixedAssetLine);

    } else {
      depreciation = computeLinearDepreciation(fixedAsset, baseValue);
    }
    if (BigDecimal.ZERO.compareTo(previousAccountingValue.subtract(depreciation)) > 0) {
      depreciation = previousAccountingValue;
    }
    return depreciation;
  }

  protected BigDecimal computeDepreciationBase(
      FixedAsset fixedAsset, FixedAssetLine previousFixedAssetLine) {

    if (getComputationMethodSelect(fixedAsset)
        .equals(FixedAssetRepository.COMPUTATION_METHOD_DEGRESSIVE)) {
      return getAccountingValue(previousFixedAssetLine);
    }
    return previousFixedAssetLine.getDepreciationBase();
  }

  protected LocalDate computeLastProrataDepreciationDate(FixedAsset fixedAsset) {

    LocalDate d =
        DateTool.plusMonths(fixedAsset.getFirstServiceDate(), getDurationInMonth(fixedAsset));
    if (FixedAssetRepository.COMPUTATION_METHOD_DEGRESSIVE.equals(
        getComputationMethodSelect(fixedAsset))) {
      d = DateTool.minusMonths(d, getPeriodicityInMonth(fixedAsset));
    }
    return d;
  }

  protected BigDecimal getAccountingValue(FixedAssetLine fixedAssetLine) {
    if (fixedAssetLine == null) {
      return BigDecimal.ZERO;
    }
    return fixedAssetLine.getCorrectedAccountingValue().signum() != 0
        ? fixedAssetLine.getCorrectedAccountingValue()
        : fixedAssetLine.getAccountingValue();
  }
}

/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.apps.base.service;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Period;
import com.axelor.apps.base.db.Year;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.base.db.repo.YearRepository;
import com.axelor.apps.base.exceptions.BaseExceptionMessage;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class YearServiceImpl implements YearService {

  protected YearRepository yearRepository;

  @Inject
  public YearServiceImpl(YearRepository yearRepository) {
    this.yearRepository = yearRepository;
  }

  @Override
  public Year createYear(
      Company company,
      String name,
      String code,
      LocalDate fromDate,
      LocalDate toDate,
      Integer periodDuration,
      int typeSelect) {
    Year year = new Year();
    year.setCompany(company);
    year.setName(name);
    year.setCode(code);
    year.setFromDate(fromDate);
    year.setToDate(toDate);
    year.setPeriodDurationSelect(periodDuration);
    year.setTypeSelect(typeSelect);
    return year;
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  public void generatePeriodsForYear(Year year) throws AxelorException {
    year.setPeriodList(generatePeriods(year));
    yearRepository.save(year);
  }

  @Override
  public List<Period> generatePeriods(Year year) throws AxelorException {

    Integer duration = year.getPeriodDurationSelect();
    LocalDate fromDate = year.getFromDate();
    LocalDate toDate = year.getToDate();
    LocalDate periodToDate = fromDate;
    Integer periodNumber = 1;
    return generatePeriods(year, periodToDate, toDate, periodNumber, fromDate, duration);
  }

  @Override
  public List<Period> generatePeriods(
      Year year,
      LocalDate periodToDate,
      LocalDate toDate,
      Integer periodNumber,
      LocalDate fromDate,
      Integer duration)
      throws AxelorException {
    List<Period> periods = new ArrayList<Period>();
    int c = 0;
    int loopLimit = 1000;
    while (periodToDate.isBefore(toDate)) {
      if (periodNumber != 1) fromDate = fromDate.plusMonths(duration);
      if (c >= loopLimit) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_INCONSISTENCY, I18n.get(BaseExceptionMessage.PERIOD_3));
      }
      c += 1;
      periodToDate = fromDate.plusMonths(duration).minusDays(1);
      if (periodToDate.isAfter(toDate)) periodToDate = toDate;
      if (fromDate.isAfter(toDate)) continue;
      periods.add(createPeriod(year, periodToDate, periodNumber, fromDate));
      periodNumber++;
    }
    return periods;
  }

  protected Period createPeriod(
      Year year, LocalDate periodToDate, Integer periodNumber, LocalDate fromDate) {
    Period period = new Period();
    period.setFromDate(fromDate);
    period.setToDate(periodToDate);
    period.setYear(year);
    period.setName(String.format("%02d", periodNumber) + "/" + year.getCode());
    period.setCode(
        (String.format("%02d", periodNumber)
                + "/"
                + year.getCode()
                + "_"
                + year.getCompany().getCode())
            .toUpperCase());
    period.setStatusSelect(year.getStatusSelect());
    return period;
  }

  @Override
  public Year getYear(LocalDate date, Company company, Integer type) {

    return yearRepository
        .all()
        .filter(
            "self.company = ?1 AND self.fromDate <= ?2 AND self.toDate >= ?2 AND self.typeSelect = ?3",
            company,
            date,
            type)
        .fetchOne();
  }
}

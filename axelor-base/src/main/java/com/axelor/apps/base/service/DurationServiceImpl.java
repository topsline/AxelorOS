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

import com.axelor.apps.base.db.Duration;
import com.axelor.apps.base.db.repo.DurationRepository;
import com.axelor.apps.base.service.app.AppBaseService;
import com.google.inject.Singleton;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Singleton
public class DurationServiceImpl implements DurationService {

  @Override
  public LocalDate computeDuration(Duration duration, LocalDate date) {
    if (duration == null) {
      return date;
    }
    switch (duration.getTypeSelect()) {
      case DurationRepository.TYPE_MONTH:
        return date.plusMonths(duration.getValue());
      case DurationRepository.TYPE_DAY:
        return date.plusDays(duration.getValue());
      default:
        return date;
    }
  }

  @Override
  public BigDecimal computeRatio(
      LocalDate start, LocalDate end, LocalDate totalStart, LocalDate totalEnd) {
    end = end.plus(1, ChronoUnit.DAYS);
    totalEnd = totalEnd.plus(1, ChronoUnit.DAYS);
    long totalDays = ChronoUnit.DAYS.between(totalStart, totalEnd);
    long totalComputedDays = ChronoUnit.DAYS.between(start, end);
    return BigDecimal.valueOf(totalComputedDays)
        .divide(
            BigDecimal.valueOf(totalDays),
            AppBaseService.COMPUTATION_SCALING,
            RoundingMode.HALF_UP);
  }
}

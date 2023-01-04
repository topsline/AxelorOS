/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2022 Axelor (<http://axelor.com>).
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
package com.axelor.apps.account.service.fixedasset;

import com.axelor.apps.account.db.FixedAsset;
import com.axelor.apps.account.db.FixedAssetLine;
import com.axelor.apps.account.db.repo.FixedAssetLineRepository;
import com.axelor.apps.base.service.PeriodService;
import com.axelor.apps.base.service.YearService;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RequestScoped
public class FixedAssetLineIfrsServiceImpl extends AbstractFixedAssetLineServiceImpl {

  @Inject
  public FixedAssetLineIfrsServiceImpl(
      FixedAssetLineRepository fixedAssetLineRepository,
      YearService yearService,
      PeriodService periodService) {
    super(fixedAssetLineRepository, yearService, periodService);
  }

  @Override
  protected int getPeriodicityTypeSelect(FixedAsset fixedAsset) {
    return fixedAsset.getIfrsPeriodicityTypeSelect();
  }

  @Override
  protected int getPeriodicityInMonth(FixedAsset fixedAsset) {
    return fixedAsset.getIfrsPeriodicityInMonth();
  }

  @Override
  protected List<FixedAssetLine> getFixedAssetLineList(FixedAsset fixedAsset) {
    return fixedAsset.getIfrsFixedAssetLineList();
  }

  @Override
  protected BigDecimal computeProrataBetween(
      FixedAsset fixedAsset,
      LocalDate previousRealizedDate,
      LocalDate disposalDate,
      LocalDate nextPlannedDate) {
    return Beans.get(FixedAssetLineIfrsComputationServiceImpl.class)
        .computeProrataBetween(fixedAsset, previousRealizedDate, disposalDate, nextPlannedDate);
  }

  @Override
  protected int getTypeSelect() {
    return FixedAssetLineRepository.TYPE_SELECT_IFRS;
  }
}

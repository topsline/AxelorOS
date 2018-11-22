/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
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
package com.axelor.apps.account.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.axelor.apps.account.db.FixedAsset;
import com.axelor.apps.account.db.Invoice;
import com.axelor.exception.AxelorException;

public interface FixedAssetService {

  /**
   * Allow to generate and compute the fixed asset lines
   *
   * @param fixedAsset
   * @return
   */
  public FixedAsset generateAndcomputeLines(FixedAsset fixedAsset);

  /**
   * Allow to create fixed asset from invoice
   *
   * @param invoice
   * @return
   * @throws AxelorException
   */
  public void createFixedAsset(Invoice invoice) throws AxelorException;

  /**
   * Allow to disposal remaining depreciation
   *
   * @param disposalDate
   * @param disposalAmount
   * @param fixedAsset
   * @throws AxelorException
   */
  public void disposal(LocalDate disposalDate, BigDecimal disposalAmount, FixedAsset fixedAsset)
      throws AxelorException;
}

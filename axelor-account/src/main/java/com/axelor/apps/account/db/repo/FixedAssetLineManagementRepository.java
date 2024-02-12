/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.apps.account.db.repo;

import com.axelor.apps.account.db.FixedAssetLine;
import com.axelor.apps.account.service.fixedasset.FixedAssetLineToolService;
import com.axelor.apps.base.AxelorException;
import com.axelor.inject.Beans;
import java.util.Map;

public class FixedAssetLineManagementRepository extends FixedAssetLineRepository {

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    if (json != null && json.get("id") != null) {
      FixedAssetLine fixedAssetLine = this.find((Long) json.get("id"));
      try {
        json.put(
            "$currencyNumberOfDecimals",
            Beans.get(FixedAssetLineToolService.class).getCompanyScale(fixedAssetLine));
      } catch (AxelorException e) {
        throw new RuntimeException(e);
      }
    }

    return super.populate(json, context);
  }
}

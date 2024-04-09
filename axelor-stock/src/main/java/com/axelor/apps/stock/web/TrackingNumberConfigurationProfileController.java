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
package com.axelor.apps.stock.web;

import com.axelor.apps.stock.db.TrackingNumberConfigurationProfile;
import com.axelor.apps.stock.service.TrackingNumberConfigurationProfileService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Singleton;

@Singleton
public class TrackingNumberConfigurationProfileController {

  public void setDefaultsFieldFormula(ActionRequest request, ActionResponse response) {

    TrackingNumberConfigurationProfile profile =
        request.getContext().asType(TrackingNumberConfigurationProfile.class);

    Beans.get(TrackingNumberConfigurationProfileService.class).setDefaultsFieldFormula(profile);
    response.setValue("profileFieldFormulaSet", profile.getProfileFieldFormulaSet());
  }
}

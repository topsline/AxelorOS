/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2020 Axelor (<http://axelor.com>).
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
package com.axelor.apps.cash.management.web;

import com.axelor.apps.cash.management.db.ForecastGenerator;
import com.axelor.apps.cash.management.db.repo.ForecastGeneratorRepository;
import com.axelor.apps.cash.management.service.ForecastService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class ForecastController {

  public void generate(ActionRequest request, ActionResponse response) {
    ForecastGenerator forecastGenerator = request.getContext().asType(ForecastGenerator.class);
    forecastGenerator =
        Beans.get(ForecastGeneratorRepository.class).find(forecastGenerator.getId());
    Beans.get(ForecastService.class).generate(forecastGenerator);
    response.setReload(true);
  }
}

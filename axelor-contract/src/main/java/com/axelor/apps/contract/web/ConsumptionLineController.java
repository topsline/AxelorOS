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
package com.axelor.apps.contract.web;

import com.axelor.apps.contract.db.ConsumptionLine;
import com.axelor.apps.contract.service.ConsumptionLineService;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class ConsumptionLineController {

  public void changeProduct(ActionRequest request, ActionResponse response) {
    ConsumptionLine line = request.getContext().asType(ConsumptionLine.class);
    try {
      Beans.get(ConsumptionLineService.class).fill(line, line.getProduct());
      response.setValues(line);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }
}

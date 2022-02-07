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
package com.axelor.apps.hr.web.timesheet;

import com.axelor.apps.hr.db.Timesheet;
import com.axelor.apps.hr.db.TimesheetLine;
import com.axelor.apps.hr.db.repo.TimesheetLineRepository;
import com.axelor.apps.hr.service.timesheet.TimesheetLineService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.HandleExceptionResponse;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;

public class TimesheetLineController {

  private static final String HOURS_DURATION_FIELD = "hoursDuration";

  private static final String DURATION_FIELD = "duration";

  /**
   * Called from timesheet line editable grid or form. Get the timesheet corresponding to
   * timesheetline and call {@link TimesheetLineService#computeHoursDuration(Timesheet, BigDecimal,
   * boolean)}
   *
   * @param request
   * @param response
   * @throws AxelorException
   */
  @HandleExceptionResponse
  public void setStoredDuration(ActionRequest request, ActionResponse response)
      throws AxelorException {

    TimesheetLine timesheetLine = request.getContext().asType(TimesheetLine.class);
    Timesheet timesheet;
    Context parent = request.getContext().getParent();
    if (parent != null && parent.getContextClass().equals(Timesheet.class)) {
      timesheet = parent.asType(Timesheet.class);
    } else {
      timesheet = timesheetLine.getTimesheet();
    }
    BigDecimal hoursDuration =
        Beans.get(TimesheetLineService.class)
            .computeHoursDuration(timesheet, timesheetLine.getDuration(), true);

    response.setValue(HOURS_DURATION_FIELD, hoursDuration);
  }

  /**
   * Called from timesheet editor Get the timesheet corresponding to timesheetline and call {@link
   * TimesheetLineService#computeHoursDuration(Timesheet, BigDecimal, boolean)}
   *
   * @param request
   * @param response
   * @throws AxelorException
   */
  @HandleExceptionResponse
  public void setDuration(ActionRequest request, ActionResponse response) throws AxelorException {

    TimesheetLine timesheetLine = request.getContext().asType(TimesheetLine.class);
    Timesheet timesheet;
    Context parent = request.getContext().getParent();
    if (parent != null && parent.getContextClass().equals(Timesheet.class)) {
      timesheet = parent.asType(Timesheet.class);
    } else {
      timesheet = timesheetLine.getTimesheet();
    }
    BigDecimal duration =
        Beans.get(TimesheetLineService.class)
            .computeHoursDuration(timesheet, timesheetLine.getHoursDuration(), false);

    response.setValue(DURATION_FIELD, duration);
  }

  /**
   * Invert value of 'toInvoice' field and save the record
   *
   * @param request
   * @param response
   */
  @Transactional
  @HandleExceptionResponse
  public void updateToInvoice(ActionRequest request, ActionResponse response) {

    TimesheetLine timesheetLine = request.getContext().asType(TimesheetLine.class);
    timesheetLine = Beans.get(TimesheetLineRepository.class).find(timesheetLine.getId());
    timesheetLine.setToInvoice(!timesheetLine.getToInvoice());
    Beans.get(TimesheetLineRepository.class).save(timesheetLine);
    response.setValue("toInvoice", timesheetLine.getToInvoice());
  }
}

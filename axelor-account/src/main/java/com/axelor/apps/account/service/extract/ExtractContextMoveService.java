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
package com.axelor.apps.account.service.extract;

import com.axelor.apps.account.db.Move;
import com.axelor.exception.AxelorException;
import com.axelor.rpc.Context;
import java.time.LocalDate;
import java.util.Map;

public interface ExtractContextMoveService {

  public Map<String, Object> getMapFromMoveWizardGenerateReverseForm(Context context)
      throws AxelorException;

  Map<String, Object> getMapFromMoveWizardMassReverseForm(Context context);

  LocalDate getDateOfReversion(Context context, Move move, int dateOfReversionSelect)
      throws AxelorException;
}

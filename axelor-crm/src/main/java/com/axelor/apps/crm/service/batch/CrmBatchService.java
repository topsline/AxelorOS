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
package com.axelor.apps.crm.service.batch;

import com.axelor.apps.base.db.Batch;
import com.axelor.apps.base.exceptions.BaseExceptionMessage;
import com.axelor.apps.base.service.administration.AbstractBatchService;
import com.axelor.apps.crm.db.CrmBatch;
import com.axelor.apps.crm.db.repo.CrmBatchRepository;
import com.axelor.db.Model;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.common.base.Strings;

/**
 * InvoiceBatchService est une classe implémentant l'ensemble des batchs de comptabilité et
 * assimilé.
 *
 * @author Geoffrey DUBAUX
 * @version 0.1
 */
public class CrmBatchService extends AbstractBatchService {

  @Override
  protected Class<? extends Model> getModelClass() {
    return CrmBatch.class;
  }

  @Override
  public Batch run(Model batchModel) throws AxelorException {

    Batch batch;
    CrmBatch crmBatch = (CrmBatch) batchModel;

    switch (crmBatch.getActionSelect()) {
      case CrmBatchRepository.ACTION_BATCH_EVENT_REMINDER:
        batch = eventReminder(crmBatch);
        break;

      case CrmBatchRepository.ACTION_BATCH_TARGET:
        batch = target(crmBatch);
        break;

      case CrmBatchRepository.ACTION_CALL_FOR_TENDERS_REMINDER:
        batch = callForTendersReminder(crmBatch);
        break;

      default:
        throw new AxelorException(
            TraceBackRepository.CATEGORY_INCONSISTENCY,
            I18n.get(BaseExceptionMessage.BASE_BATCH_1),
            crmBatch.getActionSelect(),
            crmBatch.getCode());
    }

    return batch;
  }

  public Batch eventReminder(CrmBatch crmBatch) {

    return Beans.get(BatchEventReminder.class).run(crmBatch);
  }

  public Batch target(CrmBatch crmBatch) {

    return Beans.get(BatchTarget.class).run(crmBatch);
  }

  public Batch callForTendersReminder(CrmBatch crmBatch) throws AxelorException {
    if (crmBatch.getMonthLimitNbr() <= 0) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(com.axelor.apps.crm.exception.IExceptionMessage.CRM_BATCH_MONTH_NOT_POSITIVE));
    }

    if (!crmBatch.getTemplate().getContent().contains("_callForTendersList")) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(com.axelor.apps.crm.exception.IExceptionMessage.CRM_BATCH_IMPROPER_TEMPLATE));
    }

    if (Strings.isNullOrEmpty(crmBatch.getSenderEmail())) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          I18n.get(
              com.axelor.apps.crm.exception.IExceptionMessage
                  .CRM_BATCH_SENDER_EMAIL_NOT_CONFIGURED));
    }
    return Beans.get(BatchCallForTendersReminder.class).run(crmBatch);
  }
}

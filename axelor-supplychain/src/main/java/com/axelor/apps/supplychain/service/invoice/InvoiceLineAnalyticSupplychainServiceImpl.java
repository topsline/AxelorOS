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
package com.axelor.apps.supplychain.service.invoice;

import com.axelor.apps.account.db.AnalyticMoveLine;
import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.repo.AccountAnalyticRulesRepository;
import com.axelor.apps.account.db.repo.AnalyticAccountRepository;
import com.axelor.apps.account.db.repo.AnalyticMoveLineRepository;
import com.axelor.apps.account.service.analytic.AnalyticMoveLineService;
import com.axelor.apps.account.service.analytic.AnalyticToolService;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.account.service.invoice.InvoiceLineAnalyticServiceImpl;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.supplychain.model.AnalyticLineModel;
import com.axelor.apps.supplychain.service.AnalyticLineModelService;
import com.axelor.utils.service.ListToolService;
import com.google.inject.Inject;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

public class InvoiceLineAnalyticSupplychainServiceImpl extends InvoiceLineAnalyticServiceImpl
    implements InvoiceLineAnalyticSupplychainService {
  protected AnalyticLineModelService analyticLineModelService;
  protected AnalyticMoveLineRepository analyticMoveLineRepo;

  @Inject
  public InvoiceLineAnalyticSupplychainServiceImpl(
      AnalyticAccountRepository analyticAccountRepository,
      AccountAnalyticRulesRepository accountAnalyticRulesRepository,
      AnalyticMoveLineService analyticMoveLineService,
      AnalyticToolService analyticToolService,
      AccountConfigService accountConfigService,
      ListToolService listToolService,
      AppAccountService appAccountService,
      AnalyticLineModelService analyticLineModelService,
      AnalyticMoveLineRepository analyticMoveLineRepo) {
    super(
        analyticAccountRepository,
        accountAnalyticRulesRepository,
        analyticMoveLineService,
        analyticToolService,
        accountConfigService,
        listToolService,
        appAccountService);

    this.analyticLineModelService = analyticLineModelService;
    this.analyticMoveLineRepo = analyticMoveLineRepo;
  }

  @Override
  public List<AnalyticMoveLine> createAnalyticDistributionWithTemplate(InvoiceLine invoiceLine) {
    List<AnalyticMoveLine> analyticMoveLineList =
        super.createAnalyticDistributionWithTemplate(invoiceLine);
    for (AnalyticMoveLine analyticMoveLine : analyticMoveLineList) {
      analyticMoveLine.setSaleOrderLine(invoiceLine.getSaleOrderLine());
      analyticMoveLine.setPurchaseOrderLine(invoiceLine.getPurchaseOrderLine());
    }
    return analyticMoveLineList;
  }

  @Override
  public void setInvoiceLineAnalyticInfo(
      InvoiceLine invoiceLine, Invoice invoice, AnalyticLineModel analyticLineModel)
      throws AxelorException {
    if (analyticLineModel.getAnalyticDistributionTemplate() != null
        || CollectionUtils.isNotEmpty(analyticLineModel.getAnalyticMoveLineList())) {
      analyticLineModelService.setInvoiceLineAnalyticInfo(analyticLineModel, invoiceLine);

      this.copyAnalyticMoveLines(analyticLineModel.getAnalyticMoveLineList(), invoiceLine);
      this.computeAnalyticDistribution(invoiceLine);
    } else {
      List<AnalyticMoveLine> analyticMoveLineList =
          this.getAndComputeAnalyticDistribution(invoiceLine, invoice);

      analyticMoveLineList.forEach(invoiceLine::addAnalyticMoveLineListItem);
    }
  }

  protected void copyAnalyticMoveLines(
      List<AnalyticMoveLine> originalAnalyticMoveLineList, InvoiceLine invoiceLine) {
    if (originalAnalyticMoveLineList == null) {
      return;
    }

    for (AnalyticMoveLine originalAnalyticMoveLine : originalAnalyticMoveLineList) {
      AnalyticMoveLine analyticMoveLine =
          analyticMoveLineRepo.copy(originalAnalyticMoveLine, false);

      analyticMoveLine.setTypeSelect(AnalyticMoveLineRepository.STATUS_FORECAST_INVOICE);

      invoiceLine.addAnalyticMoveLineListItem(analyticMoveLine);
    }
  }
}

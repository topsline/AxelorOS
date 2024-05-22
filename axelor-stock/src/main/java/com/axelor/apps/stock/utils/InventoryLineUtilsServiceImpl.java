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
package com.axelor.apps.stock.utils;

import com.axelor.apps.stock.db.InventoryLine;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.db.StockLocationLine;
import com.axelor.apps.stock.db.repo.StockLocationRepository;
import com.google.inject.Inject;
import java.util.Objects;

public class InventoryLineUtilsServiceImpl implements InventoryLineUtilsService {

  protected StockLocationRepository stockLocationRepository;

  @Inject
  public InventoryLineUtilsServiceImpl(StockLocationRepository stockLocationRepository) {
    this.stockLocationRepository = stockLocationRepository;
  }

  @Override
  public boolean isPresentInStockLocation(InventoryLine inventoryLine) {

    Objects.requireNonNull(inventoryLine);

    if (inventoryLine.getProduct() == null
        || inventoryLine.getStockLocation() == null
        || inventoryLine.getStockLocation().getStockLocationLineList() == null) {
      return false;
    }

    StockLocation stockLocation =
        stockLocationRepository.find(inventoryLine.getStockLocation().getId());
    return stockLocation.getStockLocationLineList().stream()
        .map(StockLocationLine::getProduct)
        .anyMatch(product -> inventoryLine.getProduct().equals(product));
  }
}

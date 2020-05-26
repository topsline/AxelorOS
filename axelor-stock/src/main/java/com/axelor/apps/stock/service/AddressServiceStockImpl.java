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
package com.axelor.apps.stock.service;

import com.axelor.apps.base.service.AddressServiceImpl;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.module.StockModule;
import com.axelor.db.JPA;
import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

@Alternative
@Priority(StockModule.PRIORITY)
@RequestScoped
public class AddressServiceStockImpl extends AddressServiceImpl {
  static {
    registerCheckUsedFunc(AddressServiceStockImpl::checkAddressUsedStock);
  }

  public static void init() {}

  private static boolean checkAddressUsedStock(Long addressId) {
    return JPA.all(StockMove.class)
                .filter("self.fromAddress.id = ?1 OR self.toAddress.id = ?1", addressId)
                .fetchOne()
            != null
        || JPA.all(StockLocation.class).filter("self.address.id = ?1", addressId).fetchOne()
            != null;
  }
}

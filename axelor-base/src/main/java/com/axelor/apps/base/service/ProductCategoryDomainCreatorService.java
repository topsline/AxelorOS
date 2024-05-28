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
package com.axelor.apps.base.service;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.ProductCategory;

public interface ProductCategoryDomainCreatorService {
  /**
   * Find child of given category, and recursively children of found children. Then create a domain
   * to filter the children from the parent category list.
   *
   * @param productCategory
   * @return product category domain
   * @throws AxelorException
   */
  String createProductCategoryDomainFilteringChildren(ProductCategory productCategory)
      throws AxelorException;
}

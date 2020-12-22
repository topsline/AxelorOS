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
package com.axelor.apps.businessproject.service.app;

import com.axelor.apps.base.db.AppBusinessProject;
import com.axelor.apps.base.db.repo.AppBusinessProjectRepository;
import com.axelor.apps.base.service.app.AppBaseServiceImpl;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AppBusinessProjectServiceImpl extends AppBaseServiceImpl
    implements AppBusinessProjectService {

  @Inject private AppBusinessProjectRepository appBusinessProjectRepo;

  @Override
  public AppBusinessProject getAppBusinessProject() {
    return appBusinessProjectRepo.all().fetchOne();
  }
}

/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2023 Axelor (<http://axelor.com>).
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
package com.axelor.apps.hr.service.app;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.repo.CompanyRepository;
import com.axelor.apps.base.service.app.AppBaseServiceImpl;
import com.axelor.apps.hr.db.HRConfig;
import com.axelor.apps.hr.db.repo.HRConfigRepository;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.axelor.studio.app.service.AppVersionService;
import com.axelor.studio.db.AppHumanResource;
import com.axelor.studio.db.AppLeave;
import com.axelor.studio.db.AppTimesheet;
import com.axelor.studio.db.repo.AppHumanResourceRepository;
import com.axelor.studio.db.repo.AppLeaveRepository;
import com.axelor.studio.db.repo.AppRepository;
import com.axelor.studio.db.repo.AppTimesheetRepository;
import com.axelor.studio.service.AppSettingsStudioService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import java.util.List;

@Singleton
public class AppHumanResourceServiceImpl extends AppBaseServiceImpl
    implements AppHumanResourceService {

  private AppTimesheetRepository appTimesheetRepo;
  private AppLeaveRepository appLeaveRepo;

  protected CompanyRepository companyRepo;

  protected HRConfigRepository hrConfigRepo;

  protected AppHumanResourceRepository appHumanResourceRepository;

  @Inject
  public AppHumanResourceServiceImpl(
      AppRepository appRepo,
      MetaFiles metaFiles,
      AppVersionService appVersionService,
      MetaModelRepository metaModelRepo,
      AppSettingsStudioService appSettingsStudioService,
      AppTimesheetRepository appTimesheetRepo,
      AppLeaveRepository appLeaveRepo,
      CompanyRepository companyRepo,
      HRConfigRepository hrConfigRepo,
      AppHumanResourceRepository appHumanResourceRepository) {
    super(appRepo, metaFiles, appVersionService, metaModelRepo, appSettingsStudioService);
    this.appTimesheetRepo = appTimesheetRepo;
    this.appLeaveRepo = appLeaveRepo;
    this.companyRepo = companyRepo;
    this.hrConfigRepo = hrConfigRepo;
    this.appHumanResourceRepository = appHumanResourceRepository;
  }

  @Override
  public AppHumanResource getAppHumanResource() {
    return appHumanResourceRepository.all().fetchOne();
  }

  @Override
  public AppTimesheet getAppTimesheet() {
    return appTimesheetRepo.all().fetchOne();
  }

  @Override
  public AppLeave getAppLeave() {
    return appLeaveRepo.all().fetchOne();
  }

  @Override
  @Transactional
  public void generateHrConfigurations() {

    List<Company> companies = companyRepo.all().filter("self.hrConfig is null").fetch();

    for (Company company : companies) {
      HRConfig hrConfig = new HRConfig();
      hrConfig.setCompany(company);
      hrConfigRepo.save(hrConfig);
    }
  }
}

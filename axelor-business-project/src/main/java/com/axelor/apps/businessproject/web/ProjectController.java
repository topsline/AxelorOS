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
package com.axelor.apps.businessproject.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.repo.InvoiceLineRepository;
import com.axelor.apps.businessproject.report.IReport;
import com.axelor.apps.businessproject.service.ProjectBusinessService;
import com.axelor.apps.hr.service.employee.EmployeeService;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.db.repo.ProjectRepository;
import com.axelor.apps.project.service.ProjectService;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Map;

public class ProjectController {

	private final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

    @Inject
    private ProjectService projectService;

    @Inject
    private ProjectBusinessService projectBusinessService;
	
	public void printProject(ActionRequest request,ActionResponse response) throws AxelorException  {
		Project project = request.getContext().asType(Project.class);

		String name = I18n.get("Project") + " " + project.getCode();
		
		String fileLink = ReportFactory.createReport(IReport.PROJECT, name+"-${date}")
				.addParam("ProjectId", project.getId())
				.addParam("Locale", ReportSettings.getPrintingLocale(null))
				.toAttach(project)
				.generate()
				.getFileLink();

		logger.debug("Printing "+name);
	
		response.setView(ActionView
				.define(name)
				.add("html", fileLink).map());	
	}

	public void computeProgress(ActionRequest request,ActionResponse response){

		Project project = request.getContext().asType(Project.class);
		
		BigDecimal duration = BigDecimal.ZERO;
		if(BigDecimal.ZERO.compareTo(project.getDuration()) != 0){
			duration = project.getTimeSpent().add(project.getLeadDelay()).divide(project.getDuration(), 2, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal(100));
		}
		
		if(duration.compareTo(BigDecimal.ZERO) == -1 || duration.compareTo(new BigDecimal(100)) == 1){
			duration = BigDecimal.ZERO;
		}

		response.setValue("progress", duration);

	}

	public void computeDurationFromChildren(ActionRequest request, ActionResponse response)  {
		Project project = request.getContext().asType(Project.class);

		BigDecimal duration = projectService.computeDurationFromChildren(project.getId());

		BigDecimal visibleDuration = Beans.get(EmployeeService.class).getUserDuration(duration, AuthUtils.getUser(), false);

		response.setValue("duration", duration);
		response.setValue("$visibleDuration", visibleDuration);

	}
	
	
	public void printPlannifAndCost(ActionRequest request,ActionResponse response) throws AxelorException  {

		Project project = request.getContext().asType(Project.class);

		String name = I18n.get("Planification and costs");
		
		if (project.getCode() != null) {
			name += " (" + project.getCode() + ")";
		}
		
		String fileLink = ReportFactory.createReport(IReport.PLANNIF_AND_COST, name)
				.addParam("ProjectId", project.getId())
				.addParam("Locale", ReportSettings.getPrintingLocale(null))
				.toAttach(project)
				.generate()
				.getFileLink();

	
		response.setView(ActionView
				.define(name)
				.add("html", fileLink).map());	
	}

    public void managePurchaseInvoiceLine(ActionRequest request, ActionResponse response) {
	    Project project = Beans.get(ProjectRepository.class).find(request.getContext().asType(Project.class).getId());
	    InvoiceLine purchaseInvoiceLine = Beans.get(InvoiceLineRepository.class).find(Long.valueOf(((Integer) ((Map) request.getContext().get("purchaseInvoiceLine")).get("id"))));

        projectBusinessService.manageInvoiceLine(purchaseInvoiceLine, project);

	    response.setReload(true);
    }

    public void manageSaleInvoiceLine(ActionRequest request, ActionResponse response) {
        Project project = Beans.get(ProjectRepository.class).find(request.getContext().asType(Project.class).getId());
        InvoiceLine saleInvoiceLine = Beans.get(InvoiceLineRepository.class).find(Long.valueOf(((Integer) ((Map) request.getContext().get("saleInvoiceLine")).get("id"))));

        projectBusinessService.manageInvoiceLine(saleInvoiceLine, project);

        response.setReload(true);
    }

    public void countToInvoiceNotInvoiced(ActionRequest request, ActionResponse response) {
        Project project = Beans.get(ProjectRepository.class).find(request.getContext().asType(Project.class).getId());
        response.setValue("$toInvoiceNotInvoiced", projectBusinessService.countToInvoiceNotInvoiced(project));
    }
}

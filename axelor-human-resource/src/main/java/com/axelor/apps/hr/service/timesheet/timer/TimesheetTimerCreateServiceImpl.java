package com.axelor.apps.hr.service.timesheet.timer;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.TSTimer;
import com.axelor.apps.hr.db.repo.TSTimerRepository;
import com.axelor.apps.hr.exception.HumanResourceExceptionMessage;
import com.axelor.apps.hr.service.app.AppHumanResourceService;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.db.ProjectTask;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.axelor.studio.db.AppTimesheet;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class TimesheetTimerCreateServiceImpl implements TimesheetTimerCreateService {
  protected TSTimerRepository tsTimerRepository;
  protected AppHumanResourceService appHumanResourceService;
  protected TimesheetTimerService timesheetTimerService;

  @Inject
  public TimesheetTimerCreateServiceImpl(
      TSTimerRepository tsTimerRepository,
      AppHumanResourceService appHumanResourceService,
      TimesheetTimerService timesheetTimerService) {
    this.tsTimerRepository = tsTimerRepository;
    this.appHumanResourceService = appHumanResourceService;
    this.timesheetTimerService = timesheetTimerService;
  }

  @Override
  public TSTimer createOrUpdateTimer(
      Employee employee,
      Project project,
      ProjectTask projectTask,
      Product product,
      Long duration,
      String comment)
      throws AxelorException {
    checkFields(employee, project, projectTask, product);
    AppTimesheet appTimesheet = appHumanResourceService.getAppTimesheet();
    boolean isMultipleTimer = appTimesheet.getIsMultipleTimerEnabled();
    TSTimer timer;

    if (isMultipleTimer) {
      return createTSTimer(employee, project, projectTask, product, duration, comment);
    } else {
      timer = timesheetTimerService.getCurrentTSTimer();
      if (timer == null) {
        return createTSTimer(employee, project, projectTask, product, duration, comment);
      }
      if (timer.getStatusSelect() == TSTimerRepository.STATUS_START) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_INCONSISTENCY,
            "A timer is already started, stop it before starting a new one.");
      }
      resetTimer(timer);
      updateTimer(timer, employee, project, projectTask, product, duration, comment);
    }

    return timer;
  }

  @Override
  public TSTimer createOrUpdateTimer(
      Project project, ProjectTask projectTask, Product product, Long duration, String comment)
      throws AxelorException {
    Employee employee = null;
    User user = AuthUtils.getUser();
    if (user != null) {
      employee = user.getEmployee();
    }
    if (employee == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.TIMESHEET_TIMER_USER_NO_EMPLOYEE));
    }
    return createOrUpdateTimer(employee, project, projectTask, product, duration, comment);
  }

  @Transactional
  public void resetTimer(TSTimer timer) {
    timer.setStatusSelect(TSTimerRepository.STATUS_DRAFT);
    timer.setTimesheetLine(null);
    timer.setStartDateTime(null);
    timer.setDuration(0L);
    timer.setComments(null);
    timer.setLastStartDateT(null);
    timer.setUpdatedDuration(null);
  }

  @Transactional(rollbackOn = {Exception.class})
  @Override
  public TSTimer createTSTimer(
      Employee employee,
      Project project,
      ProjectTask projectTask,
      Product product,
      Long duration,
      String comment)
      throws AxelorException {
    checkFields(employee, project, projectTask, product);
    TSTimer timer = new TSTimer();
    timer.setStatusSelect(TSTimerRepository.STATUS_DRAFT);
    updateTimer(timer, employee, project, projectTask, product, duration, comment);
    return tsTimerRepository.save(timer);
  }

  @Transactional
  @Override
  public TSTimer updateTimer(
      TSTimer timer,
      Employee employee,
      Project project,
      ProjectTask projectTask,
      Product product,
      Long duration,
      String comment)
      throws AxelorException {
    AppTimesheet appTimesheet = appHumanResourceService.getAppTimesheet();

    if (timer.getStatusSelect() != TSTimerRepository.STATUS_DRAFT
        && (employee != null || project != null || projectTask != null || product != null)) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.TIMESHEET_TIMER_UPDATE_STATUS_ISSUE));
    }

    if (duration != null && !appTimesheet.getEditModeTSTimer()) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.TIMESHEET_TIMER_TIMER_STOP_CONFIG_DISABLED));
    }

    checkRelation(project, projectTask, product);

    if (employee != null) {
      timer.setEmployee(employee);
    }
    if (project != null) {
      timer.setProject(project);
    }
    if (projectTask != null) {
      timer.setProjectTask(projectTask);
    }
    if (product != null) {
      timer.setProduct(product);
    }
    if (duration != null) {
      timer.setUpdatedDuration(duration);
    }
    if (StringUtils.notEmpty(comment)) {
      timer.setComments(comment);
    }
    return timer;
  }

  protected void checkFields(
      Employee employee, Project project, ProjectTask projectTask, Product product)
      throws AxelorException {
    if (employee == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.TIMESHEET_TIMER_EMPTY_EMPLOYEE));
    }

    if (product == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.TIMESHEET_TIMER_EMPTY_ACTIVITY));
    }

    checkRelation(project, projectTask, product);
  }

  protected void checkRelation(Project project, ProjectTask projectTask, Product product)
      throws AxelorException {
    if ((project == null && projectTask != null) || (project != null && projectTask == null)) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.TIMESHEET_TIMER_EMPTY_PROJECT_OR_TASK));
    }

    if (project != null && projectTask != null && !project.equals(projectTask.getProject())) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.TIMESHEET_TIMER_PROJECT_TASK_INCONSISTENCY));
    }

    if (projectTask != null && !product.equals(projectTask.getProduct())) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_INCONSISTENCY,
          I18n.get(HumanResourceExceptionMessage.TIMESHEET_TIMER_ACTIVITY_INCONSISTENCY));
    }
  }
}
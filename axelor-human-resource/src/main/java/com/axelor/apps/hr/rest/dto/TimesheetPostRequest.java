package com.axelor.apps.hr.rest.dto;

import com.axelor.apps.hr.db.TSTimer;
import com.axelor.apps.hr.db.TimesheetLine;
import com.axelor.utils.api.ObjectFinder;
import com.axelor.utils.api.RequestPostStructure;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections.CollectionUtils;

public class TimesheetPostRequest extends RequestPostStructure {
  @NotNull private LocalDate fromDate;

  private LocalDate toDate;

  private List<Long> timesheetLineIdList;

  private List<Long> timerIdList;

  public LocalDate getFromDate() {
    return fromDate;
  }

  public void setFromDate(LocalDate fromDate) {
    this.fromDate = fromDate;
  }

  public LocalDate getToDate() {
    return toDate;
  }

  public void setToDate(LocalDate toDate) {
    this.toDate = toDate;
  }

  public List<Long> getTimesheetLineIdList() {
    return timesheetLineIdList;
  }

  public void setTimesheetLineIdList(List<Long> timesheetLineIdList) {
    this.timesheetLineIdList = timesheetLineIdList;
  }

  public List<Long> getTimerIdList() {
    return timerIdList;
  }

  public void setTimerIdList(List<Long> timerIdList) {
    this.timerIdList = timerIdList;
  }

  public List<TimesheetLine> fetchTimesheetLines() {
    if (CollectionUtils.isEmpty(timesheetLineIdList)) {
      return Collections.emptyList();
    }

    List<TimesheetLine> timesheetLineList = new ArrayList<>();
    for (Long id : timesheetLineIdList) {
      timesheetLineList.add(ObjectFinder.find(TimesheetLine.class, id, ObjectFinder.NO_VERSION));
    }
    return timesheetLineList;
  }

  public List<TSTimer> fetchTSTimers() {
    if (CollectionUtils.isEmpty(timerIdList)) {
      return Collections.emptyList();
    }

    List<TSTimer> timerList = new ArrayList<>();
    for (Long id : timerIdList) {
      timerList.add(ObjectFinder.find(TSTimer.class, id, ObjectFinder.NO_VERSION));
    }
    return timerList;
  }
}

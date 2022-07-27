package com.axelor.apps.account.db.repo.listener;

import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.account.service.move.control.MovePreSaveControlService;
import com.axelor.apps.account.service.move.control.accounting.MoveAccountingControlService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import java.lang.invoke.MethodHandles;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveListener {

  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @PrePersist
  @PreUpdate
  public void beforeSave(Move move) throws AxelorException {

    log.debug("Applying pre-save operations on move {}", move);

    Beans.get(MovePreSaveControlService.class).checkValidity(move);

    if (move.getStatusSelect() == MoveRepository.STATUS_ACCOUNTED
        || move.getStatusSelect() == MoveRepository.STATUS_SIMULATED
        || move.getStatusSelect() == MoveRepository.STATUS_VALIDATED) {
      Beans.get(MoveAccountingControlService.class).controlAccounting(move);
    }

    log.debug("Applied pre-save operations");
  }
}

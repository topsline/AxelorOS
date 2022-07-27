package com.axelor.apps.account.service.move.control;

import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.service.move.control.moveline.MoveLinePreSaveControlService;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovePreSaveControlServiceImpl implements MovePreSaveControlService {

  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected MoveChangeControlService moveChangeControlService;

  @Inject
  public MovePreSaveControlServiceImpl(
      MoveLinePreSaveControlService moveLinePreSaveControlService,
      MoveChangeControlService moveChangeControlService) {
    this.moveChangeControlService = moveChangeControlService;
  }

  @Override
  public void checkValidity(Move move) throws AxelorException {

    log.debug("Checking validity of move {}", move);
    Objects.requireNonNull(move);
    moveChangeControlService.checkIllegalRemoval(move);
  }
}

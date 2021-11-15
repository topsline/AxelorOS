package com.axelor.apps.account.service.fecimport;

import java.util.List;

import com.axelor.apps.account.db.FECImport;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.repo.FECImportRepository;
import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.move.MoveValidateService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class FECImportServiceImpl implements FECImportService {

  protected MoveValidateService moveValidateService;
  protected AppAccountService appAccountService;
  protected MoveRepository moveRepository;
  protected FECImportRepository fecImportRepository;

  @Inject
  public FECImportServiceImpl(
      MoveValidateService moveValidateService,
      AppAccountService appAccountService,
      MoveRepository moveRepository,
      FECImportRepository fecImportRepository) {
    this.moveValidateService = moveValidateService;
    this.appAccountService = appAccountService;
    this.moveRepository = moveRepository;
    this.fecImportRepository = fecImportRepository;
  }

  @Override
  public void completeImport(FECImport fecImport, List<Move> moves) {
    completeAndvalidateMoves(fecImport, moves);
  }

  @Transactional
  protected void completeAndvalidateMoves(FECImport fecImport, List<Move> moveList) {
    if (fecImport != null) {
    	fecImport = fecImportRepository.find(fecImport.getId());
      for (Move move : moveList) {
        try {
          move = moveRepository.find(move.getId());
          if (move != null) {
              move.setDescription(fecImport.getMoveDescription());
              if (move.getValidationDate() != null) {
                move.setReference(
                    String.format(
                        "%s%s%s",
                        fecImport.getId().toString(),
                        move.getReference(),
                        appAccountService.getTodayDate(move.getCompany()).toString()));
              }
              if (fecImport.getCompany() == null) {
                fecImport.setCompany(move.getCompany());
              }
              move.setFecImport(fecImport);
              if (fecImport.getValidGeneratedMove()) {
                moveValidateService.validate(move);
              } else {
                moveRepository.save(move);
              }  
          }

        } catch (Exception e) {
          move.setStatusSelect(MoveRepository.STATUS_NEW);
        }
      }
    }
  }
}

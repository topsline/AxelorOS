package com.axelor.apps.account.service.fecimport;

import com.axelor.apps.account.db.FECImport;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.db.repo.FECImportRepository;
import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.move.MoveValidateService;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.ImportHistory;
import com.axelor.apps.base.db.repo.CompanyRepository;
import com.axelor.apps.base.service.imports.importer.Importer;
import com.axelor.apps.base.service.imports.listener.ImporterListener;
import com.axelor.data.csv.CSVImporter;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FECImporter extends Importer {

  protected MoveValidateService moveValidateService;
  protected AppAccountService appAccountService;
  protected MoveRepository moveRepository;
  protected FECImportRepository fecImportRepository;
  protected CompanyRepository companyRepository;
  private final List<Move> moveList = new ArrayList<>();
  private FECImport fecImport;
  private Company company;

  @Inject
  public FECImporter(
      MoveValidateService moveValidateService,
      AppAccountService appAccountService,
      MoveRepository moveRepository,
      FECImportRepository fecImportRepository,
      CompanyRepository companyRepository) {
    this.moveValidateService = moveValidateService;
    this.appAccountService = appAccountService;
    this.moveRepository = moveRepository;
    this.fecImportRepository = fecImportRepository;
    this.companyRepository = companyRepository;
  }

  @Override
  protected ImportHistory process(String bind, String data, Map<String, Object> importContext)
      throws IOException {

    CSVImporter importer = new CSVImporter(bind, data);

    ImporterListener listener =
        new ImporterListener(getConfiguration().getName()) {
          @Override
          public void handle(Model bean, Exception e) {
            super.handle(bean, e);
          }

          @Override
          public void imported(Integer total, Integer success) {
            try {
              completeAndvalidateMoves(fecImport, moveList, this);
            } catch (Exception e) {
              this.handle(null, e);
            }
            super.imported(total, success);
          }

          @Override
          public void imported(Model bean) {
            addMoveFromMoveLine(bean);
            super.imported(bean);
          }
        };

    importer.addListener(listener);
    importer.setContext(importContext);
    importer.run();
    return addHistory(listener);
  }

  protected void addMoveFromMoveLine(Model bean) {
    if (bean.getClass().equals(MoveLine.class)) {
      MoveLine moveLine = (MoveLine) bean;
      if (moveLine.getMove() != null) {
        Move move = moveLine.getMove();
        if (!moveList.contains(move)) {
          moveList.add(move);
        }
      }
    }
  }

  public FECImporter addFecImport(FECImport fecImport) {
    this.fecImport = fecImport;
    return this;
  }

  @Override
  protected ImportHistory process(String bind, String data) throws IOException {
    return process(bind, data, null);
  }

  public List<Move> getMoves() {
    return this.moveList;
  }

  protected void completeAndvalidateMoves(
      FECImport fecImport, List<Move> moveList, ImporterListener listener) {
    if (fecImport != null) {
      int i = 0;
      Long companyId = null;
      for (Move move : moveList) {
        move = moveRepository.find(move.getId());
        if (companyId == null && move != null) {
          companyId = move.getCompany().getId();
        }
        // We modify move in two parts. First part we set description and fecImport on the move
        // Second part we set reference and validate the move if necessary.
        // We do this in two parts because reference for move must be unique, and in case there is
        // an error the rollback must not undo description and fecImport.
        move = setDescriptionAndFecImport(fecImport, listener, move);
        move = setReferenceAndValidate(fecImport, listener, move);
        if (i % 10 == 0) {
          JPA.clear();
        }
        i++;
      }
      this.company = companyRepository.find(companyId);
    }
  }

  @Transactional
  protected Move setReferenceAndValidate(
      FECImport fecImport, ImporterListener listener, Move move) {
    try {

      if (move != null) {
        String csvReference = extractCSVMoveReference(move.getReference());

        if (move.getValidationDate() != null) {
          move.setReference(String.format("%s", csvReference));
        } else {
          move.setReference(String.format("#%s", move.getId().toString()));
        }

        if (fecImport.getValidGeneratedMove()) {
          moveValidateService.validate(move);
        } else {
          return moveRepository.save(move);
        }
        return move;
      }

    } catch (Exception e) {
      move.setStatusSelect(MoveRepository.STATUS_NEW);
      listener.handle(move, e);
    }
    return null;
  }

  @Transactional
  protected Move setDescriptionAndFecImport(
      FECImport fecImport, ImporterListener listener, Move move) {
    try {
      if (move != null) {
        fecImport = fecImportRepository.find(fecImport.getId());
        move.setDescription(fecImport.getMoveDescription());
        move.setFecImport(fecImport);
        return moveRepository.save(move);
      }

    } catch (Exception e) {
      move.setStatusSelect(MoveRepository.STATUS_NEW);
      listener.handle(move, e);
    }
    return null;
  }

  protected String extractCSVMoveReference(String reference) {
    if (reference != null) {
      int indexOfSeparator = reference.indexOf("-");
      if (indexOfSeparator < 0) {
        return reference.replaceFirst("#", "");
      } else {
        return reference.substring(0, indexOfSeparator).replaceFirst("#", "");
      }
    }
    return reference;
  }

  public Company getCompany() {
    return this.company;
  }
}

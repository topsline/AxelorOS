package com.axelor.apps.account.service.move.massentry;

import com.axelor.apps.account.db.Journal;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.db.MoveLineMassEntry;
import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.base.db.repo.YearRepository;
import com.axelor.apps.base.service.PeriodService;
import com.axelor.common.ObjectUtils;
import com.google.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MassEntryToolServiceImpl implements MassEntryToolService {

  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected PeriodService periodService;

  @Inject
  public MassEntryToolServiceImpl(PeriodService periodService) {
    this.periodService = periodService;
  }

  @Override
  public void clearMoveLineMassEntryListAndAddNewLines(
      Move massEntryMove, Move move, Integer temporaryMoveNumber) {
    List<MoveLineMassEntry> moveLineMassEntryList =
        new ArrayList<>(massEntryMove.getMoveLineMassEntryList());
    for (MoveLineMassEntry moveLineMassEntry : moveLineMassEntryList) {
      if (Objects.equals(moveLineMassEntry.getTemporaryMoveNumber(), temporaryMoveNumber)) {
        massEntryMove.removeMoveLineMassEntryListItem(moveLineMassEntry);
      }
    }
    this.sortMoveLinesMassEntryByTemporaryNumber(massEntryMove);

    moveLineMassEntryList =
        convertMoveLinesIntoMoveLineMassEntry(move, move.getMoveLineList(), temporaryMoveNumber);
    if (moveLineMassEntryList.size() > 0) {
      for (MoveLineMassEntry moveLineMassEntry : moveLineMassEntryList) {
        massEntryMove.addMoveLineMassEntryListItem(moveLineMassEntry);
      }
    }
  }

  @Override
  public void sortMoveLinesMassEntryByTemporaryNumber(Move move) {
    if (ObjectUtils.notEmpty(move.getMoveLineMassEntryList())) {
      move.getMoveLineMassEntryList()
          .sort(
              new Comparator<MoveLineMassEntry>() {
                @Override
                public int compare(MoveLineMassEntry o1, MoveLineMassEntry o2) {
                  return o1.getTemporaryMoveNumber() - o2.getTemporaryMoveNumber();
                }
              });
    }
  }

  @Override
  public List<MoveLineMassEntry> convertMoveLinesIntoMoveLineMassEntry(
      Move move, List<MoveLine> moveLines, Integer temporaryMoveNumber) {
    List<MoveLineMassEntry> moveLineMassEntryList = new ArrayList<>();
    if (move != null && ObjectUtils.notEmpty(moveLines)) {
      for (MoveLine moveLine : moveLines) {
        moveLineMassEntryList.add(
            this.convertMoveLineIntoMoveLineMassEntry(move, moveLine, temporaryMoveNumber));
      }
    }
    return moveLineMassEntryList;
  }

  @Override
  public MoveLineMassEntry convertMoveLineIntoMoveLineMassEntry(
      Move move, MoveLine moveLine, Integer tempMoveNumber) {
    MoveLineMassEntry moveLineMassEntry = new MoveLineMassEntry();
    if (move != null && moveLine != null) {
      moveLineMassEntry.setInputAction(1);
      moveLineMassEntry.setMovePaymentMode(move.getPaymentMode());
      moveLineMassEntry.setMovePaymentCondition(move.getPaymentCondition());
      moveLineMassEntry.setTemporaryMoveNumber(tempMoveNumber);
      moveLineMassEntry.setMoveDescription(move.getDescription());
      moveLineMassEntry.setMovePartnerBankDetails(move.getPartnerBankDetails());
      moveLineMassEntry.setMoveStatusSelect(move.getStatusSelect());

      moveLineMassEntry.setPartner(moveLine.getPartner());
      moveLineMassEntry.setAccount(moveLine.getAccount());
      moveLineMassEntry.setDate(moveLine.getDate());
      moveLineMassEntry.setDueDate(moveLine.getDueDate());
      moveLineMassEntry.setCutOffStartDate(moveLine.getCutOffStartDate());
      moveLineMassEntry.setCutOffEndDate(moveLine.getCutOffEndDate());
      moveLineMassEntry.setCounter(moveLine.getCounter());
      moveLineMassEntry.setDebit(moveLine.getDebit());
      moveLineMassEntry.setCredit(moveLine.getCredit());
      moveLineMassEntry.setDescription(moveLine.getDescription());
      moveLineMassEntry.setOrigin(moveLine.getOrigin());
      moveLineMassEntry.setOriginDate(moveLine.getOriginDate());
      moveLineMassEntry.setTaxLine(moveLine.getTaxLine());
      moveLineMassEntry.setTaxLineBeforeReverse(moveLine.getTaxLineBeforeReverse());
      moveLineMassEntry.setCurrencyAmount(moveLine.getCurrencyAmount());
      moveLineMassEntry.setCurrencyRate(moveLine.getCurrencyRate());
      moveLineMassEntry.setSourceTaxLine(moveLine.getSourceTaxLine());
    }

    return moveLineMassEntry;
  }

  @Override
  public List<MoveLineMassEntry> getEditedMoveLineMassEntry(
      List<MoveLineMassEntry> moveLineMassEntryList) {
    List<MoveLineMassEntry> resultList = new ArrayList<>();

    for (MoveLineMassEntry moveLineMassEntry : moveLineMassEntryList) {
      if (moveLineMassEntry.getIsEdited()) {
        resultList.add(moveLineMassEntry);
      }
    }
    return resultList;
  }

  @Override
  public List<Move> createMoveListFromMassEntryList(Move parentMove) {
    List<Move> moveList = new ArrayList<>();
    Move moveToAdd;

    for (int i = 1;
        i <= this.getMaxTemporaryMoveNumber(parentMove.getMoveLineMassEntryList());
        i++) {
      moveToAdd = this.createMoveFromMassEntryList(parentMove, i);
      moveList.add(moveToAdd);
    }

    return moveList;
  }

  public Move createMoveFromMassEntryList(Move parentMove, int temporaryMoveNumber) {
    Move moveResult = new Move();
    boolean firstMoveLine = true;

    moveResult.setJournal(parentMove.getJournal());
    moveResult.setCompany(parentMove.getCompany());
    moveResult.setCurrency(parentMove.getCurrency());
    moveResult.setCompanyBankDetails(parentMove.getCompanyBankDetails());

    for (MoveLineMassEntry massEntryLine : parentMove.getMoveLineMassEntryList()) {
      if (massEntryLine.getTemporaryMoveNumber() == temporaryMoveNumber
          && massEntryLine.getInputAction() == 1) {
        if (firstMoveLine) {
          if (massEntryLine.getDate() != null && moveResult.getCompany() != null) {
            moveResult.setPeriod(
                periodService.getPeriod(
                    massEntryLine.getDate(), moveResult.getCompany(), YearRepository.TYPE_FISCAL));
          }
          moveResult.setReference(massEntryLine.getTemporaryMoveNumber().toString());
          moveResult.setDate(massEntryLine.getDate());
          moveResult.setPartner(massEntryLine.getPartner());
          moveResult.setOrigin(massEntryLine.getOrigin());
          moveResult.setStatusSelect(massEntryLine.getMoveStatusSelect());
          moveResult.setOriginDate(massEntryLine.getOriginDate());
          moveResult.setDescription(massEntryLine.getMoveDescription());
          moveResult.setPaymentMode(massEntryLine.getMovePaymentMode());
          moveResult.setPaymentCondition(massEntryLine.getMovePaymentCondition());
          moveResult.setPartnerBankDetails(massEntryLine.getMovePartnerBankDetails());
          firstMoveLine = false;
        }
        massEntryLine.setMove(moveResult);
        massEntryLine.setFieldsErrorList(null);
        moveResult.addMoveLineListItem(massEntryLine);
        moveResult.addMoveLineMassEntryListItem(massEntryLine);
      }
    }

    return moveResult;
  }

  @Override
  public Integer getMaxTemporaryMoveNumber(List<MoveLineMassEntry> moveLineMassEntryList) {
    int max = 0;

    for (MoveLineMassEntry moveLine : moveLineMassEntryList) {
      if (moveLine.getTemporaryMoveNumber() > max) {
        max = moveLine.getTemporaryMoveNumber();
      }
    }

    return max;
  }

  @Override
  public void setNewStatusSelectOnMassEntryLines(Move move, Integer newStatusSelect) {
    if (ObjectUtils.notEmpty(move.getMoveLineMassEntryList())) {
      for (MoveLineMassEntry moveLineMassEntry : move.getMoveLineMassEntryList()) {
        moveLineMassEntry.setMoveStatusSelect(newStatusSelect);
      }
    }
  }

  @Override
  public boolean verifyJournalAuthorizeNewMove(
      List<MoveLineMassEntry> moveLineMassEntryList, Journal journal) {
    if (!journal.getAllowAccountingNewOnMassEntry()) {
      for (MoveLineMassEntry moveLineMassEntry : moveLineMassEntryList) {
        if (moveLineMassEntry.getMoveStatusSelect().equals(MoveRepository.STATUS_NEW)) {
          return false;
        }
      }
    }
    return true;
  }
}

package com.axelor.apps.base.service.batch;

import com.axelor.apps.base.db.BatchImportHistory;
import com.axelor.apps.base.db.FileSourceConnectorParameters;
import com.axelor.apps.base.db.repo.BatchImportHistoryRepository;
import com.axelor.apps.base.service.administration.AbstractBatch;
import com.axelor.apps.base.service.filesourceconnector.FileSourceConnectorService;
import com.axelor.apps.base.service.filesourceconnector.models.FileTransfertSession;
import com.axelor.apps.base.translation.ITranslation;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.util.List;

public abstract class AbstractImportBatch extends AbstractBatch {

  protected FileSourceConnectorService fileSourceConnectorService;
  protected BatchImportHistoryRepository batchImportHistoryRepository;
  protected UserRepository userRepository;

  @Inject
  protected AbstractImportBatch(
      FileSourceConnectorService fileSourceConnectorService,
      BatchImportHistoryRepository batchImportHistoryRepository,
      UserRepository userRepository) {
    this.fileSourceConnectorService = fileSourceConnectorService;
    this.batchImportHistoryRepository = batchImportHistoryRepository;
    this.userRepository = userRepository;
  }

  protected List<MetaFile> downloadFiles(
      FileSourceConnectorParameters fileSourceConnectorParameters) throws AxelorException {

    FileTransfertSession session =
        fileSourceConnectorService.createSession(
            fileSourceConnectorParameters.getFileSourceConnector());
    return fileSourceConnectorService.download(session, fileSourceConnectorParameters);
  }

  @Override
  protected void stop() {

    StringBuilder comment = new StringBuilder();
    comment.append(
        "\t"
            + String.format(
                I18n.get(ITranslation.BASE_IMPORT_BATCH_FILES_IMPORTED), batch.getDone()));
    comment.append(
        "\t"
            + String.format(
                I18n.get(com.axelor.apps.base.exceptions.IExceptionMessage.ALARM_ENGINE_BATCH_4),
                batch.getAnomaly()));
    addComment(comment.toString());
    super.stop();
  }

  @Transactional
  protected void createBatchHistory(MetaFile dataMetaFile, MetaFile logMetaFile) {
    BatchImportHistory batchImportHistory = new BatchImportHistory();
    batchImportHistory.setBatch(batchRepo.find(getCurrentBatchId()));
    batchImportHistory.setDataMetaFile(dataMetaFile);
    batchImportHistory.setLogMetaFile(logMetaFile);
    batchImportHistory.setUser(userRepository.find(batch.getCreatedBy().getId()));
    batchImportHistoryRepository.save(batchImportHistory);
  }
}

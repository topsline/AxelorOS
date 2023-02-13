package com.axelor.apps.gdpr.service.response;

import com.axelor.apps.gdpr.db.GDPRRequest;
import com.axelor.apps.gdpr.db.repo.GDPRRequestRepository;
import com.axelor.apps.message.db.EmailAddress;
import com.axelor.auth.db.AuditableModel;
import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.exception.AxelorException;
import com.axelor.meta.db.MetaField;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.persistence.Query;
import wslite.json.JSONException;

public class GdprResponseServiceImpl implements GdprResponseService {

  protected GdprResponseAccessService gdprResponseAccessService;
  protected GdprResponseErasureService gdprResponseErasureService;

  @Inject
  public GdprResponseServiceImpl(
      GdprResponseAccessService gdprResponseAccessService,
      GdprResponseErasureService gdprResponseErasureService) {
    this.gdprResponseAccessService = gdprResponseAccessService;
    this.gdprResponseErasureService = gdprResponseErasureService;
  }

  @Override
  public List<MetaField> selectMetaFields(GDPRRequest gdprRequest) throws ClassNotFoundException {

    Class<? extends AuditableModel> modelSelectKlass =
        extractClassFromModel(gdprRequest.getModelSelect());
    String modelSelect = modelSelectKlass.getSimpleName();

    Query selectedObj =
        JPA.em()
            .createQuery(
                "SELECT self FROM MetaField self WHERE self.typeName= :modelSelect AND self.relationship NOT LIKE 'ManyToMany'");
    selectedObj.setParameter("modelSelect", modelSelect);

    return selectedObj.getResultList();
  }

  @Override
  public Optional<String> getEmailFromPerson(AuditableModel reference) {
    Mapper mapper = Mapper.of(reference.getClass());
    return Optional.ofNullable(mapper.get(reference, "emailAddress"))
        .map(EmailAddress.class::cast)
        .map(EmailAddress::getAddress);
  }

  @Override
  public Class<? extends AuditableModel> extractClassFromModel(String model)
      throws ClassNotFoundException {
    return (Class<? extends AuditableModel>) Class.forName(model);
  }

  @Override
  public AuditableModel extractReferenceFromModelAndId(String model, Long id)
      throws ClassNotFoundException {
    return com.axelor.db.Query.of(extractClassFromModel(model)).filter("id = ?", id).fetchOne();
  }

  @Override
  public void generateResponse(GDPRRequest gdprRequest)
      throws AxelorException, IOException, ClassNotFoundException, JSONException {
    if (gdprRequest.getTypeSelect() == GDPRRequestRepository.REQUEST_TYPE_ACCESS) {
      gdprResponseAccessService.generateAccessResponseDataFile(gdprRequest);

    } else {
      gdprResponseErasureService.createErasureResponse(gdprRequest);
      gdprResponseErasureService.anonymizeTrackingDatas(gdprRequest);
    }
  }

  @Transactional(rollbackOn = {Exception.class})
  @Override
  public void sendResponse(GDPRRequest gdprRequest) throws AxelorException {
    if (gdprRequest.getTypeSelect() == GDPRRequestRepository.REQUEST_TYPE_ACCESS) {
      gdprResponseAccessService.sendEmailResponse(gdprRequest.getGdprResponse());
    } else {
      gdprResponseErasureService.sendEmailResponse(gdprRequest.getGdprResponse());
    }
    gdprRequest.setStatusSelect(GDPRRequestRepository.REQUEST_STATUS_SENT);
  }
}

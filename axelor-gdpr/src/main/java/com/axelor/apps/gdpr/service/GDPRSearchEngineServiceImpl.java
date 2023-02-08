package com.axelor.apps.gdpr.service;

import com.axelor.apps.gdpr.db.GDPRSearchConfig;
import com.axelor.apps.gdpr.db.GDPRSearchConfigLine;
import com.axelor.apps.gdpr.exception.GdprExceptionMessage;
import com.axelor.apps.gdpr.service.app.AppGDPRService;
import com.axelor.auth.db.AuditableModel;
import com.axelor.db.Query;
import com.axelor.db.mapper.Mapper;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaField;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.axelor.rpc.Context;
import com.axelor.script.GroovyScriptHelper;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class GDPRSearchEngineServiceImpl implements GDPRSearchEngineService {

  protected AppGDPRService appGDPRService;
  protected MetaModelRepository metaModelRepo;

  @Inject
  public GDPRSearchEngineServiceImpl(
      AppGDPRService appGDPRService, MetaModelRepository metaModelRepo) {
    this.appGDPRService = appGDPRService;
    this.metaModelRepo = metaModelRepo;
  }

  @Override
  public List<Map<String, Object>> searchObject(Map<String, Object> searchParams)
      throws AxelorException {

    List<Map<String, Object>> results = new ArrayList<>();

    try {
      results = bindDataUsingSearchConfig(searchParams);

    } catch (ClassNotFoundException e) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR, I18n.get("Can not find object."));
    }

    return results;
  }

  /**
   * search using search config
   *
   * @param searchParams
   * @return
   * @throws ClassNotFoundException
   */
  public List<Map<String, Object>> bindDataUsingSearchConfig(Map<String, Object> searchParams)
      throws ClassNotFoundException {
    List<GDPRSearchConfig> searchConfigs = appGDPRService.getAppGDPR().getRequestSearchConfig();

    List<Map<String, Object>> results = new ArrayList<>();

    for (GDPRSearchConfig searchConfig : searchConfigs) {
      MetaModel metaModel = searchConfig.getMetaModel();
      Class<? extends AuditableModel> modelClass =
          (Class<? extends AuditableModel>) Class.forName(metaModel.getFullName());

      String query = buildSearchQuery(searchParams, searchConfig);

      //       apply search config query
      List<? extends AuditableModel> models =
          Query.of(modelClass).filter(query).bind(searchParams).fetch();

      models.forEach(model -> results.add(convertResultToDisplayMap(searchConfig, model)));
    }

    return results;
  }

  public String buildSearchQuery(Map<String, Object> searchParams, GDPRSearchConfig searchConfig) {
    StringBuilder query = new StringBuilder();
    List<GDPRSearchConfigLine> searchConfigLines = searchConfig.getSearchConfigLines();

    for (GDPRSearchConfigLine searchConfigLine : searchConfigLines) {
      String param =
          Optional.ofNullable(searchParams.get(searchConfigLine.getKey()))
              .map(Object::toString)
              .orElse("");

      if (StringUtils.isEmpty(param)) {
        continue;
      }

      query.append(searchConfigLine.getQuery());
      query.append(" AND ");
    }

    query.append(" 1 = 1");
    return query.toString();
  }

  /**
   * @param searchConfig
   * @param reference
   * @return
   */
  public Map<String, Object> convertResultToDisplayMap(
      GDPRSearchConfig searchConfig, AuditableModel reference) {
    Context scriptContext = new Context(Mapper.toMap(reference), reference.getClass());
    Map<String, Object> mappedObject = new HashMap<>();

    mappedObject.put("type", I18n.get(reference.getClass().getSimpleName()));
    mappedObject.put("typeClass", reference.getClass().getName());
    mappedObject.put("objectId", reference.getId());

    for (GDPRSearchConfigLine searchConfigLine : searchConfig.getSearchConfigLines()) {
      mappedObject.put(
          searchConfigLine.getKey(), evalField(scriptContext, searchConfigLine.getMapping()));
    }

    return mappedObject;
  }

  public String evalField(Context context, String fieldName) {
    String[] fields = fieldName.split("\\.");
    int count = 0;
    StringBuilder fieldToTest = new StringBuilder();
    Optional<Object> value = Optional.empty();

    while (count < fields.length) {
      if (fieldToTest.length() > 0) {
        fieldToTest.append(".");
      }
      fieldToTest.append(fields[count]);
      value = Optional.ofNullable(new GroovyScriptHelper(context).eval(fieldToTest.toString()));

      if (!value.isPresent()) {
        break;
      }
      count++;
    }

    return value.map(Object::toString).orElse("");
  }

  /**
   * get string value for given field name
   *
   * @param metaModel
   * @param mapper
   * @param reference
   * @param fieldName
   * @return
   * @throws AxelorException
   * @throws ClassNotFoundException
   */
  protected String getFieldValue(
      MetaModel metaModel, Mapper mapper, AuditableModel reference, String fieldName)
      throws AxelorException, ClassNotFoundException {

    String[] fields = fieldName.split("\\.", 2);

    Optional<Object> value = Optional.ofNullable(mapper.get(reference, fields[0]));

    if (fields.length == 1 || !value.isPresent()) {
      // simple case
      return value.map(Object::toString).orElse(StringUtils.EMPTY);
    } else {
      // handle subobject case
      MetaField metaField =
          metaModel.getMetaFields().stream()
              .filter(mf -> fields[0].equals(mf.getName()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new AxelorException(
                          TraceBackRepository.CATEGORY_INCONSISTENCY,
                          I18n.get(GdprExceptionMessage.FIELD_NOT_FOUND)));

      String fullName = metaField.getPackageName() + "." + metaField.getTypeName();
      Class<? extends AuditableModel> subObjectClass =
          (Class<? extends AuditableModel>) Class.forName(fullName);

      MetaModel subMetaModel =
          metaModelRepo.all().filter("self.fullName = '" + fullName + "'").fetchOne();
      AuditableModel model = subObjectClass.cast(mapper.get(reference, fields[0]));
      Mapper subMapper = Mapper.of(subObjectClass);

      return getFieldValue(subMetaModel, subMapper, model, fields[1]);
    }
  }
}

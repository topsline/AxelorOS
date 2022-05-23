/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.base.service.app;

import com.axelor.apps.base.db.FakerApiField;
import com.axelor.apps.base.exceptions.IExceptionMessages;
import com.axelor.db.mapper.Property;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaJsonField;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wslite.json.JSONException;
import wslite.json.JSONObject;

public class AnonymizeServiceImpl implements AnonymizeService {

  protected FakerService fakerService;

  protected Logger LOG = LoggerFactory.getLogger(getClass());

  @Inject
  public AnonymizeServiceImpl(FakerService fakerService) {
    this.fakerService = fakerService;
  }

  @Override
  public Object anonymizeValue(Object object, Property property) throws AxelorException {
    try {
      if (property.isJson()) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessages.JSON_FIELD_CAN_NOT_BE_ANONYMIZED));
      }
      if (property.getMaxSize() != null && (int) property.getMaxSize() > 0) {
        return anonymize(object, property.getType().toString(), (int) property.getMaxSize());
      } else {
        return anonymize(object, property.getType().toString(), 0);
      }
    } catch (NoSuchAlgorithmException e) {
      throw new AxelorException(TraceBackRepository.CATEGORY_CONFIGURATION_ERROR, e.getMessage());
    }
  }

  @Override
  public Object anonymizeValue(Object object, Property property, FakerApiField fakerApiField)
      throws AxelorException {

    if (fakerApiField != null) {
      return fakerService.generateFakeData(fakerApiField);
    }

    return anonymizeValue(object, property);
  }

  @Override
  public JSONObject createAnonymizedJson(
      Object object, HashMap<MetaJsonField, FakerApiField> fakerMap) throws AxelorException {
    JSONObject anonymizedJson = new JSONObject();
    List<String> metaJsonFieldsNameToAnonymize =
        fakerMap.keySet().stream().map(MetaJsonField::getName).collect(Collectors.toList());
    for (MetaJsonField metaJsonField : fakerMap.keySet()) {
      metaJsonFieldsNameToAnonymize.add(metaJsonField.getName());
    }
    try {
      JSONObject jsonObject = new JSONObject(object.toString());
      for (Object field : jsonObject.keySet()) {
        if (metaJsonFieldsNameToAnonymize.contains(field.toString())) {
          anonymizedJson.put(
              field.toString(),
              anonymizeJsonValue(
                  fakerMap, field.toString(), jsonObject.get(field.toString()).toString()));
        } else {
          anonymizedJson.put(field.toString(), jsonObject.get(field.toString()).toString());
        }
      }
      return anonymizedJson;
    } catch (JSONException | NoSuchAlgorithmException e) {
      throw new AxelorException(TraceBackRepository.CATEGORY_CONFIGURATION_ERROR, e.getMessage());
    }
  }

  protected Object anonymizeJsonValue(
      HashMap<MetaJsonField, FakerApiField> fakerMap, String fieldName, String objectValue)
      throws AxelorException, NoSuchAlgorithmException {
    MetaJsonField metaJsonField =
        fakerMap.keySet().stream()
            .filter(metaJsonField1 -> fieldName.equals(metaJsonField1.getName()))
            .findAny()
            .get(); // we will always find a metaJsonField since we called contains on the list
    // before
    if (fakerMap.get(metaJsonField) != null) {
      return fakerService.generateFakeData(fakerMap.get(metaJsonField));
    } else {
      return anonymize(objectValue, metaJsonField.getType(), metaJsonField.getMaxSize());
    }
  }

  protected Object anonymize(Object object, String type, int maxSize)
      throws NoSuchAlgorithmException {
    switch (type.toLowerCase()) {
      case "text":
      case "string":
        byte[] shaInBytes = hashString(object.toString());

        if (maxSize != 0 && shaInBytes.length > maxSize) {
          return bytesToHex(shaInBytes).substring(0, maxSize);
        } else {
          return bytesToHex(shaInBytes);
        }

      case "long":
      case "double":
        return 0;

      case "integer":
        return BigInteger.ZERO;

      case "decimal":
        return BigDecimal.ZERO;

      case "date":
        return LocalDate.of(1970, 1, 1);

      case "time":
        return LocalTime.of(0, 0, 0);

      case "datetime":
        return LocalDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 0));

      case "binary":
      default:
        return null;
    }
  }

  protected byte[] hashString(String data) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-256");
      md.update(getSalt());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e);
    }
    byte[] result = md.digest(data.getBytes(StandardCharsets.UTF_8));
    return result;
  }

  protected byte[] getSalt() throws NoSuchAlgorithmException {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    return salt;
  }

  protected String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}

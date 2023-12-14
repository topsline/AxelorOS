/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.apps.base.service;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.AddressTemplate;
import com.axelor.apps.base.db.AddressTemplateLine;
import com.axelor.apps.base.db.City;
import com.axelor.apps.base.db.Country;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PartnerAddress;
import com.axelor.apps.base.db.PickListEntry;
import com.axelor.apps.base.db.Street;
import com.axelor.apps.base.db.repo.AddressTemplateRepository;
import com.axelor.apps.base.db.repo.CityRepository;
import com.axelor.apps.base.db.repo.StreetRepository;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.base.exceptions.BaseExceptionMessage;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.i18n.I18n;
import com.axelor.rpc.Context;
import com.axelor.text.GroovyTemplates;
import com.axelor.text.StringTemplates;
import com.axelor.text.Templates;
import com.axelor.utils.helpers.address.AddressHelper;
import com.google.api.client.util.Maps;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wslite.json.JSONException;

@Singleton
public class AddressServiceImpl implements AddressService {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected final String EMPTY_LINE_REMOVAL_REGEX = "(?m)^\\s*$(\\n|\\r\\n)";
  private static final char TEMPLATE_DELIMITER = '$';
  private GroovyTemplates groovyTemplates;
  protected AddressHelper ads;
  protected CityRepository cityRepository;
  protected StreetRepository streetRepository;
  protected AppBaseService appBaseService;
  protected AddressAttrsService addressAttrsService;

  protected MapService mapService;
  protected static final Set<Function<Long, Boolean>> checkUsedFuncs = new LinkedHashSet<>();

  private static final Pattern ZIP_CODE_PATTERN =
      Pattern.compile(
          "\\d{4} [A-Z]{2} |\\d{4,6}|\\d{3}-\\d{4}|[A-Z]{1,2}[0-9][A-Z0-9]? [0-9][A-Z]{1}[A-Z0-9]?");

  static {
    registerCheckUsedFunc(AddressServiceImpl::checkAddressUsedBase);
  }

  @Inject
  public AddressServiceImpl(
      GroovyTemplates groovyTemplates,
      AddressHelper ads,
      MapService mapService,
      CityRepository cityRepository,
      StreetRepository streetRepository,
      AppBaseService appBaseService,
      AddressAttrsService addressAttrsService) {
    this.groovyTemplates = groovyTemplates;
    this.ads = ads;
    this.mapService = mapService;
    this.cityRepository = cityRepository;
    this.streetRepository = streetRepository;
    this.appBaseService = appBaseService;
    this.addressAttrsService = addressAttrsService;
  }

  @Override
  public boolean check(String wsdlUrl) {
    return ads.doCanSearch(wsdlUrl);
  }

  @Override
  public Map<String, Object> validate(String wsdlUrl, String search) {
    return ads.doSearch(wsdlUrl, search);
  }

  @Override
  public com.qas.web_2005_02.Address select(String wsdlUrl, String moniker) {
    return ads.doGetAddress(wsdlUrl, moniker);
  }

  @Override
  public Address createAddress(
      String room, String floor, String streetName, String postBox, Country country) {

    Address address = new Address();
    address.setRoom(room);
    address.setFloor(floor);
    address.setStreetName(streetName);
    address.setPostBox(postBox);
    address.setCountry(country);

    return address;
  }

  @Override
  public boolean checkAddressUsed(Long addressId) {
    LOG.debug("Address Id to be checked = {}", addressId);
    return checkUsedFuncs.stream().anyMatch(checkUsedFunc -> checkUsedFunc.apply(addressId));
  }

  protected static void registerCheckUsedFunc(Function<Long, Boolean> checkUsedFunc) {
    checkUsedFuncs.add(checkUsedFunc);
  }

  private static boolean checkAddressUsedBase(Long addressId) {
    return JPA.all(PartnerAddress.class).filter("self.address.id = ?1", addressId).fetchOne()
            != null
        || JPA.all(Partner.class).filter("self.mainAddress.id = ?1", addressId).fetchOne() != null
        || JPA.all(PickListEntry.class).filter("self.address.id = ?1", addressId).fetchOne()
            != null;
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  public Optional<Pair<BigDecimal, BigDecimal>> getOrUpdateLatLong(Address address)
      throws AxelorException, JSONException {
    Preconditions.checkNotNull(address, I18n.get(BaseExceptionMessage.ADDRESS_CANNOT_BE_NULL));
    Optional<Pair<BigDecimal, BigDecimal>> latLong = getLatLong(address);

    if (latLong.isPresent()) {
      return latLong;
    }

    return updateLatLong(address);
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  public Optional<Pair<BigDecimal, BigDecimal>> updateLatLong(Address address)
      throws AxelorException, JSONException {
    Preconditions.checkNotNull(address, I18n.get(BaseExceptionMessage.ADDRESS_CANNOT_BE_NULL));

    if (mapService.isConfigured() && StringUtils.notBlank(address.getFullName())) {
      Map<String, Object> result = mapService.getMap(address.getFullName());
      if (result == null) {
        address.setIsValidLatLong(false);
        return Optional.empty();
      }
      address.setIsValidLatLong(true);
      BigDecimal latitude = (BigDecimal) result.get("latitude");
      BigDecimal longitude = (BigDecimal) result.get("longitude");
      setLatLong(address, Pair.of(latitude, longitude));
    }

    return getLatLong(address);
  }

  @Override
  @Transactional
  public void resetLatLong(Address address) {
    Preconditions.checkNotNull(address, I18n.get(BaseExceptionMessage.ADDRESS_CANNOT_BE_NULL));
    setLatLong(address, Pair.of(null, null));
  }

  protected void setLatLong(Address address, Pair<BigDecimal, BigDecimal> latLong) {
    address.setLatit(latLong.getLeft());
    address.setLongit(latLong.getRight());
  }

  protected Optional<Pair<BigDecimal, BigDecimal>> getLatLong(Address address) {
    if (address.getLatit() != null && address.getLongit() != null) {
      return Optional.of(Pair.of(address.getLatit(), address.getLongit()));
    }

    return Optional.empty();
  }

  @Override
  public String computeFullName(Address address) {

    String l2 = address.getRoom();
    String l3 = address.getFloor();
    String l4 = address.getStreetName();
    String l5 = address.getPostBox();

    return (!Strings.isNullOrEmpty(l2) ? l2 : "")
        + (!Strings.isNullOrEmpty(l3) ? " " + l3 : "")
        + (!Strings.isNullOrEmpty(l4) ? " " + l4 : "")
        + (!Strings.isNullOrEmpty(l5) ? " " + l5 : "");
  }

  @Override
  public String computeAddressStr(Address address) {
    return address.getFormattedFullName();
  }

  @Override
  public void autocompleteAddress(Address address) {
    String zip = address.getZip();
    if (zip == null) {
      return;
    }
    Country country = address.getCountry();

    City city = address.getCity();
    if (city == null) {
      List<City> cities = cityRepository.findByZipAndCountry(zip, country).fetch();
      city = cities.size() == 1 ? cities.get(0) : null;
      address.setCity(city);
    }

    if (appBaseService.getAppBase().getStoreStreets()) {
      List<Street> streets =
          streetRepository.all().filter("self.city = :city").bind("city", city).fetch();
      if (streets.size() == 1) {
        Street street = streets.get(0);
        address.setStreet(street);
        String name = street.getName();
        String num = address.getBuildingNumber();
        address.setStreetName(num != null ? num + " " + name : name);
      } else {
        address.setStreet(null);
        address.setStreetName(null);
      }
    }
  }

  @Override
  public String getZipCode(Address address) {
    //    if (address.getAddressL6() == null) {
    //      return null;
    //    }
    //
    //    Matcher matcher = ZIP_CODE_PATTERN.matcher(address.getAddressL6());
    //    if (matcher.find()) {
    //      return matcher.group();
    //    }
    return "null";
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  public void setFormattedFullName(Address address) throws AxelorException {
    AddressTemplate addressTemplate = address.getCountry().getAddressTemplate();
    String content = addressTemplate.getTemplateStr();

    try {
      Templates templates;
      if (addressTemplate.getEngineSelect() == AddressTemplateRepository.GROOVY_TEMPLATE) {
        templates = this.groovyTemplates;
      } else {
        templates = new StringTemplates(TEMPLATE_DELIMITER, TEMPLATE_DELIMITER);
      }

      Map<String, Object> templatesContext = Maps.newHashMap();
      Class<?> klass = EntityHelper.getEntityClass(address);
      Context context = new Context(Mapper.toMap(address), klass);
      templatesContext.put(klass.getSimpleName(), context.asType(klass));
      String fullFormattedString = templates.fromText(content).make(templatesContext).render();

      if (StringUtils.isBlank(fullFormattedString)) {
        throw new RuntimeException(
            String.format(
                I18n.get(BaseExceptionMessage.ADDRESS_TEMPLATE_ERROR), addressTemplate.getName()));
      }

      fullFormattedString = fullFormattedString.replaceAll(EMPTY_LINE_REMOVAL_REGEX, "");
      address.setFormattedFullName(fullFormattedString);

    } catch (Exception e) {
      LOG.error("Runtime Exception Address: {}", addressTemplate.getName());
      throw new AxelorException(e, TraceBackRepository.CATEGORY_CONFIGURATION_ERROR);
    }
  }

  @Override
  public Map<String, Map<String, Object>> getCountryAddressMetaFieldOnChangeAttrsMap(
      Address address) {
    Map<String, Map<String, Object>> attrsMap = new HashMap<>();

    if (ObjectUtils.notEmpty(
        address.getCountry().getAddressTemplate().getAddressTemplateLineList())) {
      List<AddressTemplateLine> addressTemplateLineList =
          address.getCountry().getAddressTemplate().getAddressTemplateLineList();
      addressAttrsService.addHiddenAndTitle(addressTemplateLineList, attrsMap);
      for (AddressTemplateLine addressTemplateLine : addressTemplateLineList) {
        addressAttrsService.addFieldUnhide(addressTemplateLine.getMetaField().getName(), attrsMap);
      }
    }

    return attrsMap;
  }
}

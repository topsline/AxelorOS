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

import static com.axelor.apps.base.db.repo.PartnerRepository.PARTNER_TYPE_INDIVIDUAL;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.ResponseMessageType;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Country;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PartnerAddress;
import com.axelor.apps.base.db.PartnerPriceList;
import com.axelor.apps.base.db.PriceList;
import com.axelor.apps.base.db.RegistrationNumberTemplate;
import com.axelor.apps.base.db.repo.PartnerAddressRepository;
import com.axelor.apps.base.db.repo.PartnerRepository;
import com.axelor.apps.base.db.repo.SequenceRepository;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.base.exceptions.BaseExceptionMessage;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.message.db.EmailAddress;
import com.axelor.message.db.repo.MessageRepository;
import com.axelor.utils.helpers.ComputeNameHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PartnerServiceImpl implements PartnerService {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected PartnerRepository partnerRepo;
  protected AppBaseService appBaseService;

  @Inject
  public PartnerServiceImpl(PartnerRepository partnerRepo, AppBaseService appBaseService) {
    this.partnerRepo = partnerRepo;
    this.appBaseService = appBaseService;
  }

  private Pattern phoneNumberPattern =
      Pattern.compile("^\\+?(?:[0-9]{2,3}(?:\\s|\\.)?){3,6}[0-9]{2,3}$");

  public Partner createPartner(
      String name,
      String firstName,
      String fixedPhone,
      String mobilePhone,
      EmailAddress emailAddress,
      Currency currency,
      Address deliveryAddress,
      Address mainInvoicingAddress,
      boolean createContact) {
    Partner partner = new Partner();

    partner.setName(name);
    partner.setFirstName(firstName);
    partner.setPartnerTypeSelect(PartnerRepository.PARTNER_TYPE_COMPANY);
    partner.setIsProspect(true);
    partner.setFixedPhone(fixedPhone);
    partner.setMobilePhone(mobilePhone);
    partner.setEmailAddress(emailAddress);
    partner.setCurrency(currency);
    this.setPartnerFullName(partner);

    if (createContact) {
      Partner contact =
          this.createContact(
              partner,
              name,
              firstName,
              fixedPhone,
              mobilePhone,
              emailAddress != null ? new EmailAddress(emailAddress.getAddress()) : null,
              mainInvoicingAddress);
      partner.addContactPartnerSetItem(contact);
    }

    if (deliveryAddress == mainInvoicingAddress) {
      addPartnerAddress(partner, mainInvoicingAddress, true, true, true);
    } else {
      addPartnerAddress(partner, deliveryAddress, true, false, true);
      addPartnerAddress(partner, mainInvoicingAddress, true, true, false);
    }

    return partner;
  }

  public Partner createContact(
      Partner partner,
      String name,
      String firstName,
      String fixedPhone,
      String mobilePhone,
      EmailAddress emailAddress,
      Address mainAddress) {

    Partner contact = new Partner();
    contact.setPartnerTypeSelect(PARTNER_TYPE_INDIVIDUAL);
    contact.setIsContact(true);
    contact.setName(name);
    contact.setFirstName(firstName);
    contact.setMainPartner(partner);
    contact.setEmailAddress(emailAddress);
    contact.setMainAddress(mainAddress);
    this.setPartnerFullName(contact);

    return partner;
  }

  @Override
  public void onSave(Partner partner) throws AxelorException {

    if (partner.getPartnerSeq() == null
        && appBaseService.getAppBase().getGeneratePartnerSequence()) {
      String seq =
          Beans.get(SequenceService.class)
              .getSequenceNumber(SequenceRepository.PARTNER, Partner.class, "partnerSeq", partner);
      if (seq == null) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(BaseExceptionMessage.PARTNER_1));
      }
      partner.setPartnerSeq(seq);
    }

    if (partner.getEmailAddress() != null) {
      long existEmailCount =
          partnerRepo
              .all()
              .filter(
                  "self.id != ?1 and self.emailAddress = ?2",
                  partner.getId(),
                  partner.getEmailAddress())
              .count();

      if (existEmailCount > 0) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_NO_UNIQUE_KEY,
            I18n.get(BaseExceptionMessage.PARTNER_EMAIL_EXIST));
      }
    }

    updatePartnerAddress(partner);

    if (partner.getPartnerTypeSelect() == PARTNER_TYPE_INDIVIDUAL) {
      partner.setContactPartnerSet(new HashSet<>());
    }

    if (!partner.getIsContact() && partner.getContactPartnerSet() != null) {
      for (Partner contact : partner.getContactPartnerSet()) {
        if (contact.getMainPartner() == null) {
          contact.setMainPartner(partner);
        }
      }
    }

    this.setPartnerFullName(partner);
    this.setCompanyStr(partner);

    if (!isRegistrationCodeValid(partner)) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_MISSING_FIELD,
          I18n.get(BaseExceptionMessage.PARTNER_INVALID_REGISTRATION_CODE));
    }
  }

  /**
   * Updates M2O and O2M fields of partner that manage partner addresses. This method ensures
   * consistency between these two fields.
   *
   * @param partner
   * @throws AxelorException
   */
  protected void updatePartnerAddress(Partner partner) throws AxelorException {
    Address address = partner.getMainAddress();

    if (!partner.getIsContact() && !partner.getIsEmployee()) {
      if (partner.getPartnerAddressList() != null) {
        partner.setMainAddress(checkDefaultAddress(partner));
      }
    } else if (address == null) {
      partner.removePartnerAddressListItem(
          JPA.all(PartnerAddress.class)
              .filter("self.partner = :partnerId AND self.isDefaultAddr = 't'")
              .bind("partnerId", partner.getId())
              .fetchOne());

    } else if (partner.getPartnerAddressList() != null
        && partner.getPartnerAddressList().stream()
            .map(PartnerAddress::getAddress)
            .noneMatch(address::equals)) {
      PartnerAddress mainAddress = new PartnerAddress();
      mainAddress.setAddress(address);
      mainAddress.setIsDefaultAddr(true);
      mainAddress.setIsDeliveryAddr(true);
      mainAddress.setIsInvoicingAddr(true);
      partner.addPartnerAddressListItem(mainAddress);
    }
  }

  /**
   * Ensures that there is exactly one default invoicing address and no more than one default
   * delivery address. If the partner address list is valid, returns the default invoicing address.
   *
   * @param partner
   * @throws AxelorException
   */
  protected Address checkDefaultAddress(Partner partner) throws AxelorException {
    List<PartnerAddress> partnerAddressList = partner.getPartnerAddressList();
    Address defaultInvoicingAddress = null;
    Address defaultDeliveryAddress = null;

    if (partnerAddressList != null) {
      for (PartnerAddress partnerAddress : partnerAddressList) {
        if (partnerAddress.getIsDefaultAddr() && partnerAddress.getIsInvoicingAddr()) {
          if (defaultInvoicingAddress != null) {
            throw new AxelorException(
                TraceBackRepository.CATEGORY_INCONSISTENCY,
                I18n.get(BaseExceptionMessage.ADDRESS_8));
          }
          defaultInvoicingAddress = partnerAddress.getAddress();
        }

        if (partnerAddress.getIsDefaultAddr() && partnerAddress.getIsDeliveryAddr()) {
          if (defaultDeliveryAddress != null) {
            throw new AxelorException(
                TraceBackRepository.CATEGORY_INCONSISTENCY,
                I18n.get(BaseExceptionMessage.ADDRESS_9));
          }
          defaultDeliveryAddress = partnerAddress.getAddress();
        }
      }
    }
    return defaultInvoicingAddress;
  }

  @Override
  public void setPartnerFullName(Partner partner) {
    partner.setSimpleFullName(this.computeSimpleFullName(partner));
    partner.setFullName(this.computeFullName(partner));
  }

  @Override
  public String computeFullName(Partner partner) {
    return ComputeNameHelper.computeFullName(
        partner.getFirstName(),
        partner.getName(),
        partner.getPartnerSeq(),
        String.valueOf(partner.getId()));
  }

  @Override
  public String computeSimpleFullName(Partner partner) {
    return ComputeNameHelper.computeSimpleFullName(
        partner.getFirstName(), partner.getName(), String.valueOf(partner.getId()));
  }

  @Override
  public Map<String, String> getSocialNetworkUrl(
      String name, String firstName, Integer typeSelect) {

    Map<String, String> urlMap = new HashMap<String, String>();
    if (typeSelect == 2) {
      name =
          firstName != null && name != null
              ? firstName + "+" + name
              : name == null ? firstName : name;
    }
    name = name == null ? "" : name;
    urlMap.put(
        "google",
        "<a class='fa fa-google' href='https://www.google.com/search?q="
            + name
            + "&gws_rd=cr"
            + "' target='_blank' />");
    urlMap.put(
        "linkedin",
        "<a class='fa fa-linkedin' href='https://www.linkedin.com/company/"
            + name
            + "' target='_blank' />");
    if (typeSelect == 2) {
      urlMap.put(
          "linkedin",
          "<a class='fa fa-linkedin' href='http://www.linkedin.com/pub/dir/"
              + name.replace("+", "/")
              + "' target='_blank' />");
    }

    return urlMap;
  }

  @Deprecated
  @Override
  public List<Long> findPartnerMails(Partner partner, int emailType) {
    List<Long> idList = new ArrayList<Long>();

    idList.addAll(this.findMailsFromPartner(partner, emailType));

    if (partner.getIsContact()) {
      return idList;
    }

    Set<Partner> contactSet = partner.getContactPartnerSet();
    if (contactSet != null && !contactSet.isEmpty()) {
      for (Partner contact : contactSet) {
        idList.addAll(this.findMailsFromPartner(contact, emailType));
      }
    }
    return idList;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Long> findMailsFromPartner(Partner partner, int emailType) {

    String query =
        String.format(
            "SELECT DISTINCT(email.id) FROM Message as email WHERE email.mediaTypeSelect = 2 "
                + "AND email IN (SELECT message FROM MultiRelated as related WHERE related.relatedToSelect = 'com.axelor.apps.base.db.Partner' AND related.relatedToSelectId = %s)",
            partner.getId());

    String emailAddress =
        (partner.getEmailAddress() != null) ? partner.getEmailAddress().getAddress() : null;
    if (emailAddress != null) {
      query +=
          " OR (:emailAddress IN ("
              + ((emailType == MessageRepository.TYPE_RECEIVED)
                  ? "email.fromEmailAddress.address"
                  : "SELECT em.address FROM EmailAddress em WHERE em member of email.toEmailAddressSet")
              + "))";
    } else {
      query += " AND email.typeSelect = " + emailType;
    }
    javax.persistence.Query q = JPA.em().createQuery(query);
    if (emailAddress != null) {
      q.setParameter("emailAddress", emailAddress);
    }

    return q.getResultList();
  }

  protected PartnerAddress createPartnerAddress(Address address, Boolean isDefault) {

    PartnerAddress partnerAddress = new PartnerAddress();
    partnerAddress.setAddress(address);
    partnerAddress.setIsDefaultAddr(isDefault);

    return partnerAddress;
  }

  @Transactional
  @Override
  public void resetDefaultAddress(Partner partner, String addrTypeQuery) {

    if (partner.getId() != null) {
      PartnerAddressRepository partnerAddressRepo = Beans.get(PartnerAddressRepository.class);
      PartnerAddress partnerAddress =
          partnerAddressRepo
              .all()
              .filter(
                  "self.partner.id = ? AND self.isDefaultAddr = true" + addrTypeQuery,
                  partner.getId())
              .fetchOne();
      if (partnerAddress != null) {
        partnerAddress.setIsDefaultAddr(false);
        partnerAddressRepo.save(partnerAddress);
      }
    }
  }

  @Override
  public Partner addPartnerAddress(
      Partner partner,
      Address address,
      Boolean isDefault,
      Boolean isInvoicing,
      Boolean isDelivery) {

    PartnerAddress partnerAddress = createPartnerAddress(address, isDefault);

    if (isDefault != null && isDefault) {
      LOG.debug("Add partner address : isDelivery = {}", isDelivery);
      LOG.debug("Add partner address : isInvoicing = {}", isInvoicing);

      String query =
          String.format(
              " AND self.isDeliveryAddr = %s AND self.isInvoicingAddr = %s",
              isDelivery, isInvoicing);
      resetDefaultAddress(partner, query);
    }

    partnerAddress.setIsInvoicingAddr(isInvoicing);
    partnerAddress.setIsDeliveryAddr(isDelivery);
    partnerAddress.setIsDefaultAddr(isDefault);
    partner.addPartnerAddressListItem(partnerAddress);

    return partner;
  }

  @Override
  public void addContactToPartner(Partner contact) {
    if (contact.getMainPartner() != null) {
      Partner partner = contact.getMainPartner();

      partner.addContactPartnerSetItem(contact);
      savePartner(partner);
    }
  }

  protected Address getAddress(Partner partner, String querySpecific, String queryComman) {

    if (partner != null) {
      PartnerAddressRepository partnerAddressRepo = Beans.get(PartnerAddressRepository.class);

      List<PartnerAddress> partnerAddressList =
          partnerAddressRepo.all().filter(querySpecific, partner.getId()).fetch();
      if (partnerAddressList.isEmpty()) {
        partnerAddressList = partnerAddressRepo.all().filter(queryComman, partner.getId()).fetch();

        if (partnerAddressList.isEmpty()) {
          partnerAddressList =
              partnerAddressRepo.all().filter("self.partner.id = ?1", partner.getId()).fetch();
        }
      }

      if (partnerAddressList.size() == 1) {
        return partnerAddressList.get(0).getAddress();
      }
      for (PartnerAddress partnerAddress : partnerAddressList) {
        if (partnerAddress.getIsDefaultAddr()) {
          return partnerAddress.getAddress();
        }
      }
    }

    return null;
  }

  @Override
  public Address getInvoicingAddress(Partner partner) {

    return getAddress(
        partner,
        "self.partner.id = ?1 AND self.isInvoicingAddr = true AND self.isDeliveryAddr = false AND self.isDefaultAddr = true",
        "self.partner.id = ?1 AND self.isInvoicingAddr = true");
  }

  @Override
  public Address getDeliveryAddress(Partner partner) {

    return getAddress(
        partner,
        "self.partner.id = ?1 AND self.isDeliveryAddr = true AND self.isInvoicingAddr = false AND self.isDefaultAddr = true",
        "self.partner.id = ?1 AND self.isDeliveryAddr = true");
  }

  @Override
  public Address getDefaultAddress(Partner partner) {

    return getAddress(
        partner,
        "self.partner.id = ?1 AND self.isDeliveryAddr = true AND self.isInvoicingAddr = true AND self.isDefaultAddr = true",
        "self.partner.id = ?1 AND self.isDefaultAddr = true");
  }

  @Transactional
  @Override
  public Partner savePartner(Partner partner) {
    return partnerRepo.save(partner);
  }

  @Override
  public BankDetails getDefaultBankDetails(Partner partner) {
    return partner.getBankDetailsList().stream()
        .filter(BankDetails::getIsDefault)
        .findFirst()
        .orElse(null);
  }

  @Transactional(rollbackOn = {Exception.class})
  @Override
  public String getSIRENNumber(Partner partner) throws AxelorException {
    char[] Str = new char[9];
    if (partner == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          I18n.get(BaseExceptionMessage.PARTNER_2),
          I18n.get(BaseExceptionMessage.EXCEPTION),
          "");
    }
    if (partner.getRegistrationCode() == null || partner.getRegistrationCode().isEmpty()) {
      throw new AxelorException(
          partner,
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          I18n.get(BaseExceptionMessage.PARTNER_2),
          I18n.get(BaseExceptionMessage.EXCEPTION),
          partner.getName());
    } else {
      String registrationCode = partner.getRegistrationCode();
      // remove whitespace in the registration code before using it
      registrationCode.replaceAll("\\s", "").getChars(0, 9, Str, 0);
    }

    return new String(Str);
  }

  @Transactional
  @Override
  public void convertToIndividualPartner(Partner partner) {
    partner.setIsContact(false);
    partner.setPartnerTypeSelect(PARTNER_TYPE_INDIVIDUAL);
    addPartnerAddress(partner, partner.getMainAddress(), true, false, false);
    partner.setMainAddress(null);
  }

  public boolean isThereDuplicatePartner(Partner partner) {
    return isThereDuplicatePartnerQuery(partner, false) != null;
  }

  /**
   * Search for the sale price list for the current date in the partner.
   *
   * @param partner
   * @return the sale price list for the partner null if no active price list has been found
   */
  @Override
  public PriceList getSalePriceList(Partner partner) {
    PartnerPriceList partnerPriceList = partner.getSalePartnerPriceList();
    if (partnerPriceList == null) {
      return null;
    }
    Set<PriceList> priceListSet = partnerPriceList.getPriceListSet();
    if (priceListSet == null) {
      return null;
    }
    LocalDate today =
        appBaseService.getTodayDate(
            Optional.ofNullable(AuthUtils.getUser()).map(User::getActiveCompany).orElse(null));
    List<PriceList> candidatePriceListList = new ArrayList<>();
    for (PriceList priceList : priceListSet) {
      LocalDate beginDate =
          priceList.getApplicationBeginDate() != null
              ? priceList.getApplicationBeginDate()
              : LocalDate.MIN;
      LocalDate endDate =
          priceList.getApplicationEndDate() != null
              ? priceList.getApplicationEndDate()
              : LocalDate.MAX;
      if (beginDate.compareTo(today) <= 0 && today.compareTo(endDate) <= 0) {
        candidatePriceListList.add(priceList);
      }
    }

    // if we found multiple price list, then the user will have to select one
    if (candidatePriceListList.size() == 1) {
      return candidatePriceListList.get(0);
    } else {
      return null;
    }
  }

  /**
   * Get the partner Localization.code. If null, return the default partner locale.
   *
   * @param partner
   * @return
   */
  @Override
  public String getPartnerLocale(Partner partner) {

    String locale = null;

    if (partner != null && partner.getLocalization() != null) {
      locale = partner.getLocalization().getCode();
    }
    if (!Strings.isNullOrEmpty(locale)) {
      return locale;
    }

    return appBaseService.getDefaultPartnerLocale();
  }

  /**
   * Normalize phone number.
   *
   * @param phoneNumber
   * @return
   */
  @Override
  public String normalizePhoneNumber(String phoneNumber) {
    return StringUtils.isBlank(phoneNumber) ? null : phoneNumber.replaceAll("\\s|\\.|-", "");
  }

  /**
   * Check phone number.
   *
   * @param phoneNumber
   * @return
   */
  @Override
  public boolean checkPhoneNumber(String phoneNumber) {
    return StringUtils.isBlank(phoneNumber)
        ? false
        : phoneNumberPattern.matcher(phoneNumber).matches();
  }

  /**
   * Get phone number field name.
   *
   * @param actionName
   * @return
   */
  @Override
  public String getPhoneNumberFieldName(String actionName) {
    Preconditions.checkNotNull(actionName, I18n.get("Action name cannot be null."));
    return actionName.substring(actionName.lastIndexOf('-') + 1);
  }

  @Override
  public void setCompanyStr(Partner partner) {
    partner.setCompanyStr(this.computeCompanyStr(partner));
  }

  @Override
  public String computeCompanyStr(Partner partner) {
    String companyStr = "";
    if (partner.getCompanySet() != null && partner.getCompanySet().size() > 0) {
      for (Company company : partner.getCompanySet()) {
        companyStr += company.getCode() + ",";
      }
      return companyStr.substring(0, companyStr.length() - 1);
    }
    return null;
  }

  public Partner isThereDuplicatePartnerInArchive(Partner partner) {
    return isThereDuplicatePartnerQuery(partner, true);
  }

  protected Partner isThereDuplicatePartnerQuery(Partner partner, boolean isInArchived) {
    String newName = this.computeSimpleFullName(partner);
    if (Strings.isNullOrEmpty(newName)) {
      return null;
    }
    Long partnerId = partner.getId();
    String filter =
        "lower(self.simpleFullName) = lower(:newName) "
            + "and self.partnerTypeSelect = :_partnerTypeSelect ";
    if (partnerId != null) {
      filter += "and self.id != :partnerId ";
    }
    if (isInArchived) {
      filter += "and self.archived = true ";
    } else {
      filter += "and ( self.archived != true OR self.archived is null ) ";
    }

    Query<Partner> partnerQuery =
        partnerRepo
            .all()
            .filter(filter)
            .bind("newName", newName)
            .bind("_partnerTypeSelect", partner.getPartnerTypeSelect());
    if (partnerId != null) {
      partnerQuery = partnerQuery.bind("partnerId", partnerId);
    }
    return partnerQuery.fetchOne();
  }

  @Override
  public boolean isRegistrationCodeValid(Partner partner) {
    String registrationCode = partner.getRegistrationCode();
    if (partner.getPartnerTypeSelect() != PartnerRepository.PARTNER_TYPE_COMPANY
        || Strings.isNullOrEmpty(registrationCode)) {
      return true;
    }

    registrationCode = registrationCode.replace(" ", "");
    if (partner.getMainAddress() != null
        && partner.getMainAddress().getAddressL7Country() != null) {
      Country businessCountry = partner.getMainAddress().getAddressL7Country();
      RegistrationNumberTemplate registrationNumberTemplate =
          businessCountry.getRegistrationNumberTemplate();

      if (registrationNumberTemplate != null) {
        return validateRegistrationCode(registrationCode, registrationNumberTemplate);
      }
    }
    return true;
  }

  private boolean validateRegistrationCode(
      String registrationCode, RegistrationNumberTemplate registrationNumberTemplate) {
    try {
      String origin = registrationNumberTemplate.getValidationMethodSelect();
      if (registrationNumberTemplate.getIsRequiredForCompanies()
          && StringUtils.isBlank(registrationCode)) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_MISSING_FIELD,
            I18n.get(BaseExceptionMessage.REGISTRATION_CODE_EMPTY_FOR_COMPANIES));
      }

      if (registrationCode.length() != registrationNumberTemplate.getRequiredSize()) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_MISSING_FIELD,
            I18n.get(BaseExceptionMessage.PARTNER_INVALID_REGISTRATION_CODE));
      }
      if (origin != null) {
        Class<? extends RegistrationNumberValidation> clazz =
            Class.forName(origin).asSubclass(RegistrationNumberValidation.class);
        boolean isValidRegistrationCode =
            Beans.get(clazz).computeRegistrationCodeValidity(registrationCode);
        if (!isValidRegistrationCode) {
          throw new AxelorException(
              TraceBackRepository.CATEGORY_MISSING_FIELD,
              I18n.get(BaseExceptionMessage.PARTNER_INVALID_REGISTRATION_CODE));
        }
      }
    } catch (ClassNotFoundException | AxelorException e) {
      TraceBackService.trace(e, String.valueOf(ResponseMessageType.ERROR));
      return false;
    }

    return true;
  }

  @Override
  public String getRegistrationCodeTitleFromTemplate(Partner partner) {
    if (partner.getMainAddress() != null
        && partner.getMainAddress().getAddressL7Country() != null) {
      Country businessCountry = partner.getMainAddress().getAddressL7Country();
      RegistrationNumberTemplate registrationNumberTemplate =
          businessCountry.getRegistrationNumberTemplate();
      if (registrationNumberTemplate != null
          && !StringUtils.isBlank(registrationNumberTemplate.getTitleToDisplay())) {
        return registrationNumberTemplate.getTitleToDisplay();
      }
    }
    return null;
  }

  @Override
  public Map<String, Map<String, Object>> getRegistrationCodeValidationAttrs(Partner partner) {
    if (partner.getMainAddress() != null
        && partner.getMainAddress().getAddressL7Country() != null) {
      Country businessCountry = partner.getMainAddress().getAddressL7Country();
      RegistrationNumberTemplate registrationNumberTemplate =
          businessCountry.getRegistrationNumberTemplate();

      if (registrationNumberTemplate != null) {
        try {
          String origin = registrationNumberTemplate.getValidationMethodSelect();
          if (origin != null) {
            Class<? extends RegistrationNumberValidation> clazz =
                (Class<? extends RegistrationNumberValidation>) Class.forName(origin);
            return Beans.get(clazz).getRegistrationCodeValidationAttrs(partner);
          }
        } catch (ClassNotFoundException e) {
          TraceBackService.trace(e, String.valueOf(ResponseMessageType.ERROR));
        }
      }
    }
    return null;
  }

  @Override
  public Map<String, Map<String, Object>> getPartnerTypeSelectAttrs(Partner partner) {
    Map<String, Map<String, Object>> attrsMap = new HashMap<>();
    attrsMap.put("siren", new HashMap<>());
    attrsMap.put("nic", new HashMap<>());
    attrsMap
        .get("siren")
        .put("hidden", partner.getPartnerTypeSelect() == PartnerRepository.PARTNER_TYPE_INDIVIDUAL);
    attrsMap
        .get("nic")
        .put("hidden", partner.getPartnerTypeSelect() == PartnerRepository.PARTNER_TYPE_INDIVIDUAL);
    return attrsMap;
  }
}

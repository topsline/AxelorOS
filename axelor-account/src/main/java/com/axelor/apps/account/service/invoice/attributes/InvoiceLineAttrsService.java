package com.axelor.apps.account.service.invoice.attributes;

import com.axelor.apps.account.db.Invoice;
import java.util.Map;

public interface InvoiceLineAttrsService {

  void setInTaxPriceScale(
      Invoice invoice, Map<String, Map<String, Object>> attrsMap, String prefix);

  void setExTaxTotalScale(
      Invoice invoice, Map<String, Map<String, Object>> attrsMap, String prefix);

  void setInTaxTotalScale(
      Invoice invoice, Map<String, Map<String, Object>> attrsMap, String prefix);

  void setCompanyExTaxTotalScale(Invoice invoice, Map<String, Map<String, Object>> attrsMap);

  void setCompanyInTaxTotalScale(Invoice invoice, Map<String, Map<String, Object>> attrsMap);
}

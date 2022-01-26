package com.axelor.apps.sale.service.saleorder;

import java.util.List;
import java.util.Objects;

import com.axelor.apps.account.db.FiscalPosition;
import com.axelor.apps.account.db.TaxNumber;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PriceList;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.team.db.Team;

public class SaleOrderMergingServiceImpl implements SaleOrderMergingService {

	protected static class CommonFieldsImpl implements CommonFields {
		
		private Company commonCompany = null;
		private Currency commonCurrency = null;
		private Partner commonClientPartner = null;
		private TaxNumber commonTaxNumber = null;
		private FiscalPosition commonFiscalPosition = null;
		private Team commonTeam = null;
		private Partner commonContactPartner = null;
		private PriceList commonPriceList = null;
		
		@Override
		public Company getCommonCompany() {
			return commonCompany;
		}

		@Override
		public void setCommonCompany(Company commonCompany) {
			this.commonCompany = commonCompany;
			
		}

		@Override
		public Currency getCommonCurrency() {
			return commonCurrency;
		}

		@Override
		public void setCommonCurrency(Currency commonCurrency) {
			this.commonCurrency = commonCurrency;
			
		}

		@Override
		public Partner getCommonClientPartner() {
			return this.commonClientPartner;
		}

		@Override
		public void setCommonClientPartner(Partner commonClientPartner) {
			this.commonClientPartner = commonClientPartner;
			
		}

		@Override
		public TaxNumber getCommonTaxNumber() {
			return commonTaxNumber;
		}

		@Override
		public void setCommonTaxNumber(TaxNumber commonTaxNumber) {
			this.commonTaxNumber = commonTaxNumber;
		}

		@Override
		public FiscalPosition getCommonFiscalPosition() {
			return commonFiscalPosition;
		}

		@Override
		public void setCommonFiscalPosition(FiscalPosition commonFiscalPosition) {
			this.commonFiscalPosition = commonFiscalPosition;
			
		}

		@Override
		public Team getCommonTeam() {
			return commonTeam;
		}

		@Override
		public void setCommonTeam(Team commonTeam) {
			this.commonTeam = commonTeam;
			
		}

		@Override
		public Partner getCommonContactPartner() {
			return commonContactPartner;
		}

		@Override
		public void setCommonContactPartner(Partner commonContactPartner) {
			this.commonContactPartner = commonContactPartner;
			
		}

		@Override
		public PriceList getCommonPriceList() {
			return commonPriceList;
		}

		@Override
		public void setCommonPriceList(PriceList commonPriceList) {
			this.commonPriceList = commonPriceList;
			
		}
		
	}
	
	protected static class ChecksImpl implements Checks {
		
		private boolean existCurrencyDiff = false;
		private boolean existCompanyDiff = false;
		private boolean existClientPartnerDiff = false;
		private boolean existTaxNumberDiff = false;
		private boolean existFiscalPositionDiff = false;
		private boolean existTeamDiff = false;
		private boolean existContactPartnerDiff = false;
		private boolean existPriceListDiff = false;

		@Override
		public boolean isExistCurrencyDiff() {
			return existCurrencyDiff;
		}

		@Override
		public void setExistCurrencyDiff(boolean existCurrencyDiff) {
			this.existCurrencyDiff = existCurrencyDiff;
			
		}

		@Override
		public boolean isExistCompanyDiff() {
			return existCompanyDiff;
		}

		@Override
		public void setExistCompanyDiff(boolean existCompanyDiff) {
			this.existCompanyDiff = existCompanyDiff;
			
		}

		@Override
		public boolean isExistClientPartnerDiff() {
			return existClientPartnerDiff;
		}

		@Override
		public void setExistClientPartnerDiff(boolean existClientPartnerDiff) {
			this.existClientPartnerDiff = existClientPartnerDiff;
			
		}

		@Override
		public boolean isExistTaxNumberDiff() {
			return existTaxNumberDiff;
		}

		@Override
		public void setExistTaxNumberDiff(boolean existTaxNumberDiff) {
			this.existTaxNumberDiff = existTaxNumberDiff;
			
		}

		@Override
		public boolean isExistFiscalPositionDiff() {
			return existFiscalPositionDiff;
		}

		@Override
		public void setExistFiscalPositionDiff(boolean existFiscalPositionDiff) {
			this.existFiscalPositionDiff = existFiscalPositionDiff;
			
		}

		@Override
		public boolean isExistTeamDiff() {
			return existTeamDiff;
		}

		@Override
		public void setExistTeamDiff(boolean existTeamDiff) {
			this.existTeamDiff = existTeamDiff;
			
		}

		@Override
		public boolean isExistContactPartnerDiff() {
			return existContactPartnerDiff;
		}

		@Override
		public void setExistContactPartnerDiff(boolean existContactPartnerDiff) {
			this.existContactPartnerDiff = existContactPartnerDiff;
			
		}

		@Override
		public boolean isExistPriceListDiff() {
			return existPriceListDiff;
		}

		@Override
		public void setExistPriceListDiff(boolean existPriceListDiff) {
			this.existPriceListDiff = existPriceListDiff;
			
		}
		
	}
	
	protected static class SaleOrderMergingResultImpl implements SaleOrderMergingResult {
		
		private SaleOrder saleOrder;
		private boolean isConfirmationNeeded;
		private final CommonFieldsImpl commonFields;
		private final ChecksImpl checks;
		
		public SaleOrderMergingResultImpl() {
			this.saleOrder = null;
			this.isConfirmationNeeded = false;
			this.commonFields = new CommonFieldsImpl();
			this.checks = new ChecksImpl();
		}

		public SaleOrder getSaleOrder() {
			return saleOrder;
		}

		public void setSaleOrder(SaleOrder saleOrder) {
			this.saleOrder = saleOrder;
		}

		@Override
		public void needConfirmation() {
			this.isConfirmationNeeded = true;
			
		}

		@Override
		public boolean isConfirmationNeeded() {
			return isConfirmationNeeded;
		}
	}
	
	@Override
	public SaleOrderMergingResult create() {
		return new SaleOrderMergingResultImpl();
	}

	@Override
	public CommonFields getCommonFields(SaleOrderMergingResult result) {
		return ((SaleOrderMergingResultImpl) result).commonFields;
	}

	@Override
	public Checks getChecks(SaleOrderMergingResult result) {
		return ((SaleOrderMergingResultImpl) result).checks;
	}

	@Override
	public SaleOrderMergingResult mergeSaleOrders(List<SaleOrder> saleOrdersToMerge) throws AxelorException {
		Objects.requireNonNull(saleOrdersToMerge);
		SaleOrderMergingResult result = create();
		
		if (saleOrdersToMerge.isEmpty()) {
			throw new AxelorException(TraceBackRepository.CATEGORY_INCONSISTENCY, "List of sale orders to merge is empty");
		}
		
		SaleOrder firstSaleOrder = saleOrdersToMerge.get(0);
		fillCommonFields(firstSaleOrder, result);
		saleOrdersToMerge.stream().skip(1).forEach(saleOrder -> {
			updateDiffsCommonFields(saleOrder, result);
		});
		return result;
	}

	protected void updateDiffsCommonFields(SaleOrder saleOrder, SaleOrderMergingResult result) {
		CommonFields commonFields = getCommonFields(result);
		Checks checks = getChecks(result);
		if (commonFields.getCommonCurrency() != null && !commonFields.getCommonCurrency().equals(saleOrder.getCurrency())) {
			commonFields.setCommonCurrency(null);
			checks.setExistCurrencyDiff(true);
		}
		if (commonFields.getCommonClientPartner() != null && !commonFields.getCommonClientPartner().equals(saleOrder.getClientPartner())) {
			commonFields.setCommonClientPartner(null);
			checks.setExistClientPartnerDiff(true);
		}
		if (commonFields.getCommonCompany() != null && !commonFields.getCommonCompany().equals(saleOrder.getCompany())) {
			commonFields.setCommonCompany(null);
			checks.setExistCompanyDiff(true);
		}
		if (commonFields.getCommonContactPartner() != null && !commonFields.getCommonContactPartner().equals(saleOrder.getContactPartner())) {
			commonFields.setCommonContactPartner(null);
			checks.setExistContactPartnerDiff(true);
		}
		if (commonFields.getCommonTeam() != null && !commonFields.getCommonTeam().equals(saleOrder.getTeam())) {
			commonFields.setCommonTeam(null);
			checks.setExistTeamDiff(true);
		}
		if (commonFields.getCommonPriceList() != null && !commonFields.getCommonPriceList().equals(saleOrder.getPriceList())) {
			commonFields.setCommonPriceList(null);
			checks.setExistPriceListDiff(true);
		}
		//TaxNumber can be null
		if ((!checks.isExistFiscalPositionDiff() && commonFields.getCommonFiscalPosition() == null && saleOrder.getFiscalPosition() != null)
				|| (commonFields.getCommonFiscalPosition() != null )) {
			commonFields.setCommonFiscalPosition(null);
			checks.setExistFiscalPositionDiff(true);
		}
		//TODO:
		
	}

	protected void fillCommonFields(SaleOrder firstSaleOrder, SaleOrderMergingResult result) {
		CommonFields commonFields = getCommonFields(result);
		commonFields.setCommonCompany(firstSaleOrder.getCompany());
		commonFields.setCommonCurrency(firstSaleOrder.getCurrency());
		commonFields.setCommonContactPartner(firstSaleOrder.getContactPartner());
		commonFields.setCommonFiscalPosition(firstSaleOrder.getFiscalPosition());
		commonFields.setCommonPriceList(firstSaleOrder.getPriceList());
		commonFields.setCommonTaxNumber(firstSaleOrder.getTaxNumber());
		commonFields.setCommonTeam(firstSaleOrder.getTeam());
		commonFields.setCommonClientPartner(firstSaleOrder.getClientPartner());
		
	}

}

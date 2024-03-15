/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.apps.production.service;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.db.repo.CompanyRepository;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.ProductCompanyService;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.production.db.repo.ManufOrderRepository;
import com.axelor.apps.production.service.app.AppProductionService;
import com.axelor.apps.production.service.manuforder.ManufOrderService;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.TrackingNumberConfiguration;
import com.axelor.apps.stock.db.repo.StockLocationLineRepository;
import com.axelor.apps.stock.db.repo.StockLocationRepository;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.apps.stock.service.StockLocationLineService;
import com.axelor.apps.stock.service.StockLocationService;
import com.axelor.apps.supplychain.service.ProductStockLocationServiceImpl;
import com.axelor.apps.supplychain.service.StockLocationServiceSupplychain;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import com.axelor.utils.helpers.StringHelper;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProductionProductStockLocationServiceImpl extends ProductStockLocationServiceImpl
    implements ProductionProductStockLocationService {

  protected AppProductionService appProductionService;
  protected ManufOrderService manufOrderService;
  protected StockMoveLineRepository stockMoveLineRepository;

  protected ProductCompanyService productCompanyService;

  @Inject
  public ProductionProductStockLocationServiceImpl(
      UnitConversionService unitConversionService,
      AppSupplychainService appSupplychainService,
      ProductRepository productRepository,
      CompanyRepository companyRepository,
      StockLocationRepository stockLocationRepository,
      StockLocationService stockLocationService,
      StockLocationServiceSupplychain stockLocationServiceSupplychain,
      StockLocationLineService stockLocationLineService,
      StockLocationLineRepository stockLocationLineRepository,
      AppProductionService appProductionService,
      ManufOrderService manufOrderService,
      StockMoveLineRepository stockMoveLineRepository,
      AppBaseService appBaseService,
      ProductCompanyService productCompanyService) {
    super(
        unitConversionService,
        appSupplychainService,
        productRepository,
        companyRepository,
        stockLocationRepository,
        stockLocationService,
        stockLocationServiceSupplychain,
        stockLocationLineService,
        stockLocationLineRepository,
        appBaseService);
    this.appProductionService = appProductionService;
    this.manufOrderService = manufOrderService;
    this.stockMoveLineRepository = stockMoveLineRepository;
    this.productCompanyService = productCompanyService;
  }

  @Override
  public Map<String, Object> computeIndicators(Long productId, Long companyId, Long stockLocationId)
      throws AxelorException {
    Map<String, Object> map = super.computeIndicators(productId, companyId, stockLocationId);
    Product product = productRepository.find(productId);
    Company company = companyRepository.find(companyId);
    StockLocation stockLocation = stockLocationRepository.find(stockLocationId);
    int scale = appBaseService.getNbDecimalDigitForQty();
    BigDecimal consumeManufOrderQty =
        this.getConsumeManufOrderQty(product, company, stockLocation)
            .setScale(scale, RoundingMode.HALF_UP);
    BigDecimal availableQty =
        (BigDecimal)
            map.getOrDefault(
                "$availableQty", BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP));
    map.put(
        "$buildingQty",
        this.getBuildingQty(product, company, stockLocation).setScale(scale, RoundingMode.HALF_UP));
    map.put("$consumeManufOrderQty", consumeManufOrderQty);
    map.put(
        "$missingManufOrderQty",
        BigDecimal.ZERO
            .max(consumeManufOrderQty.subtract(availableQty))
            .setScale(scale, RoundingMode.HALF_UP));
    return map;
  }

  protected BigDecimal getBuildingQty(Product product, Company company, StockLocation stockLocation)
      throws AxelorException {
    if (product == null || product.getUnit() == null) {
      return BigDecimal.ZERO;
    }
    Long companyId = 0L;
    Long stockLocationId = 0L;
    if (company != null) {
      companyId = company.getId();
      if (stockLocation != null) {
        stockLocationId = stockLocation.getId();
      }
    }

    String query = getBuildingQtyForAProduct(product.getId(), companyId, stockLocationId);
    List<StockMoveLine> stockMoveLineList = stockMoveLineRepository.all().filter(query).fetch();

    BigDecimal sumBuildingQty = BigDecimal.ZERO;
    if (!stockMoveLineList.isEmpty()) {

      Unit unitConversion = product.getUnit();
      for (StockMoveLine stockMoveLine : stockMoveLineList) {
        BigDecimal productBuildingQty = stockMoveLine.getRealQty();
        unitConversionService.convert(
            stockMoveLine.getUnit(),
            unitConversion,
            productBuildingQty,
            productBuildingQty.scale(),
            product);
        sumBuildingQty = sumBuildingQty.add(productBuildingQty);
      }
    }
    return sumBuildingQty;
  }

  protected BigDecimal getConsumeManufOrderQty(
      Product product, Company company, StockLocation stockLocation) throws AxelorException {
    if (product == null || product.getUnit() == null) {
      return BigDecimal.ZERO;
    }
    Long companyId = 0L;
    Long stockLocationId = 0L;
    if (company != null) {
      companyId = company.getId();
      if (stockLocation != null) {
        stockLocationId = stockLocation.getId();
      }
    }
    String query = getConsumeAndMissingQtyForAProduct(product.getId(), companyId, stockLocationId);
    List<StockMoveLine> stockMoveLineList = stockMoveLineRepository.all().filter(query).fetch();

    BigDecimal sumConsumeManufOrderQty = BigDecimal.ZERO;
    if (!stockMoveLineList.isEmpty()) {
      Unit unitConversion = product.getUnit();
      for (StockMoveLine stockMoveLine : stockMoveLineList) {
        BigDecimal productConsumeManufOrderQty = stockMoveLine.getRealQty();
        unitConversionService.convert(
            stockMoveLine.getUnit(),
            unitConversion,
            productConsumeManufOrderQty,
            productConsumeManufOrderQty.scale(),
            product);
        sumConsumeManufOrderQty = sumConsumeManufOrderQty.add(productConsumeManufOrderQty);
      }
    }
    return sumConsumeManufOrderQty;
  }

  protected BigDecimal getMissingManufOrderQty(
      Product product, Company company, StockLocation stockLocation) throws AxelorException {
    if (product == null || product.getUnit() == null) {
      return BigDecimal.ZERO;
    }
    Long companyId = 0L;
    Long stockLocationId = 0L;
    if (company != null) {
      companyId = company.getId();
      if (stockLocation != null) {
        stockLocationId = stockLocation.getId();
      }
    }
    String query = getConsumeAndMissingQtyForAProduct(product.getId(), companyId, stockLocationId);
    List<StockMoveLine> stockMoveLineList = stockMoveLineRepository.all().filter(query).fetch();

    BigDecimal sumMissingManufOrderQty = BigDecimal.ZERO;
    if (!stockMoveLineList.isEmpty()) {
      Unit unitConversion = product.getUnit();
      for (StockMoveLine stockMoveLine : stockMoveLineList) {
        BigDecimal productMissingManufOrderQty = getMissingQtyOfStockMoveLine(stockMoveLine);
        unitConversionService.convert(
            stockMoveLine.getUnit(),
            unitConversion,
            productMissingManufOrderQty,
            productMissingManufOrderQty.scale(),
            product);
        sumMissingManufOrderQty = sumMissingManufOrderQty.add(productMissingManufOrderQty);
      }
    }
    return sumMissingManufOrderQty;
  }

  protected BigDecimal getMissingQtyOfStockMoveLine(StockMoveLine stockMoveLine)
      throws AxelorException {
    if (stockMoveLine.getProduct() != null) {
      BigDecimal availableQty = stockMoveLine.getAvailableQty();
      BigDecimal availableQtyForProduct = stockMoveLine.getAvailableQtyForProduct();
      BigDecimal realQty = stockMoveLine.getRealQty();

      if (availableQty.compareTo(realQty) >= 0 || !stockMoveLine.getProduct().getStockManaged()) {
        return BigDecimal.ZERO;
      } else if (availableQtyForProduct.compareTo(realQty) >= 0) {
        return BigDecimal.ZERO;
      } else if (availableQty.compareTo(realQty) < 0
          && availableQtyForProduct.compareTo(realQty) < 0) {

        TrackingNumberConfiguration trackingNumberConfiguration =
            (TrackingNumberConfiguration)
                productCompanyService.get(
                    stockMoveLine.getProduct(),
                    "trackingNumberConfiguration",
                    Optional.ofNullable(stockMoveLine.getStockMove())
                        .map(StockMove::getCompany)
                        .orElse(null));

        if (trackingNumberConfiguration != null) {
          return availableQtyForProduct.subtract(realQty);
        } else {
          return availableQty.subtract(realQty);
        }
      }
    }
    return BigDecimal.ZERO;
  }

  @Override
  public String getConsumeAndMissingQtyForAProduct(
      Long productId, Long companyId, Long stockLocationId) {
    List<Integer> statusList = getMOFiltersOnProductionConfig();
    String statusListQuery =
        statusList.stream().map(String::valueOf).collect(Collectors.joining(","));
    String query =
        "self.product.id = "
            + productId
            + " AND self.stockMove.statusSelect = "
            + StockMoveRepository.STATUS_PLANNED
            + " AND self.fromStockLocation.typeSelect != "
            + StockLocationRepository.TYPE_VIRTUAL
            + " AND ( (self.consumedManufOrder IS NOT NULL AND self.consumedManufOrder.statusSelect IN ("
            + statusListQuery
            + "))"
            + " OR (self.consumedOperationOrder IS NOT NULL AND self.consumedOperationOrder.statusSelect IN ( "
            + statusListQuery
            + ") ) ) ";
    if (companyId != 0L) {
      query += " AND self.stockMove.company.id = " + companyId;
      if (stockLocationId != 0L) {
        if (stockLocationId != 0L) {
          StockLocation stockLocation =
              Beans.get(StockLocationRepository.class).find(stockLocationId);
          List<StockLocation> stockLocationList =
              Beans.get(StockLocationService.class)
                  .getAllLocationAndSubLocation(stockLocation, false);
          if (!stockLocationList.isEmpty()
              && stockLocation.getCompany().getId().equals(companyId)) {
            query +=
                " AND self.fromStockLocation.id IN ("
                    + StringHelper.getIdListString(stockLocationList)
                    + ") ";
          }
        }
      }
    }

    return query;
  }

  @Override
  public String getBuildingQtyForAProduct(Long productId, Long companyId, Long stockLocationId) {
    List<Integer> statusList = getMOFiltersOnProductionConfig();
    String statusListQuery =
        statusList.stream().map(String::valueOf).collect(Collectors.joining(","));
    String query =
        "self.product.id = "
            + productId
            + " AND self.stockMove.statusSelect = "
            + StockMoveRepository.STATUS_PLANNED
            + " AND self.stockMove.toStockLocation.typeSelect != "
            + StockLocationRepository.TYPE_VIRTUAL
            + " AND self.producedManufOrder IS NOT NULL "
            + " AND self.producedManufOrder.statusSelect IN ( "
            + statusListQuery
            + " )";
    if (companyId != 0L) {
      query += "AND self.stockMove.company.id = " + companyId;
      if (stockLocationId != 0L) {
        StockLocation stockLocation =
            Beans.get(StockLocationRepository.class).find(stockLocationId);
        List<StockLocation> stockLocationList =
            Beans.get(StockLocationService.class)
                .getAllLocationAndSubLocation(stockLocation, false);
        if (!stockLocationList.isEmpty() && stockLocation.getCompany().getId().equals(companyId)) {
          query +=
              " AND self.stockMove.toStockLocation.id IN ("
                  + StringHelper.getIdListString(stockLocationList)
                  + ") ";
        }
      }
    }

    return query;
  }

  protected List<Integer> getMOFiltersOnProductionConfig() {
    List<Integer> statusList = new ArrayList<>();
    statusList.add(ManufOrderRepository.STATUS_IN_PROGRESS);
    statusList.add(ManufOrderRepository.STATUS_STANDBY);
    String status = appProductionService.getAppProduction().getmOFilterOnStockDetailStatusSelect();
    if (!StringUtils.isBlank(status)) {
      statusList = StringHelper.getIntegerList(status);
    }
    return statusList;
  }
}

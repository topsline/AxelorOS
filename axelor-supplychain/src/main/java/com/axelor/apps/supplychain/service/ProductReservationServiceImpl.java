package com.axelor.apps.supplychain.service;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.stock.db.StockLocationLine;
import com.axelor.apps.stock.utils.StockLocationUtilsService;
import com.axelor.apps.supplychain.db.ProductReservation;
import com.axelor.apps.supplychain.db.repo.ProductReservationRepository;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductReservationServiceImpl implements ProductReservationService {

  protected ProductReservationRepository productReservationRepository;
  protected StockLocationUtilsService stockLocationUtilsService;

  @Inject
  public ProductReservationServiceImpl(
      ProductReservationRepository productReservationRepository,
      StockLocationUtilsService stockLocationUtilsService) {
    this.productReservationRepository = productReservationRepository;
    this.stockLocationUtilsService = stockLocationUtilsService;
  }

  @Override
  @Transactional
  public void cancelReservation(ProductReservation productReservation) {
    productReservation.setStatus(ProductReservationRepository.PRODUCT_RESERVATION_STATUS_CANCELED);
    productReservationRepository.save(productReservation);
  }

  @Override
  @Transactional
  public ProductReservation updateStatus(ProductReservation productReservation)
      throws AxelorException {

    if (productReservation.getTypeSelect()
        == ProductReservationRepository.TYPE_PRODUCT_RESERVATION_RESERVATION) {
      updateReservationStatus(productReservation);

    } else if (productReservation.getTypeSelect()
        == ProductReservationRepository.TYPE_PRODUCT_RESERVATION_ALLOCATION) {
      boolean isTracked =
          (productReservation.getProduct().getTrackingNumberConfiguration() != null);
      BigDecimal alreadyAllocatedQty =
          productReservationRepository
              .all()
              .filter(
                  "self.typeSelect = :typeSelect AND self.status = :status"
                      + (productReservation.getStockLocation() != null
                          ? " AND self.stockLocation = :stockLocation"
                          : "")
                      + (isTracked
                          ? " AND self.trackingNumber = :trackingNumber"
                          : " AND self.product = :product"))
              .bind("product", productReservation.getProduct())
              .bind("typeSelect", productReservation.getTypeSelect())
              .bind("status", ProductReservationRepository.PRODUCT_RESERVATION_STATUS_IN_PROGRESS)
              .bind("stockLocation", productReservation.getStockLocation())
              .fetchStream()
              .map(ProductReservation::getQty)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      Optional<StockLocationLine> optionalLine =
          productReservation.getStockLocation().getDetailsStockLocationLineList().stream()
              .filter(it -> it.getTrackingNumber().equals(productReservation.getTrackingNumber()))
              .findFirst();
      BigDecimal realQty =
          optionalLine.map(StockLocationLine::getCurrentQty).orElse(BigDecimal.ZERO);
      BigDecimal availableQty =
          realQty.subtract(alreadyAllocatedQty).setScale(2, RoundingMode.HALF_UP);

      if (productReservation.getQty().compareTo(availableQty) <= 0) {
        productReservation.setStatus(
            ProductReservationRepository.PRODUCT_RESERVATION_STATUS_IN_PROGRESS);
      }
    }
    return productReservation;
  }

  protected BigDecimal getRealQtyForProductReservation(ProductReservation productReservation)
      throws AxelorException {
    BigDecimal realQty = BigDecimal.ZERO;
    if (productReservation.getProduct() != null) {
      List<Long> stockLocationIds = new ArrayList<Long>();
      if (productReservation.getStockLocation() != null) {
        stockLocationIds.add(productReservation.getStockLocation().getId());
      }
      realQty =
          stockLocationUtilsService.getRealQtyOfProductInStockLocations(
              productReservation.getProduct().getId(), stockLocationIds, null);
    }
    return realQty;
  }

  protected void updateReservationStatus(ProductReservation productReservation) {
    int reservationTypeSelect = Integer.parseInt(productReservation.getTypeOfUsageTypeSelect());

    BigDecimal productReservationQty = productReservation.getQty();
    if (reservationTypeSelect
            == ProductReservationRepository.PRODUCT_TYPE_OF_USAGE_TO_SELL_ON_MOVE_LINE
        && productReservationQty.compareTo(productReservation.getOriginSaleOrderLine().getQty())
            <= 0) {
      productReservation.setStatus(
          ProductReservationRepository.PRODUCT_RESERVATION_STATUS_IN_PROGRESS);
    }
  }
}

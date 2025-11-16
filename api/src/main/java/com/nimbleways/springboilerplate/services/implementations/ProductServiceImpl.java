package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public ProductServiceImpl(ProductRepository productRepository, NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void processProduct(Product product) {
        logger.debug("Processing product: {} of type: {}", product.getName(), product.getType());
        ProductType productType = product.getType();
        
        switch (productType) {
            case NORMAL:
                processNormalProduct(product);
                break;
            case SEASONAL:
                processSeasonalProduct(product);
                break;
            case EXPIRABLE:
                processExpirableProduct(product);
                break;
            default:
                logger.warn("Unknown product type: {} for product: {}", productType, product.getName());
        }
    }

    void notifyDelay(int leadTime, Product product) {
        logger.debug("Notifying delay for product: {} with lead time: {}", product.getName(), leadTime);
        product.setLeadTime(leadTime);
        productRepository.save(product);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }

    void processNormalProduct(Product product) {
        logger.debug("Processing normal product: {}", product.getName());
        if (product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            int leadTime = product.getLeadTime();
            if (leadTime > 0) {
                notifyDelay(leadTime, product);
            }
        }
    }

    void processSeasonalProduct(Product product) {
        logger.debug("Processing seasonal product: {}", product.getName());
        LocalDate now = LocalDate.now();
        if (now.isAfter(product.getSeasonStartDate()) 
                && now.isBefore(product.getSeasonEndDate())
                && product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            handleSeasonalProduct(product);
        }
    }

    void processExpirableProduct(Product product) {
        logger.debug("Processing expirable product: {}", product.getName());
        LocalDate now = LocalDate.now();
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(now)) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            handleExpiredProduct(product);
        }
    }

    private void handleSeasonalProduct(Product product) {
        logger.debug("Handling seasonal product: {}", product.getName());
        LocalDate now = LocalDate.now();
        if (now.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate())) {
            notificationService.sendOutOfStockNotification(product.getName());
            product.setAvailable(0);
            productRepository.save(product);
        } else if (product.getSeasonStartDate().isAfter(now)) {
            notificationService.sendOutOfStockNotification(product.getName());
            productRepository.save(product);
        } else {
            notifyDelay(product.getLeadTime(), product);
        }
    }

    private void handleExpiredProduct(Product product) {
        logger.debug("Handling expired product: {}", product.getName());
        LocalDate now = LocalDate.now();
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(now)) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.setAvailable(0);
            productRepository.save(product);
        }
    }
}


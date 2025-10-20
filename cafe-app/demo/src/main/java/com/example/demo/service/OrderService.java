package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.model.Customer;
import com.example.demo.model.MenuItem;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.MenuRepository;
import com.example.demo.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final CustomerRepository customerRepository;

    // ID mặc định cho "Khách vãng lai". Đảm bảo có một khách hàng với ID này trong CSDL.
    private static final Long GUEST_CUSTOMER_ID = 1L;

    public OrderService(OrderRepository orderRepository,
                        MenuRepository menuRepository,
                        CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.menuRepository = menuRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Tạo hoặc cập nhật một đơn hàng, xử lý thanh toán và hoàn tất.
     */
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Order createOrUpdateOrder(OrderRequestDto orderRequest) {
        if (orderRequest == null || orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng không được để trống.");
        }

        Order order;
        Long customerId = orderRequest.getCustomerId();
        String tableNumber = orderRequest.getTableNumber();

        // Tìm đơn hàng đang chờ hiện có
        Optional<Order> existingPendingOrderOpt = findExistingPendingOrder(customerId, tableNumber);

        if (existingPendingOrderOpt.isPresent()) {
            order = existingPendingOrderOpt.get();
            order.getOrderItems().clear(); // Xóa các món cũ để thêm lại danh sách mới
        } else {
            order = new Order();
            order.setOrderDate(LocalDateTime.now());
            
            Customer customer;
            if (customerId != null) {
                customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với ID: " + customerId));
            } else {
                customer = customerRepository.findById(GUEST_CUSTOMER_ID)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng vãng lai với ID " + GUEST_CUSTOMER_ID + ". Vui lòng tạo trước."));
            }
            order.setCustomer(customer);
        }
        
        order.setOrderType(orderRequest.getOrderType());
        order.setTableNumber(tableNumber);

        // Xử lý và thêm các món ăn vào đơn hàng
        for (OrderItemDto itemDto : orderRequest.getItems()) {
            MenuItem mainMenuItem = menuRepository.findById(itemDto.getMenuItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món ăn với ID: " + itemDto.getMenuItemId()));

            double mainItemPrice = calculatePriceForSize(mainMenuItem, itemDto.getSelectedSize());
            double totalToppingPrice = 0.0;
            StringBuilder toppingNames = new StringBuilder();

            if (itemDto.getToppingIds() != null && !itemDto.getToppingIds().isEmpty()) {
                List<MenuItem> toppings = menuRepository.findAllById(itemDto.getToppingIds());
                for (MenuItem topping : toppings) {
                    if (!"Topping".equalsIgnoreCase(topping.getCategory())) {
                        throw new IllegalArgumentException("Món ăn với ID " + topping.getId() + " không phải là topping hợp lệ.");
                    }
                    totalToppingPrice += topping.getPrice();
                    if (!toppingNames.isEmpty()) toppingNames.append(", ");
                    toppingNames.append(topping.getName());
                }
            }
            
            double finalPricePerItem = mainItemPrice + totalToppingPrice;

            OrderItem orderItem = new OrderItem();
            
            String fullItemName = mainMenuItem.getName();
            if (itemDto.getSelectedSize() != null && !itemDto.getSelectedSize().trim().isEmpty()) {
                fullItemName += " (Size " + itemDto.getSelectedSize().toUpperCase() + ")";
            }
            if (!toppingNames.isEmpty()) {
                fullItemName += " + " + toppingNames;
            }
            orderItem.setItemName(fullItemName);
            orderItem.setMenuItem(mainMenuItem);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setSelectedSize(itemDto.getSelectedSize());
            orderItem.setNotes(itemDto.getNotes());
            orderItem.setPriceAtOrder(finalPricePerItem);

            order.addOrderItem(orderItem);
        }

        recalculateOrderTotal(order);

        // --- Bắt đầu logic thanh toán và hoàn tất ---
        order.setPaymentMethod(orderRequest.getPaymentMethod());

        if ("CASH".equals(orderRequest.getPaymentMethod())) {
            Double cashReceived = orderRequest.getCashReceived();
            if (cashReceived == null || cashReceived < order.getTotalAmount()) {
                throw new IllegalArgumentException("Số tiền khách đưa không đủ để thanh toán.");
            }
            order.setCashReceived(cashReceived);
            order.setCashChange(cashReceived - order.getTotalAmount());
        }
        
        // Đặt trạng thái cuối cùng của đơn hàng là HOÀN THÀNH
        order.setStatus("COMPLETED");

        return orderRepository.save(order);
    } 

    private Optional<Order> findExistingPendingOrder(Long customerId, String tableNumber) {
        if (customerId != null) {
            return orderRepository.findByCustomerIdAndStatus(customerId, "PENDING");
        }
        // Bạn có thể mở rộng logic này nếu muốn tìm đơn hàng đang chờ theo số bàn
        // if (tableNumber != null && !tableNumber.isBlank()) {
        //     return orderRepository.findByTableNumberAndStatus(tableNumber, "PENDING");
        // }
        return Optional.empty();
    }
    
    private void recalculateOrderTotal(Order order) {
        double total = order.getOrderItems().stream()
                .mapToDouble(item -> item.getPriceAtOrder() * item.getQuantity())
                .sum();
        order.setTotalAmount(total);
    }

    private double calculatePriceForSize(MenuItem item, String size) {
        if (size == null || size.trim().isEmpty() || "M".equalsIgnoreCase(size)) {
            if (item.getPrice() == null) throw new IllegalArgumentException("Giá size M không có cho món: " + item.getName());
            return item.getPrice();
        }
        
        switch (size.toUpperCase()) {
            case "S":
                if (item.getPriceS() == null) throw new IllegalArgumentException("Size S không có cho món: " + item.getName());
                return item.getPriceS();
            case "L":
                if (item.getPriceL() == null) throw new IllegalArgumentException("Size L không có cho món: " + item.getName());
                return item.getPriceL();
            default:
                if (item.getPrice() == null) throw new IllegalArgumentException("Giá size M không có cho món: " + item.getName());
                return item.getPrice();
        }
    }
    
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryDto> getAllOrdersForHistory() {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"));
        return orders.stream()
                .map(this::convertToOrderHistoryDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Xóa một đơn hàng và tất cả các mục đơn hàng liên quan
     * @param orderId ID của đơn hàng cần xóa
     * @throws EntityNotFoundException nếu không tìm thấy đơn hàng
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));
        
        // Xóa tất cả các mục đơn hàng trước
        order.getOrderItems().clear();
        
        // Xóa đơn hàng
        orderRepository.delete(order);
    }

    private OrderHistoryDto convertToOrderHistoryDto(Order order) {
        OrderHistoryDto dto = new OrderHistoryDto();
        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setTableNumber(order.getTableNumber());
        dto.setOrderType(order.getOrderType());

        Customer customer = order.getCustomer();
        if (customer != null) {
            try {
                String customerName = customer.getName(); 
                dto.setCustomer(new CustomerDto(customer.getId(), customerName));
            } catch (EntityNotFoundException e) {
                dto.setCustomer(new CustomerDto(null, "[Khách hàng đã bị xóa]"));
            }
        } else {
            dto.setCustomer(new CustomerDto(null, "Khách vãng lai"));
        }

        List<OrderItemResponseDto> itemDtos = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemResponseDto itemDto = new OrderItemResponseDto();
                    itemDto.setId(item.getId());
                    itemDto.setItemName(item.getItemName());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setPriceAtOrder(item.getPriceAtOrder());
                    itemDto.setNotes(item.getNotes());
                    return itemDto;
                })
                .collect(Collectors.toList());
        dto.setOrderItems(itemDtos);

        return dto;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "'dashboard'", unless = "#result == null")
    public DashboardStatsDto getDashboardStatistics() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        // Use optimized single query for dashboard stats
        Object[] dashboardStats = orderRepository.getDashboardStats(startOfDay, endOfDay);
        Long totalOrders = ((Number) dashboardStats[0]).longValue();
        Double todayRevenue = (Double) dashboardStats[1];
        Long totalCustomers = ((Number) dashboardStats[2]).longValue();

        Long totalProducts = menuRepository.count();

        List<Order> latestOrdersEntities = orderRepository.findTop5ByOrderByOrderDateDesc();
        List<LatestOrderDto> latestOrders = latestOrdersEntities.stream()
                .map(order -> {
                    String customerName = "Khách vãng lai";
                    if (order.getCustomer() != null) {
                        try {
                            customerName = order.getCustomer().getName();
                        } catch (EntityNotFoundException e) {
                            customerName = "[Khách hàng đã bị xóa]";
                        }
                    }
                    return new LatestOrderDto(
                        order.getId(),
                        order.getOrderDate(),
                        order.getTotalAmount(),
                        order.getStatus(),
                        customerName
                    );
                })
                .collect(Collectors.toList());

        List<Object[]> topSellingItemsRaw = orderRepository.findTopSellingMenuItemsByQuantity();
        List<MenuItemSaleStatsDto> topSellingProducts = topSellingItemsRaw.stream()
                .limit(5)
                .map(obj -> new MenuItemSaleStatsDto(
                        (String) obj[0],
                        ((Number) obj[1]).longValue(),
                        (Double) obj[2]
                ))
                .collect(Collectors.toList());

        return new DashboardStatsDto(totalOrders, todayRevenue, totalProducts, totalCustomers,
                                     latestOrders, topSellingProducts);
    }
}
package com.example.demo.service;

import com.example.demo.model.MenuItem;
import com.example.demo.repository.MenuRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuRepository menuRepository;

    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    /**
     * Lấy tất cả các món trong menu.
     * @return Danh sách tất cả MenuItem.
     */
    public List<MenuItem> getAllMenuItems() {
        return menuRepository.findAll();
    }

    /**
     * Lấy một món trong menu theo ID.
     * @param id ID của món cần tìm.
     * @return MenuItem nếu tìm thấy.
     * @throws EntityNotFoundException nếu không tìm thấy món có ID tương ứng.
     */
    public MenuItem getMenuItemById(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món ăn với ID: " + id));
    }

    /**
     * Lưu một món mới hoặc cập nhật một món đã có.
     * @param menuItem Đối tượng MenuItem cần lưu.
     * @return MenuItem đã được lưu.
     */
    @Transactional
    public MenuItem saveMenuItem(MenuItem menuItem) {
    // Sửa lại validation để kiểm tra đúng thuộc tính 'priceM'
    if (menuItem.getPriceM() == null) {
        throw new IllegalArgumentException("Giá cho size vừa (M) không được để trống.");
    }
    
    // Đảm bảo ID không bị ghi đè khi tạo mới
    if (menuItem.getId() != null && !menuRepository.existsById(menuItem.getId())) {
         throw new EntityNotFoundException("Không tìm thấy món ăn với ID: " + menuItem.getId() + " để cập nhật.");
    }
    return menuRepository.save(menuItem);
    }
    /**
     * Xóa một món khỏi menu theo ID.
     * @param id ID của món cần xóa.
     */
    @Transactional
    public void deleteMenuItem(Long id) {
        if (!menuRepository.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy món ăn với ID: " + id + " để xóa.");
        }
        menuRepository.deleteById(id);
    }

    // --- CÁC PHƯƠNG THỨC HỖ TRỢ CHO TRANG ĐẶT HÀNG MỚI ---

    /**
     * Lấy tất cả các món có thể uống được (tức là không phải "Topping").
     * @return Danh sách các MenuItem không phải là Topping.
     */
    public List<MenuItem> getDrinkableItems() {
        return menuRepository.findByCategoryNotOrderByNameAsc("Topping");
    }

    /**
     * Lấy tất cả các món là "Topping".
     * @return Danh sách các MenuItem là Topping.
     */
    public List<MenuItem> getToppingItems() {
        return menuRepository.findByCategoryOrderByNameAsc("Topping");
    }

    /**
     * Lấy tất cả các món và nhóm chúng theo danh mục.
     * @return Một Map với key là tên danh mục và value là danh sách các MenuItem.
     */
    public Map<String, List<MenuItem>> getMenuItemsGroupedByCategory() {
        List<MenuItem> allItems = menuRepository.findAll();
        return allItems.stream()
                .filter(MenuItem::isAvailable)
                .collect(Collectors.groupingBy(MenuItem::getCategory));
    }
}
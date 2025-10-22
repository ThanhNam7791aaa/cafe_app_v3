package com.example.demo.controller;

import com.example.demo.model.MenuItem;
import com.example.demo.service.MenuService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Đây là một REST Controller, chuyên xử lý các yêu cầu API liên quan đến Menu.
 * Nó chỉ trả về dữ liệu JSON, không trả về các trang HTML (view).
 */
@RestController
@RequestMapping("/api/admin/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * API để lấy tất cả các món trong menu, được nhóm theo danh mục.
     * DÙNG CHO TRANG ĐẶT HÀNG MỚI (menu-order.html).
     * URL: GET /api/admin/menu/grouped
     * @return Một Map với key là tên danh mục và value là danh sách các MenuItem.
     */
    @GetMapping("/grouped")
    public ResponseEntity<Map<String, List<MenuItem>>> getMenuItemsGrouped() {
        // Tên phương thức đã đúng với MenuService của bạn
        Map<String, List<MenuItem>> groupedItems = menuService.getMenuItemsGroupedByCategory();
        return ResponseEntity.ok(groupedItems);
    }

    /**
     * API để lấy tất cả các món trong menu (dạng danh sách phẳng).
     * DÙNG CHO TRANG QUẢN LÝ MENU (menucontrol.html).
     * URL: GET /api/admin/menu
     * @return Danh sách tất cả MenuItem.
     */
    @GetMapping
    public ResponseEntity<List<MenuItem>> getAllMenuItems() {
        // Tên phương thức đã đúng với MenuService của bạn
        List<MenuItem> items = menuService.getAllMenuItems();
        return ResponseEntity.ok(items);
    }

    /**
     * API để lấy một món trong menu theo ID.
     * URL: GET /api/admin/menu/{id}
     * @param id ID của món cần tìm.
     * @return MenuItem nếu tìm thấy, hoặc 404 Not Found nếu không.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MenuItem> getMenuItemById(@PathVariable Long id) {
        try {
            // Tên phương thức đã đúng với MenuService của bạn
            MenuItem item = menuService.getMenuItemById(id);
            return ResponseEntity.ok(item);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * API để thêm một món mới vào menu.
     * URL: POST /api/admin/menu
     * @param menuItem Dữ liệu món mới từ request body (dạng JSON).
     * @return MenuItem đã được tạo.
     */
    @PostMapping
    public ResponseEntity<MenuItem> addMenuItem(@RequestBody MenuItem menuItem) {
        try {
            // Đảm bảo ID là null để tạo mới
            menuItem.setId(null);
            // Tên phương thức đã đúng với MenuService của bạn
            MenuItem savedItem = menuService.saveMenuItem(menuItem);
            return new ResponseEntity<>(savedItem, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * API để cập nhật một món đã có trong menu.
     * URL: PUT /api/admin/menu/{id}
     * @param id ID của món cần cập nhật.
     * @param menuItem Dữ liệu cập nhật từ request body (dạng JSON).
     * @return MenuItem đã được cập nhật.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MenuItem> updateMenuItem(@PathVariable Long id, @RequestBody MenuItem menuItem) {
        try {
            // Đảm bảo ID từ URL được sử dụng
            menuItem.setId(id);
            // Tên phương thức đã đúng với MenuService của bạn
            MenuItem updatedItem = menuService.saveMenuItem(menuItem);
            return ResponseEntity.ok(updatedItem);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * API để xóa một món khỏi menu.
     * URL: DELETE /api/admin/menu/{id}
     * @param id ID của món cần xóa.
     * @return 204 No Content nếu xóa thành công.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        try {
            // Tên phương thức đã đúng với MenuService của bạn
            menuService.deleteMenuItem(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
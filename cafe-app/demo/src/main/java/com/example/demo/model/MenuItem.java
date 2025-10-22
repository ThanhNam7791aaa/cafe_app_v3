package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_items")
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    private String image;
   
    private boolean available = true;

    // --- Các trường MỚI và đã điều chỉnh ---

    @Column(nullable = false) // Đảm bảo mọi món đều có danh mục
    private String category; // Ví dụ: "Cafe", "Trà Sữa", "Topping"

    
    @Column(name = "prices") // Ánh xạ tới cột 'prices'
    private Double priceS;

    @Column(name = "pricem", nullable = false) // Ánh xạ tới cột 'pricem'
    private Double price; // Thuộc tính 'price' trong Java sẽ đại diện cho giá size M

    @Column(name = "pricel") // Ánh xạ tới cột 'pricel'
    private Double priceL;
    // Default constructor
    public MenuItem() {
    }

    // Parameterized constructor
    public MenuItem(String name, String description, String image, boolean available,
                    String category, Double priceS, Double priceM, Double priceL) {
        this.name = name;
        this.description = description;
        this.image = image;
        this.available = available;
        this.category = category;
        this.priceS = priceS;
        this.price = priceM; // Gán giá size M cho thuộc tính 'price'
        this.priceL = priceL;
    }

    // Getters and Setters
   public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    // Getters và Setters cho các trường mới
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getPriceS() { return priceS; }
    public void setPriceS(Double priceS) { this.priceS = priceS; }
    public Double getPriceM() { return price; }
    public void setPriceM(Double priceM) { this.price = priceM; }
    public Double getPriceL() { return priceL; }
    public void setPriceL(Double priceL) { this.priceL = priceL; }
    
    /**
     * Lấy giá mặc định (size M)
     */
    public Double getPrice() {
        return price;
    }
    
    /**
     * Lấy giá theo size cụ thể
     * @param size Kích thước (S, M, L)
     * @return Giá tương ứng với size, mặc định là size M nếu size không hợp lệ
     */
    public Double getPriceBySize(String size) {
        if (size == null) {
            return price; // Mặc định size M
        }
        switch (size.toUpperCase()) {
            case "S":
                return priceS != null ? priceS : price; // Quay về M nếu S null
            case "L":
                return priceL != null ? priceL : price; // Quay về M nếu L null
            case "M":
            default:
                return price; // Trả về giá size M nếu size không hợp lệ
        }
    }
}

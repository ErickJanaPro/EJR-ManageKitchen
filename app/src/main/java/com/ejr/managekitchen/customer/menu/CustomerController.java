package com.ejr.managekitchen.customer.menu;

import com.ejr.managekitchen.customer.menu.dto.MenuDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerMenuMockService menuService;

    public CustomerController(CustomerMenuMockService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menu")
    public List<MenuDTO> getMenu() {
        return menuService.getAvailableMenu();
    }
}
package com.ejr.managekitchen.customer.menu;

import com.ejr.managekitchen.customer.menu.dto.MenuDTO;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class CustomerMenuMockService {

    public List<MenuDTO> getAvailableMenu() {
        return Arrays.asList(
            new MenuDTO(1L, "Burger Maison", "Bœuf, cheddar, salade, sauce secrète", 14.50, "🍔"),
            new MenuDTO(2L, "Pizza Margherita", "Tomate, mozzarella, basilic frais", 11.00, "🍕"),
            new MenuDTO(3L, "Salade César", "Poulet grillé, parmesan, croûtons", 12.50, "🥗")
        );
    }
}
package com.ejr.managekitchen.customer.menu;

import com.ejr.managekitchen.customer.menu.dto.MenuDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Catalogue provisoire aligné sur operations.order.port.StubSupplyChainAdapter
 * en attendant l'implémentation de F3 (Recettes & menus) par Dev 1.
 */
@Service
public class CustomerMenuMockService {

    public List<MenuDTO> getAvailableMenu() {
        return List.of(
                new MenuDTO(1L, "Burger végétarien", "Galette de légumes, cheddar affiné, salade croquante", new BigDecimal("12.50"), "🍔", true),
                new MenuDTO(2L, "Salade de saison", "Jeunes pousses, légumes grillés, vinaigrette maison", new BigDecimal("9.00"), "🥗", true),
                new MenuDTO(3L, "Tiramisu", "Mascarpone onctueux, biscuits imbibés au café, cacao", new BigDecimal("6.50"), "🍰", true),
                new MenuDTO(4L, "Risotto aux cèpes", "Riz arborio crémeux, cèpes poêlés, parmesan", new BigDecimal("15.00"), "🍄", false)
        );
    }
}
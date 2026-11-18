/**
 * Domaine Espace Client &amp; IA — Dev 3 (KPINSO-FOLLY Joël).
 * <ul>
 *   <li>{@code menu}           — consultation du menu côté client</li>
 *   <li>{@code map}            — F8 Géolocalisation &amp; carte (à créer)</li>
 *   <li>{@code recommendation} — F9 Recommandations (à créer)</li>
 *   <li>{@code assistant}      — F10 Chat &amp; assistant IA (à créer)</li>
 * </ul>
 * Points d'entrée utiles du domaine operations : API /api/orders, files WebSocket
 * /user/queue/orders et /user/queue/notifications, événement
 * {@code com.ejr.managekitchen.operations.order.OrderStatusChangedEvent} (voir doc/CONTRATS_OPERATIONS.md).
 */
package com.ejr.managekitchen.customer;

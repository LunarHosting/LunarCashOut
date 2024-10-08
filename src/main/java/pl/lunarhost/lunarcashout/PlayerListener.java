/*
 * Copyright 2024 Klaudiusz Wojtyczka <drago.klaudiusz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.lunarhost.lunarcashout;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import net.milkbowl.vault.economy.EconomyResponse;

public class PlayerListener implements Listener {

    /*
     * The plugin instance
     */
    private LunarCashOutPlugin plugin;

    /**
     * Creates the note listener
     */
    public PlayerListener(LunarCashOutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerClaimNote(PlayerInteractEvent event) {
        // Check if we need to use /deposit or if we can right click
        if (!plugin.getConfig().getBoolean("settings.allow-right-click-to-deposit-notes", true)) {
            return;
        }

        // Check the action
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Check if the player is allowed to deposit bank notes
        if (!event.getPlayer().hasPermission("banknotes.deposit")) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getPlayer().getItemInHand();

        // Verify that this is a real banknote
        if (item == null || !plugin.isBanknote(item)) {
            return;
        }

        double amount = plugin.getBanknoteAmount(item);

        // Negative banknotes are not allowed
        if (Double.compare(amount, 0) < 0) {
            return;
        }

        // Double check the response
        EconomyResponse response = plugin.getEconomy().depositPlayer(player, amount);
        if (response == null || !response.transactionSuccess()) {
            player.sendMessage(ChatColor.RED + "There was an error processing your transaction");
            plugin.getLogger().warning("Error processing player right click deposit " +
                    "(" + player.getName() + " for $" + plugin.formatDouble(amount) + ") " +
                    "[message: " + (response == null ? "null" : response.errorMessage) + "]");
            return;
        }

        // Deposit the money
        player.sendMessage(plugin.getMessage("messages.note-redeemed").replace("[money]", plugin.formatDouble(amount)));

        // Remove the slip
        if (item.getAmount() <= 1) {
            event.getPlayer().getInventory().removeItem(item);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }
}

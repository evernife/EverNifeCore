/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package br.com.finalcraft.evernifecore.nms.util;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

//Todo Remake this entire NMS System
//  - Separate per modules
//  - Remove unnecessary functions
//  - Create a default interface for non "not implemented"
//  - Add simple way for "force integrate"
public interface INMSUtils {

	@Deprecated
	public boolean hasInventory(Block b);

	@Deprecated
	public boolean hasInventory(Entity e);

	@Deprecated
	public boolean isInventoryOpen(Player p);

	@Deprecated
	public String getOpenInventoryName(Player p);

	@Deprecated
	public void updateSlot(Player p, int slot, ItemStack item);

	public String getItemRegistryName(ItemStack item);

	public ItemStack getItemFromMinecraftIdentifier(String minecraftIdentifier);

	@Deprecated
	public ArrayList<ItemStack> getTopInvetnoryItems(Player p);

	@Deprecated
	public String toBaseBinary(ItemStack itemStack);

	@Deprecated
	public ItemStack fromBaseBinary(String baseBinary64);

	@Deprecated
	public String getNBTtoString(ItemStack itemStack);

	@Deprecated
	public void applyNBTFromString(ItemStack itemStack, String nbtJson);

	@Deprecated
	public void clearNBT(ItemStack itemStack);

	public String getLocalizedName(ItemStack itemStack);

	public org.bukkit.inventory.ItemStack asItemStack(Object mcItemStack);

	public Object asMinecraftItemStack(org.bukkit.inventory.ItemStack itemStack);

	public void autoRespawnOnDeath(Player player);

	public boolean hasNBTTagCompound(ItemStack itemStack);

	@Deprecated
	public void setNBTString(ItemStack itemStack, String key, String value);

	@Deprecated
	public String getNBTString(ItemStack itemStack, String key);

	public boolean isTool(ItemStack itemStack);

	public boolean isSword(ItemStack itemStack);

	public boolean isArmor(ItemStack itemStack);

	public boolean isHelmet(ItemStack itemStack);

	public boolean isChestplate(ItemStack itemStack);

	public boolean isLeggings(ItemStack itemStack);

	public boolean isBoots(ItemStack itemStack);

	public boolean isFakePlayer(Player player);

	public Object asMinecraftEntity(Entity entity);

	public ItemStack validateItemStackHandle(ItemStack itemStack);
}
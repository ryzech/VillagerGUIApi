package masecla.villager.adapters.instances;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_5_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import masecla.villager.adapters.BaseAdapter;
import masecla.villager.classes.VillagerInventory;
import masecla.villager.classes.VillagerTrade;
import masecla.villager.events.VillagerInventoryCloseEvent;
import masecla.villager.events.VillagerInventoryModifyEvent;
import masecla.villager.events.VillagerInventoryOpenEvent;
import masecla.villager.events.VillagerTradeCompleteEvent;
import net.minecraft.server.v1_5_R2.EntityHuman;
import net.minecraft.server.v1_5_R2.IMerchant;
import net.minecraft.server.v1_5_R2.MerchantRecipe;
import net.minecraft.server.v1_5_R2.MerchantRecipeList;

public class MerchantAdapter_v1_5_R2 extends BaseAdapter implements IMerchant, Listener {

	public MerchantAdapter_v1_5_R2(VillagerInventory toAdapt) {
		super(toAdapt);
		Bukkit.getServer().getPluginManager().registerEvents(this,
				Bukkit.getPluginManager().getPlugin("VillagerGUIApi"));
	}

	@Override
	public void a(MerchantRecipe arg0) {
		try {
			Field f = arg0.getClass().getDeclaredField("maxUses");
			f.setAccessible(true);
			int maxUses = f.getInt(arg0);
			VillagerTrade trd = new VillagerTrade(CraftItemStack.asBukkitCopy(arg0.getBuyItem1()),
					CraftItemStack.asBukkitCopy(arg0.getBuyItem2()), CraftItemStack.asBukkitCopy(arg0.getBuyItem3()),
					maxUses);
			VillagerTradeCompleteEvent event = new VillagerTradeCompleteEvent(toAdapt, toAdapt.getForWho(), trd);
			Bukkit.getServer().getPluginManager().callEvent(event);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public MerchantRecipeList getOffers(EntityHuman arg0) {
		MerchantRecipeList rpl = new MerchantRecipeList();
		for (VillagerTrade trd : toAdapt.getTrades()) {
			MerchantRecipe toAdd = null;
			net.minecraft.server.v1_5_R2.ItemStack itm1 = CraftItemStack.asNMSCopy(trd.getItemOne());
			net.minecraft.server.v1_5_R2.ItemStack itm2 = null;
			net.minecraft.server.v1_5_R2.ItemStack result = CraftItemStack.asNMSCopy(trd.getResult());
			if (trd.requiresTwoItems())
				itm2 = CraftItemStack.asNMSCopy(trd.getItemTwo());
			else
				itm2 = CraftItemStack.asNMSCopy(new ItemStack(Material.AIR));

			toAdd = new MerchantRecipe(itm1, itm2, result);
			try {
				Field f = toAdd.getClass().getDeclaredField("maxUses");
				f.setAccessible(true);
				f.setInt(toAdd, trd.getMaxUses());
			} catch (Exception e) {
			}

			rpl.add(toAdd);
		}
		return rpl;
	}

	@Override
	public void openFor(Player p) {
		((CraftPlayer) p).getHandle().openTrade(this, toAdapt.getName());
		VillagerInventoryOpenEvent event = new VillagerInventoryOpenEvent(toAdapt, toAdapt.getForWho());
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	@Override
	public void a(EntityHuman arg0) {
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		if (event.getWhoClicked().getUniqueId().equals(this.toAdapt.getForWho().getUniqueId())) {
			VillagerInventoryModifyEvent modifyEvent = new VillagerInventoryModifyEvent(toAdapt,
					(Player) event.getWhoClicked(), event.getCurrentItem());
			Bukkit.getPluginManager().callEvent(modifyEvent);
			if (event.getRawSlot() == -999)
				return;
			if (event.getRawSlot() == 2 && !event.getCurrentItem().getType().equals(Material.AIR)) {
				ItemStack itemOne = this.toAdapt.getForWho().getOpenInventory().getTopInventory().getItem(0);
				ItemStack itemTwo = this.toAdapt.getForWho().getOpenInventory().getTopInventory().getItem(1);
				ItemStack result = event.getCurrentItem();
				VillagerTradeCompleteEvent completeEvent = new VillagerTradeCompleteEvent(toAdapt,
						(Player) event.getWhoClicked(), new VillagerTrade(itemOne, itemTwo, result, -1));
				Bukkit.getPluginManager().callEvent(completeEvent);
			}
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (event.getPlayer().getUniqueId().equals(this.toAdapt.getForWho().getUniqueId())) {
			VillagerInventoryCloseEvent closeEvent = new VillagerInventoryCloseEvent(toAdapt,
					(Player) event.getPlayer());
			Bukkit.getPluginManager().callEvent(closeEvent);
			HandlerList.unregisterAll(this); // Kill this event listener
		}
	}

	@Override
	public EntityHuman m_() {
		return ((CraftPlayer) toAdapt.getForWho()).getHandle();
	}

}

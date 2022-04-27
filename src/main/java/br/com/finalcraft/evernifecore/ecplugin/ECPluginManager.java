package br.com.finalcraft.evernifecore.ecplugin;

import br.com.finalcraft.evernifecore.EverNifeCore;
import br.com.finalcraft.evernifecore.api.EverNifeCoreReloadEvent;
import br.com.finalcraft.evernifecore.api.events.reload.ECPluginPreReloadEvent;
import br.com.finalcraft.evernifecore.api.events.reload.ECPluginReloadEvent;
import br.com.finalcraft.evernifecore.time.FCTimeFrame;
import br.com.finalcraft.evernifecore.util.FCMessageUtil;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

public class ECPluginManager {

    private static final HashMap<String, ECPluginData> ECPLUGINS_MAP = new HashMap<>();

    @NotNull
    public static ECPluginData getOrCreateECorePluginData(Plugin plugin){
        return ECPLUGINS_MAP.computeIfAbsent(plugin.getName(), pluginName -> new ECPluginData(plugin));
    }

    public static void reloadPlugin(@Nullable CommandSender sender, @NotNull Plugin instance) {
        ECPluginData ecPluginData = getOrCreateECorePluginData(instance);
        if (!ecPluginData.canReload()){
            throw new NotImplementedException("This plugin does not implement a reload system on it! Tell the author!");
        }
        reloadPlugin(sender, instance, ecPluginData, () -> ecPluginData.reloadPlugin());
    }

    public static void reloadPlugin(@Nullable CommandSender sender, @NotNull Plugin instance, @NotNull Runnable runnable){
        reloadPlugin(sender, instance, getOrCreateECorePluginData(instance), runnable);
    }

    public static void reloadPlugin(@Nullable CommandSender sender, @NotNull Plugin instance, ECPluginData ecPluginData, @NotNull Runnable runnable){
        //Fire Pre-Reload
        //Mainly used for Plugins that has other addons or modules
        Bukkit.getPluginManager().callEvent(new ECPluginPreReloadEvent(ecPluginData));

        long start = System.currentTimeMillis();

        //Do the reload
        runnable.run();
        //Reload locales as well
        ecPluginData.reloadAllCustomLocales();

        long end = System.currentTimeMillis();

        //Notify the Console
        ecPluginData.getPlugin().getLogger().info("§e[Reloading] §a" + ecPluginData.getPlugin().getName() + " has been reloaded! §7(It took " + new FCTimeFrame(end - start).getFormatedDiscursive(true) + ")");

        //Notify the sender if it's a Player
        if (sender != null && sender instanceof Player == true){
            FCMessageUtil.pluginHasBeenReloaded(sender, instance.getName());
        }

        Bukkit.getPluginManager().callEvent(new ECPluginReloadEvent(ecPluginData));

        //Some ECPlugins might have subModules or Addons, reload them if necessary
        for (ECPluginData ecPlugin : new ArrayList<>(ECPLUGINS_MAP.values())) {
            if (ecPlugin.canReload()){
                for (Class<?> aClass : ecPlugin.getReloadAfter()) {
                    if (instance.getClass() == aClass){
                        ecPlugin.getPlugin().getLogger().info("[ECPlugin] Reloading by demand of ´" + instance.getName() + "´ reload.");
                        ecPlugin.reloadPlugin();
                    }
                }
            }
        }

        //If it's the EverNifeCore, call it's personal reload event
        if (ecPluginData.getPlugin() == EverNifeCore.instance){
            Bukkit.getPluginManager().callEvent(new EverNifeCoreReloadEvent(ecPluginData));
        }
    }

    public static void removePluginData(String playerName){
        ECPLUGINS_MAP.remove(playerName);
    }

    public static HashMap<String, ECPluginData> getECPluginsMap() {
        return ECPLUGINS_MAP;
    }
}


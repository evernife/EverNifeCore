package br.com.finalcraft.evernifecore.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FCColorUtil {

    public static ChatColor getRandomColor(){
        List<ChatColor> colors = new ArrayList<>();
        for (ChatColor value : ChatColor.values()) {
            if (value.isColor()){
                colors.add(value);
            }
        }
        return colors.get(FCMathUtil.getRandom().nextInt(colors.size()));
    }

    public static String colorfy(@Nullable String text){
        if (text == null) return null;
        return ChatColor.translateAlternateColorCodes('&',text);
    }

    public static List<String> colorfy(@Nullable List<String> text){
        if (text == null) return null;
        for (int i = 0; i < text.size(); i++) {
            text.set(i, colorfy(text.get(i)));
        }
        return text;
    }

    public static String stripColor(@Nullable final String input){
        return ChatColor.stripColor(input);
    }

    public static List<String> stripColor(@Nullable final List<String> text){
        if (text == null) return null;
        for (int i = 0; i < text.size(); i++) {
            text.set(i, stripColor(text.get(i)));
        }
        return text;
    }

}

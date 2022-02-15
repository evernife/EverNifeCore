package br.com.finalcraft.evernifecore.itembuilder;

import br.com.finalcraft.evernifecore.gui.item.GuiItemComplex;
import br.com.finalcraft.evernifecore.util.FCInputReader;
import dev.triumphteam.gui.builder.item.BaseItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FCItemBuilder extends BaseItemBuilder<FCItemBuilder> {

    protected FCItemBuilder(@NotNull ItemStack itemStack) {
        super(itemStack);
    }

    @NotNull
    public GuiItemComplex asGuiItemComplex() {
        return new GuiItemComplex(build());
    }

    @NotNull
    public FCItemBuilder apply(Consumer<FCItemBuilder> apply){
        apply.accept(this);
        return this;
    }

    @NotNull
    public FCItemBuilder applyIf(Supplier<Boolean> condition, Consumer<FCItemBuilder> apply){
        if (condition.get() == true){
            apply.accept(this);
        }
        return this;
    }

    @NotNull
    public FCItemBuilder durability(final int durability) {
        itemStack.setDurability((short) durability);
        return this;
    }

    @NotNull
    public FCItemBuilder material(Material material) {
        itemStack.setType(material);
        return this;
    }

    @NotNull
    public FCItemBuilder material(String material) {
        Material theMaterial = FCInputReader.parseMaterial(material);
        if (theMaterial == null){
            throw new IllegalArgumentException("The materialName '" + material + "' is not a valid Bukkit Material");
        }
        itemStack.setType(theMaterial);
        return this;
    }
}
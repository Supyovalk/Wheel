package com.manchick.wheel.widget;

import com.google.gson.JsonElement;
import com.manchick.wheel.client.screen.WidgetSlot;
import com.manchick.wheel.widget.action.Action;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Codecs;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Widget {

    static final Logger LOGGER = LoggerFactory.getLogger(Widget.class);

    public static final Codec<Widget> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(TextCodecs.CODEC.fieldOf("label").forGetter(Widget::getLabel),
                Codec.either(ItemStack.ITEM_CODEC, ItemStack.CODEC).fieldOf("preview").forGetter(Widget::getPreview),
                Codec.list(Action.CODEC).fieldOf("actions").forGetter(Widget::listActions),
                Codec.optionalField("take_slot", WidgetSlot.CODEC, false).forGetter(Widget::getTakenSlot))
                .apply(instance, Widget::new);
    });

    final Text label;
    final Either<RegistryEntry<Item>, ItemStack> preview;
    final List<Action> actions;
    final Optional<WidgetSlot> takenSlot;

    public Widget(Text label, Either<RegistryEntry<Item>, ItemStack> preview, List<Action> actions, Optional<WidgetSlot> takenSlot) {
        this.label = label;
        this.preview = preview;
        this.actions = actions;
        this.takenSlot = takenSlot;
    }

    public static Optional<Widget> deserialize(JsonElement element){
        DataResult<Widget> result = CODEC.parse(JsonOps.INSTANCE, element);
        return result.resultOrPartial(err -> LOGGER.error("An error occurred whilst trying to deserialize a widget: {}", err));
    }

    public void run(MinecraftClient client){
        actions.forEach(action -> action.run(client));
    }

    public Text getLabel() {
        return label;
    }

    public Either<RegistryEntry<Item>, ItemStack> getPreview() {
        return preview;
    }

    public ItemStack getStack(){
        return preview.map(ItemStack::new, Function.identity());
    }

    public List<Action> listActions() {
        return actions;
    }

    public boolean hasSlotTaken(){
        return takenSlot.isPresent();
    }

    public Optional<WidgetSlot> getTakenSlot() {
        return takenSlot;
    }

    public static Widget empty(){
        Text label = newLine()
                .append(space()).append(Text.translatable("widget.wheel.empty")).append(space()).append(newLine())
                .append(newLine())
                .append(space()).append(Text.translatable("widget.wheel.empty_description").formatted(Formatting.GRAY)).append(space()).append(newLine());
        return new Widget(label, Either.right(ItemStack.EMPTY), List.of(), Optional.empty());
    }

    private static MutableText newLine(){
        return Text.literal("\n");
    }

    private static MutableText space(){
        return Text.literal(" ");
    }
}

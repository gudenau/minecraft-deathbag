package net.gudenau.deathbag;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ClickType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DeathBagItem extends Item {
    public DeathBagItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        var tag = stack.getSubNbt("inventory");
        if (tag == null) {
            return;
        }
        int count = tag.getList("stacks", NbtElement.COMPOUND_TYPE).size();
        tooltip.add(new TranslatableText("tooltip.gud_deathbag.items", count));
    }
    
    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        if (world.isClient()) {
            return;
        }
        // MC is weird, if we can't get the real bag refund their items...
        PlayerData.getBag(player).ifPresentOrElse(
            (newStack) -> stack.setNbt(newStack.getNbt()),
            () -> {
                var stacks = new NbtList();
                for (var refundStack : Set.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.LEATHER, 8))) {
                    var compound = new NbtCompound();
                    refundStack.writeNbt(compound);
                    stacks.add(compound);
                }
                stack.getOrCreateSubNbt("inventory").put("stacks", stacks);
            }
        );
    }
    
    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT || !slot.canTakePartial(player) || !otherStack.isEmpty()) {
            return false;
        }
        removeFirstStack(stack).ifPresent(itemStack -> {
            player.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8f, 0.8f + player.getWorld().getRandom().nextFloat() * 0.4f);
            cursorStackReference.set(itemStack);
        });
        return true;
    }
    
    private Optional<ItemStack> removeFirstStack(ItemStack stack) {
        var stackTag = stack.getSubNbt("inventory");
        if (stackTag == null) {
            return Optional.empty();
        }
        var stacks = stackTag.getList("stacks", NbtElement.COMPOUND_TYPE);
        if (stacks.isEmpty()) {
            stack.setNbt(null);
            stack.setCount(0);
            return Optional.empty();
        }
        var tag = (NbtCompound) stacks.remove(0);
        tag.remove("Slot");
        if (stacks.isEmpty()) {
            stack.setNbt(null);
            stack.setCount(0);
        }
        return Optional.ofNullable(ItemStack.fromNbt(tag));
    }
}

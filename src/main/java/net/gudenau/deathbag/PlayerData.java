package net.gudenau.deathbag;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//TODO Figure out a good way to handle single player, UUIDs can change for offline users.
public final class PlayerData {
    private static Path saveDir;
    private static final Map<UUID, PlayerData> PLAYER_DATA = new HashMap<>();
    
    public static void collectInventory(PlayerEntity player) {
        var inventory = player.getInventory();
        var list = new NbtList();
        inventory.writeNbt(list);
        PlayerData.get(player).addInventory(list);
    }
    
    public static Optional<ItemStack> getBag(PlayerEntity player) {
        return getOptional(player)
            .flatMap((data) -> data.createBag(player));
    }
    
    public static void init(Path saveDir) {
        PlayerData.saveDir = saveDir;
    }
    
    private static PlayerData get(PlayerEntity player) {
        return PLAYER_DATA.computeIfAbsent(player.getUuid(), PlayerData::new);
    }
    
    private static Optional<PlayerData> getOptional(PlayerEntity player) {
        return Optional.ofNullable(PLAYER_DATA.get(player.getUuid()));
    }
    
    public static void flushAbsentPlayers(MinecraftServer server) {
        Set<UUID> knownPlayers = new HashSet<>(PLAYER_DATA.keySet());
        server.getPlayerManager().getPlayerList().stream()
            .map(Entity::getUuid)
            .forEach(knownPlayers::remove);
        knownPlayers.forEach((id) -> {
            var data = PLAYER_DATA.remove(id);
            if (data != null) {
                data.save();
            }
        });
    }
    
    public static void flushPlayer(PlayerEntity player) {
        var data = PLAYER_DATA.remove(player.getUuid());
        if (data != null) {
            data.save();
        }
    }
    
    public static void deinit() {
        for (PlayerData value : PLAYER_DATA.values()) {
            value.save();
        }
    }
    
    private final UUID uuid;
    private NbtList data;
    
    private volatile boolean dirty = false;
    
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        load();
    }
    
    private void save() {
        if (!dirty) {
            return;
        }
        dirty = false;
        var saveFile = saveDir.resolve(uuid.toString() + ".nbt.gz");
        try {
            Files.createDirectories(saveFile.getParent());
            try (var output = Files.newOutputStream(saveFile)) {
                var compound = new NbtCompound();
                compound.put("data", data);
                NbtIo.writeCompressed(compound, output);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Death Bag save data for player with a UUID of %s".formatted(uuid), e);
        }
    }
    
    private void load() {
        var saveFile = saveDir.resolve(uuid.toString() + ".nbt.gz");
        if (Files.exists(saveFile)) {
            try (var stream = Files.newInputStream(saveFile)) {
                var compound = NbtIo.readCompressed(stream);
                data = compound.getList("data", NbtType.LIST);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load Death Bag save data for player with a UUID of %s".formatted(uuid), e);
            }
        } else {
            data = new NbtList();
        }
        dirty = false;
    }
    
    private void addInventory(NbtList list) {
        data.add(list);
        dirty = true;
    }
    
    private Optional<ItemStack> createBag(PlayerEntity player) {
        if (data.isEmpty()) {
            return Optional.empty();
        }
        var stack = new ItemStack(DeathBag.Items.DEATH_BAG);
        var inventory = new PlayerInventory(player);
        inventory.readNbt((NbtList) data.remove(0));
        dirty = true;
        
        var stackTag = stack.getOrCreateSubNbt("inventory");
        var stackList = new NbtList();
        for (int i = 0, length = inventory.size(); i < length; i++) {
            var inventoryStack = inventory.getStack(i);
            if (!inventoryStack.isEmpty()) {
                var currentTag = new NbtCompound();
                currentTag.putByte("Slot", (byte) i);
                inventoryStack.writeNbt(currentTag);
                stackList.add(currentTag);
            }
        }
        stackTag.put("stacks", stackList);
        return Optional.of(stack);
    }
    
    private boolean hasBag() {
        return !data.isEmpty();
    }
}

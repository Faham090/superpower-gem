package com.superpower.gems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SuperpowerGemsMod implements ModInitializer {
    public static final String MOD_ID = "superpower_gems";

    public enum GemType {
        AETHER("aether_gem", "Flight", "Cloud Burst"),
        BLAZE("blaze_gem", "Fire Absorption", "Inferno Dash"),
        FROST("frost_gem", "Freeze Target (3-5s)", "Ice Shield"),
        SHADOW("shadow_gem", "Invisibility", "Shadow Step"),
        STORM("storm_gem", "Chain Lightning", "Speed Boost"),
        TERRA("terra_gem", "Earth Wall", "Iron Skin"),
        VAMPIRE("vampire_gem", "Life Steal", "Bat Swarm"),
        TIME("time_gem", "Time Slow", "Haste"),
        VOID("void_gem", "Teleport Anchor", "Void Pull"),
        SOLAR("solar_gem", "Regeneration", "Sunburn Aura");

        private final String id;
        private final String primaryPower;
        private final String secondaryPower;

        GemType(String id, String primaryPower, String secondaryPower) {
            this.id = id;
            this.primaryPower = primaryPower;
            this.secondaryPower = secondaryPower;
        }

        public String getId() { return id; }
        public String getPrimaryPower() { return primaryPower; }
        public String getSecondaryPower() { return secondaryPower; }
    }

    public static final Map<Item, GemType> GEM_ITEMS = new HashMap<>();

    @Override
    public void onInitialize() {
        for (GemType type : GemType.values()) {
            Item gemItem = new Item(new Item.Settings().maxCount(1));
            Registry.register(Registries.ITEM, Identifier.of(MOD_ID, type.getId()), gemItem);
            GEM_ITEMS.put(gemItem, type);
        }

        Item rngBox = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "rng_gem_box"), 
            new Item(new Item.Settings().maxCount(16)));

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient()) {
                if (rngBox.equals(stack.getItem())) {
                    stack.decrement(1);
                    GemType rolledGem = GemType.values()[new Random().nextInt(GemType.values().length)];
                    Item awardedItem = Registries.ITEM.get(Identifier.of(MOD_ID, rolledGem.getId()));

                    if (awardedItem != null) {
                        player.giveItemStack(new ItemStack(awardedItem));
                        player.sendMessage(Text.literal("§6[Bliss RNG] §eYou rolled the " + rolledGem.name() + " Gem!"), false);
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    }
                    return TypedActionResult.success(stack);
                }

                if (GEM_ITEMS.containsKey(stack.getItem())) {
                    GemType gem = GEM_ITEMS.get(stack.getItem());
                    ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

                    if (serverPlayer.isSneaking()) {
                        serverPlayer.sendMessage(Text.literal("§dActivated: " + gem.getSecondaryPower()), true);
                        serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 2, false, false));
                    } else {
                        serverPlayer.sendMessage(Text.literal("§bActivated: " + gem.getPrimaryPower()), true);
                        if (gem == GemType.FROST) {
                            serverPlayer.getWorld().getOtherEntities(serverPlayer, serverPlayer.getBoundingBox().expand(5.0), entity -> true)
                                .forEach(target -> target.setFrozenTicks(80)); // 4-second freeze
                        } else {
                            serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 140, 1, false, false));
                        }
                    }
                    return TypedActionResult.success(stack);
                }
            }
            return TypedActionResult.pass(stack);
        });
    }
}

package com.xoxoac.modules.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Set;

public final class ScaffoldA {

    private static final Set<Material> PLACEABLE_ON = Set.of(
            Material.COBWEB, Material.BAMBOO, Material.BAMBOO_SAPLING,
            Material.SUGAR_CANE, Material.VINE, Material.TWISTING_VINES,
            Material.WEEPING_VINES, Material.CAVE_VINES, Material.CAVE_VINES_PLANT,
            Material.SNOW, Material.LILY_PAD, Material.MOSS_CARPET,
            Material.PINK_PETALS, Material.GLOW_LICHEN, Material.SCULK_VEIN
    );

    private static final BlockFace[] STRUCTURAL_FACES = {
            BlockFace.DOWN,
            BlockFace.UP,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    };

    public String name() {
        return "ScaffoldA";
    }

    public String check(Player player, BlockPlaceContext context) {
        for (BlockFace face : STRUCTURAL_FACES) {
            Block neighbour = context.block().getRelative(face);
            if (neighbour.getType().isSolid() || PLACEABLE_ON.contains(neighbour.getType())) {
                return null;
            }
        }

        return "Suspicious floating block placement";
    }
}

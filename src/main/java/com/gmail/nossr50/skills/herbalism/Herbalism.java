package com.gmail.nossr50.skills.herbalism;

import com.gmail.nossr50.config.AdvancedConfig;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.BlockUtils;
import com.gmail.nossr50.util.skills.SkillUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;

import java.util.HashSet;

public class Herbalism {

    public static int farmersDietRankLevel1 = AdvancedConfig.getInstance().getFarmerDietRankChange();
    public static int farmersDietRankLevel2 = farmersDietRankLevel1 * 2;
    public static int farmersDietMaxLevel   = farmersDietRankLevel1 * 5;

    public static int greenThumbStageChangeLevel = AdvancedConfig.getInstance().getGreenThumbStageChange();
    public static int greenThumbStageMaxLevel    = greenThumbStageChangeLevel * 4;

    /**
     * Convert blocks affected by the Green Thumb & Green Terra abilities.
     *
     * @param blockState
     *            The {@link BlockState} to check ability activation for
     * @return true if the ability was successful, false otherwise
     */
    protected static boolean convertGreenTerraBlocks(BlockState blockState) {
        switch (blockState.getType()) {
            case COBBLESTONE_WALL:
                blockState.setType(Material.MOSSY_COBBLESTONE_WALL);
                return true;

            case STONE_BRICKS:
                blockState.setType(Material.MOSSY_STONE_BRICKS);
                return true;

            case DIRT :
            case GRASS_PATH :
                blockState.setType(Material.GRASS_BLOCK);
                return true;

            case COBBLESTONE :
                blockState.setType(Material.MOSSY_COBBLESTONE);
                return true;

            default :
                return false;
        }
    }

    /**
     * Calculate the drop amounts for kelp plants based on the blocks
     * relative to them.
     *
     * @param blockState
     *            The {@link BlockState} of the bottom block of the plant
     * @return the number of bonus drops to award from the blocks in this plant
     */
    protected static int countAndMarkDoubleDropsKelp(BlockState blockState, boolean triple, HerbalismManager herbalismManager) {
        Block block = blockState.getBlock();

        int kelpMaxHeight = 255;
        int amount = 1;

        // Handle the two blocks above it - cacti & sugar cane can only grow 3 high naturally
        for (int y = 1; y < kelpMaxHeight; y++) {
            Block relativeUpBlock = block.getRelative(BlockFace.UP, y);

            if(!isKelp(relativeUpBlock))
                break;

            amount += 1;

            if(herbalismManager.checkDoubleDrop(relativeUpBlock.getState()))
                BlockUtils.markDropsAsBonus(relativeUpBlock.getState(), triple);

        }

        return amount;
    }

    private static int addKelpDrops(int dropAmount, Block relativeBlock) {
        if (isKelp(relativeBlock) && !mcMMO.getPlaceStore().isTrue(relativeBlock)) {
            dropAmount++;
        } else {
            mcMMO.getPlaceStore().setFalse(relativeBlock);
        }

        return dropAmount;
    }

    private static boolean isKelp(Block relativeBlock) {
        Material kelptype_1 = Material.KELP_PLANT;
        Material kelptype_2 = Material.KELP;

        return relativeBlock.getType() == kelptype_1 || relativeBlock.getType() == kelptype_2;
    }

    private static int calculateChorusPlantDrops(Block target, boolean triple, HerbalismManager herbalismManager) {
        return calculateChorusPlantDropsRecursive(target, new HashSet<>(), triple, herbalismManager);
    }

    private static int calculateChorusPlantDropsRecursive(Block target, HashSet<Block> traversed, boolean triple, HerbalismManager herbalismManager) {
        if (target.getType() != Material.CHORUS_PLANT)
            return 0;

        // Prevent any infinite loops, who needs more than 64 chorus anyways
        if (traversed.size() > 64)
            return 0;

        if (!traversed.add(target))
            return 0;

        int dropAmount = 0;

        if (mcMMO.getPlaceStore().isTrue(target))
            mcMMO.getPlaceStore().setFalse(target);
        else
        {
            dropAmount++;

            if(herbalismManager.checkDoubleDrop(target.getState()))
                BlockUtils.markDropsAsBonus(target.getState(), triple);
        }

        for (BlockFace blockFace : new BlockFace[] { BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST ,BlockFace.WEST})
            dropAmount += calculateChorusPlantDropsRecursive(target.getRelative(blockFace, 1), traversed, triple, herbalismManager);

        return dropAmount;
    }

    /**
     * Calculate the drop amounts for multi block plants based on the blocks
     * relative to them.
     *
     * @param blockState
     *            The {@link BlockState} of the bottom block of the plant
     * @return the number of bonus drops to award from the blocks in this plant
     */
    protected static int countAndMarkDoubleDropsMultiBlockPlant(BlockState blockState, boolean triple, HerbalismManager herbalismManager) {
        Block block = blockState.getBlock();
        Material blockType = blockState.getType();
        int dropAmount = mcMMO.getPlaceStore().isTrue(block) ? 0 : 1;

        if (blockType == Material.CHORUS_PLANT) {
            dropAmount = 1;

            if (block.getRelative(BlockFace.DOWN, 1).getType() == Material.END_STONE) {
                dropAmount = calculateChorusPlantDrops(block, triple, herbalismManager);
            }
        } else {
            // Handle the two blocks above it - cacti & sugar cane can only grow 3 high naturally
            for (int y = 1; y < 255; y++) {
                Block relativeBlock = block.getRelative(BlockFace.UP, y);

                if (relativeBlock.getType() != blockType) {
                    break;
                }

                if (mcMMO.getPlaceStore().isTrue(relativeBlock)) {
                    mcMMO.getPlaceStore().setFalse(relativeBlock);
                } else {
                    dropAmount++;

                    if(herbalismManager.checkDoubleDrop(relativeBlock.getState()))
                        BlockUtils.markDropsAsBonus(relativeBlock.getState(), triple);
                }
            }
        }

        return dropAmount;
    }

    /**
     * Convert blocks affected by the Green Thumb & Green Terra abilities.
     *
     * @param blockState
     *            The {@link BlockState} to check ability activation for
     * @return true if the ability was successful, false otherwise
     */
    protected static boolean convertShroomThumb(BlockState blockState) {
        switch (blockState.getType()) {
            case DIRT :
            case GRASS_BLOCK:
            case GRASS_PATH :
                blockState.setType(Material.MYCELIUM);
                return true;

            default :
                return false;
        }
    }

    /**
     * Check if the block has a recently grown crop from Green Thumb
     *
     * @param blockState
     *            The {@link BlockState} to check green thumb regrown for
     * @return true if the block is recently regrown, false otherwise
     */
    public static boolean isRecentlyRegrown(BlockState blockState) {
        return blockState.hasMetadata(mcMMO.greenThumbDataKey) && !SkillUtils.cooldownExpired(blockState.getMetadata(mcMMO.greenThumbDataKey).get(0).asInt(), 1);
    }
}

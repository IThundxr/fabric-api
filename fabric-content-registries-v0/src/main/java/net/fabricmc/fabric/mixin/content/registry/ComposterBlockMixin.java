package net.fabricmc.fabric.mixin.content.registry;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;

import net.fabricmc.fabric.impl.content.registry.CompostingChanceRegistryImpl;

import net.minecraft.block.BlockState;
import net.minecraft.block.ComposterBlock;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraft.world.WorldAccess;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComposterBlock.class)
public class ComposterBlockMixin {
	@Shadow
	static BlockState addToComposter(@Nullable Entity user, BlockState state, WorldAccess world, BlockPos pos, ItemStack stack) {
		throw new AssertionError();
	}

	@Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2FloatMap;containsKey(Ljava/lang/Object;)Z"), cancellable = true)
	private void fabric$injectTagBasedCompostingOnUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
		int i = state.get(ComposterBlock.LEVEL);
		ItemStack itemStack = player.getStackInHand(hand);

		if (i < 8 && fabric$isInCompostableTag(itemStack)) {
			if (i < 7 && !world.isClient) {
				BlockState blockState = addToComposter(player, state, world, pos, itemStack);
				world.syncWorldEvent(1500, pos, state != blockState ? 1 : 0);
				player.incrementStat(Stats.USED.getOrCreateStat(itemStack.getItem()));
				if (!player.getAbilities().creativeMode) {
					itemStack.decrement(1);
				}
			}

			cir.setReturnValue(ActionResult.success(world.isClient));
		}
	}

	@Inject(method = "compost", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2FloatMap;containsKey(Ljava/lang/Object;)Z"), cancellable = true)
	private static void fabric$injectTagBasedCompostingCompost(Entity user, BlockState state, ServerWorld world, ItemStack stack, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		int i = state.get(ComposterBlock.LEVEL);
		if (i < 7 && fabric$isInCompostableTag(stack)) {
			BlockState blockState = addToComposter(user, state, world, pos, stack);
			stack.decrement(1);
			cir.setReturnValue(blockState);
		}
	}

	@Inject(method = "addToComposter", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2FloatMap;getFloat(Ljava/lang/Object;)F"), cancellable = true)
	private static void fabric$injectTagBasedCompostingAddToComposter(Entity user, BlockState state, WorldAccess world, BlockPos pos, ItemStack stack, CallbackInfoReturnable<BlockState> cir) {
		int i = state.get(ComposterBlock.LEVEL);
		float compostValue = fabric$getCompostValue(stack);
		if ((i != 0 || !(compostValue > 0.0F)) && !(world.getRandom().nextDouble() < (double) compostValue)) {
			cir.setReturnValue(state);
		}
	}

	@Unique
	private static boolean fabric$isInCompostableTag(ItemStack itemStack) {
		for (TagKey<Item> tagKey : CompostingChanceRegistryImpl.ITEM_TAG_TO_LEVEL_INCREASE_CHANCE.keySet()) {
			if (itemStack.isIn(tagKey)) {
				return true;
			}
		}
		return false;
	}

	@Unique
	private static float fabric$getCompostValue(ItemStack itemStack) {
		for (TagKey<Item> tagKey : CompostingChanceRegistryImpl.ITEM_TAG_TO_LEVEL_INCREASE_CHANCE.keySet()) {
			if (itemStack.isIn(tagKey)) {
				return CompostingChanceRegistryImpl.ITEM_TAG_TO_LEVEL_INCREASE_CHANCE.get(tagKey);
			}
		}
		return -1;
	}

	@Mixin(targets = "net.minecraft.block.ComposterBlock$ComposterInventory")
	static class ComposterInventoryMixin {
		@WrapOperation(method = "canInsert", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2FloatMap;containsKey(Ljava/lang/Object;)Z"))
		private boolean fabric$wrapOnInsertForItemTagCompostables(Object2FloatMap<ItemConvertible> instance, Object o, Operation<Boolean> original) {
			return (original.call(instance, o) || fabric$isInCompostableTag(((Item) o).getDefaultStack()));
		}
	}
}

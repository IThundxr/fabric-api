/*
 * Copyright (c) 2024 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.dev;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.util.Util;

import net.fabricmc.fabric.FabricDev;

@Mixin(Util.class)
public class UtilMixin {
	@ModifyExpressionValue(method = {
			"debugRunnable(Ljava/lang/String;Ljava/lang/Runnable;)Ljava/lang/Runnable;",
			"debugSupplier(Ljava/lang/String;Ljava/util/function/Supplier;)Ljava/util/function/Supplier;"
	}, at = @At(value = "FIELD", target = "Lnet/minecraft/SharedConstants;isDevelopment:Z"))
	private static boolean mevIsDevelopmentForDevModule2(boolean original) {
		return original || FabricDev.ENABLE_SUPPLIER_AND_RUNNABLE_DEBUGGING;
	}
	
	@ModifyExpressionValue(method = {"error", "throwOrPause"}, at = @At(value = "FIELD", target = "Lnet/minecraft/SharedConstants;isDevelopment:Z"))
	private static boolean mevIsDevelopmentForDevModule3(boolean original) {
		return original || FabricDev.ENABLE_EXCEPTION_IDE_PAUSING;
	}
}

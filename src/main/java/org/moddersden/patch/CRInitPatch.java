package org.moddersden.patch;

import org.moddersden.CosmicHooks;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Consumer;
import java.util.function.Function;

public class CRInitPatch extends GamePatch {
    @Override
    public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
        // Get the game's entrypoint (set in the GameProvider) from FabricLauncher
        String entrypoint = launcher.getEntrypoint();

        /* Check to see if we got only the entrypoint we want, as you can have multiple entrypoints set.
         * (Usually for client/server differences and the like, but I like to see this as being abusable
         * and allowing one provider to load multiple games.)
         */
        if (!entrypoint.startsWith("finalforeach.cosmicreach.")) {
            return;
        }

        // Store the entrypoint class as a ClassNode variable so that we can more easily work with it.
        ClassNode mainClass = classSource.apply(entrypoint);

        if (mainClass == null) {
            throw new RuntimeException("Could not load main class " + entrypoint + "!");
        }
        /* Set the initializer method, this is usually not the main method,
         * it should ideally be placed as close to the game loop as possible without being inside it...*/
        MethodNode initMethod = findMethod(mainClass, (method) -> method.name.equals("main"));

        if (initMethod == null) {
            // Do this if our method doesn't exist in the entrypoint class.
            throw new RuntimeException("Could not find init method in " + entrypoint + "!");
        }
        // Debug log stating that we found our initializer method.
        Log.debug(LogCategory.GAME_PATCH, "Found init method: %s -> %s", entrypoint, mainClass.name);
        // Debug log stating that the method is being patched with our hooks.
        Log.debug(LogCategory.GAME_PATCH, "Patching init method %s%s", initMethod.name, initMethod.desc);

        // Assign the variable `it` to the list of instructions for our initializer method.
        // Add our hooks to the initializer method.
        initMethod.instructions.iterator().add(new MethodInsnNode(Opcodes.INVOKESTATIC, CosmicHooks.INTERNAL_NAME, "init", "()V", false));
        classEmitter.accept(mainClass);
    }
}
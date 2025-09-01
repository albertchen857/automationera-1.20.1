package com.automationera.basic;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;

public class ExportFile {
    public static Path ensureAutomationEraDir() throws IOException {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        String ver = SharedConstants.getGameVersion().getName();
        Path dir = gameDir.resolve("schematics").resolve("AutomationEra");
        Files.createDirectories(dir);
        return dir;
    }

    public static void exportTutorialFolder(String machineId, int index) {
        try {
            Path dest = ensureAutomationEraDir();
            ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
            String modid = "automationera";
            String path = "tutorial/" + machineId + "/" + machineId + "_step" + index + ".nbt";
            Identifier id = Identifier.of(modid, path);
            Resource res = rm.getResource(id).orElse(null);
            if (res != null) {
                try (InputStream in = res.getInputStream()) {
                    String filename = machineId + ".nbt";
                    Files.copy(in, dest.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                System.err.println("Resource not found: " + id);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

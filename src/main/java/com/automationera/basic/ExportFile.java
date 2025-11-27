package com.automationera.basic;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.AcceptPendingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ExportFile {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("AutomationEraTutorial/FileManager");
    private static Path ensureAutomationEraDir() throws IOException {
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
    public static void openAutomationEraDir() {
        MinecraftClient.getInstance().execute(() -> {
            try {
                Path dir = ensureAutomationEraDir();
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(dir.toFile());
                    LOGGER.info("Java desktop running");
                }else {
                    LOGGER.warn("Java Desktop error");
                    String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);

                    ProcessBuilder pb;
                    if (os.contains("win")) {
                        // Windows
                        pb = new ProcessBuilder("explorer.exe", dir.toString());
                    } else if (os.contains("mac")) {
                        // macOS
                        pb = new ProcessBuilder("open", dir.toString());
                    } else {
                        // Linux
                        pb = new ProcessBuilder("xdg-open", dir.toString());
                    }
                    pb.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

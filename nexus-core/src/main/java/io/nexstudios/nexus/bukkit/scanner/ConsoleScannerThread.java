package io.nexstudios.nexus.bukkit.scanner;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.hooks.VaultHook;

import java.util.List;
import java.util.Scanner;

public class ConsoleScannerThread implements Runnable {

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();

            if("! For help, type".contains(line)) {
                if(NexusPlugin.getInstance().getServer().getPluginManager().getPlugin("Vault") != null) {
                    NexusPlugin.getInstance().vaultHook = new VaultHook(NexusPlugin.getInstance(), NexusPlugin.nexusLogger);
                    if (NexusPlugin.getInstance().vaultHook.getEconomy() != null) {
                        NexusPlugin.nexusLogger.info("<yellow>Vault Economy<reset> hook registered successfully.");
                    } else {
                        NexusPlugin.nexusLogger.error(List.of(
                                "Vault Economy hook could not be registered. Economy provider is null.",
                                "Please check if any Economy plugin is installed and enabled!"
                        ));
                    }
                } else {
                    NexusPlugin.nexusLogger.warning("Vault is not installed or enabled. Vault hook is not be available.");
                }
                break;
            }
        }

        scanner.close();
    }
}

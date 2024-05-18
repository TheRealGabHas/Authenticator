package fr.gabhas.authenticator;

import fr.gabhas.authenticator.commands.Verify;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Authenticator extends JavaPlugin {

    @Override
    public void onEnable() {
        saveResource("config.yml", false);

        getCommand("verify").setExecutor(new Verify());
        getLogger().info(NamedTextColor.GREEN + "[v] Authenticator enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info(NamedTextColor.RED + "[x] Authenticator disabled");
    }
}

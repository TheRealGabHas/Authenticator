package fr.gabhas.authenticator.commands;

import fr.gabhas.authenticator.Authenticator;
import fr.gabhas.authenticator.utils.CooldownManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Verify implements CommandExecutor {

    private final CooldownManager cooldownManager = new CooldownManager();

    final File configFile = new File(Authenticator.getPlugin(Authenticator.class).getDataFolder(), "config.yml");
    final YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    ConfigurationSection databaseInfos = config.getConfigurationSection("database");

    {
        if (databaseInfos == null) {
            JavaPlugin.getPlugin(Authenticator.class)
                    .getLogger()
                    .severe("Please configure the config.yml file with database connection information");
        }
    }

    // Fetching database credentials
    private final String connectionURL = databaseInfos.getString("link");
    private final String DB_USERNAME = databaseInfos.getString("username");
    private final String DB_PASSWORD = databaseInfos.getString("password");


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (sender instanceof Player player) {
            UUID playerId = player.getUniqueId();

            if (cooldownManager.hasCooldown(playerId)) {
                Duration timeLeft = cooldownManager.getRemainingCooldown(playerId);

                // If the cooldown time is > 0
                if (!timeLeft.isZero() || !timeLeft.isNegative()) {
                    Component text = Component.text()
                            .color(NamedTextColor.RED)
                            .content("You can only generate one code per 5 minutes (" + timeLeft.toMinutes() + "m" + timeLeft.toSeconds() % 60 + "s)").build();

                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1f, 1f);
                    player.sendMessage(text);
                    return false;
                }
            }

            try (Connection conn = DriverManager.getConnection(connectionURL, DB_USERNAME, DB_PASSWORD)) {

                // Register the player for a 5 minutes delay
                cooldownManager.setCooldown(playerId, Duration.ofSeconds(cooldownManager.getDefaultCooldown()));

                // Generate a code that isn't already in the database
                int code;
                boolean isUnique;
                do {
                    code = ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
                    isUnique = checkUniqueness(conn, code);
                } while (!isUnique);
                // Caution: May be infinite if the full range is utilized (very unlikely for large range)

                // Preparing SQL request and database connection
                String insertStatement = "INSERT INTO tickets (datetime, uuid, code) VALUES (?, ?, ?)";

                String date = LocalDateTime.now().toString();
                String uuid = player.getUniqueId().toString();

                // Inserting the generated code in the database
                PreparedStatement ps = conn.prepareStatement(insertStatement);
                ps.setString(1, date);
                ps.setString(2, uuid);
                ps.setInt(3, code);
                ps.execute();

                // Success message
                Component text = Component.text()
                        .color(NamedTextColor.GREEN)
                        .content("Your verification code is " + code).build();

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                player.sendMessage(text);
            } catch (SQLException e) {
                JavaPlugin.getPlugin(Authenticator.class).getLogger().warning(e.toString());

                Component text = Component.text()
                        .color(NamedTextColor.RED)
                        .content("An error occurred : " + e).build();

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1f, 1f);
                player.sendMessage(text);
            }


            return true;
        }
        return false;
    }


    // Check if an entry with the given code already exists in the database
    private static boolean checkUniqueness(Connection conn, int code) throws SQLException {
        String fetchStatement = "SELECT 1 FROM tickets WHERE code = ?";
        try (PreparedStatement statement = conn.prepareStatement(fetchStatement)) {
            statement.setInt(1, code);
            try (ResultSet rs = statement.executeQuery()) {
                return !rs.next(); // If rs.next() returns false, the code is unique
            }
        }
    }
}

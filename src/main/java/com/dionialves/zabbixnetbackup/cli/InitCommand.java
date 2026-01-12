package com.dionialves.zabbixnetbackup.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(
        name = "init",
        description = "Initialize ZNB configuration"
)
public class InitCommand implements Runnable {

    @Option(
            names = "--force",
            description = "Overwrite existing configuration"
    )
    boolean force;

    @Override
    public void run() {
        System.out.println("\nInitializing ZNB configuration...\n");

        Console console = System.console();
        if (console == null) {
            System.err.println("Console not available.");
            return;
        }

        String zabbixUrl = console.readLine("Zabbix URL: ");
        String zabbixUsername = console.readLine("Zabbix API username: ");
        char[] passwordChars = console.readPassword("Zabbix API password: ");
        String zabbixPassword = new String(passwordChars);

        Path configDir = Path.of(System.getProperty("user.home"), ".znb");
        Path configFile = configDir.resolve("config");

        try {
            if (Files.exists(configFile) && !force) {
                System.err.println("Configuration already exists. Use --force to overwrite.");
                return;
            }

            Files.createDirectories(configDir);

            String content = """
                    ZABBIX_URL=%s
                    ZABBIX_USERNAME=%s
                    ZABBIX_PASSWORD=%s
                    """.formatted(
                    zabbixUrl.trim(),
                    zabbixUsername.trim(),
                    zabbixPassword.trim()
            );

            Files.writeString(configFile, content);

            System.out.println("\nConfiguration saved to " + configFile);
        } catch (IOException e) {
            System.err.println("Failed to write configuration: " + e.getMessage());
        }
    }
}

package com.dionialves.cli;

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

        String zabbixUrl = readZabbixUrl(console);
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

    private static String readZabbixUrl(Console console) {
        System.out.println("\nDigite a URL completa do endpoint JSON-RPC do Zabbix.");
        System.out.println("  Ex.: http://servidor/zabbix/api_jsonrpc.php");
        System.out.println("       https://zabbix.exemplo.com/api_jsonrpc.php");
        System.out.println();
        String url = console.readLine("Zabbix URL: ");
        if (url == null) {
            System.err.println("Nao foi possivel ler a URL (stdin fechado). Rode 'znb init' em um terminal.");
            System.exit(1);
        }
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            System.err.println("A URL deve comecar com 'http://' ou 'https://'.");
            System.err.println("Ex.: http://servidor/zabbix/api_jsonrpc.php");
            System.exit(1);
        }
        if (!url.endsWith("api_jsonrpc.php")) {
            System.err.println("A URL deve terminar com 'api_jsonrpc.php'.");
            System.err.println("Ex.: http://servidor/zabbix/api_jsonrpc.php");
            System.exit(1);
        }
        return url;
    }
}

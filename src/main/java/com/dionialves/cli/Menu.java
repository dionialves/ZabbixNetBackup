package com.dionialves.cli;

import com.dionialves.core.connectors.*;
import com.dionialves.core.service.DeviceLoader;
import com.dionialves.model.Device;

import java.util.List;
import java.util.Scanner;

public class Menu {

    public void dataEntry() throws Exception {
        Scanner scanner = new Scanner(System.in);
        int option;

        do {
            this.display();

            System.out.print("Escolha uma opção: ");

            if (scanner.hasNextInt()) {
                option = scanner.nextInt();
                scanner.nextLine();

                this.processOption(option);

            } else {

                System.out.println("\n🚫 Opção inválida! Por favor, digite um número.");

                scanner.nextLine();
                option = -1;
            }

        } while (option != 6);
        scanner.close();
    }

    private void display() {
        System.out.println("\n\n------------------------------------");
        System.out.println("--- ⚙️ Menu Principal de Backup ---");
        System.out.println("1 - Backup de Equipamentos Mikrotik");
        System.out.println("2 - Backup de Equipamentos Ubiquiti");
        System.out.println("3 - Backup de Equipamentos Mimosa");
        System.out.println("4 - Backup de Equipamentos Datacom");
        System.out.println("5 - Backup de Equipamentos Cisco");
        System.out.println("6 - Sair");
        System.out.println("------------------------------------");
    }

    private void processOption(int option) throws Exception {
        switch (option) {
            case 1:
                this.mikrotikProcess();
                break;
            case 2:
                this.ubiquitiProcess();
                break;
            case 3:
                this.mimosaProcess();
                break;
            case 4:
                this.datacomProcess();
                break;
            case 5:
                this.ciscoProcess();
                break;
            case 6:
                System.out.println("\n👋 Saindo do programa.");
                break;
            default:
                System.out.println("\n⚠️ Opção" + option + " não reconhecida. Por favor, escolha uma opção válida (1 a 6).");
        }
    }

    private void mikrotikProcess() throws Exception {
        System.out.println("\nIniciando o processo de backup para Equipamentos Mikrotik...");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o login do equipamento: ");
        String username = scanner.nextLine();

        System.out.print("Digite a senha do equipamento: ");
        String password = scanner.nextLine();

        List<Device> listOfDevices = DeviceLoader.loadDevices("Mikrotik", "209");

        MikrotikConnector mikrotikConnector = new MikrotikConnector(username, password);
        mikrotikConnector.backupDevices(listOfDevices);
    }

    private void ubiquitiProcess() throws Exception {
        System.out.println("\nIniciando o processo de backup para Equipamentos Ubiquiti...");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o login do equipamento: ");
        String username = scanner.nextLine();

        System.out.print("Digite a senha do equipamento: ");
        String password = scanner.nextLine();

        List<Device> listOfDevices = DeviceLoader.loadDevices("Ubiquiti", "214");

        UbiquitiConnector ubiquitiConnector = new UbiquitiConnector(username, password);
        ubiquitiConnector.backupDevices(listOfDevices);
    }

    private void mimosaProcess() throws Exception {
        System.out.println("\nIniciando o processo de backup para Equipamentos Mimosa...");

        Scanner scanner = new Scanner(System.in);

        System.out.print("Digite a senha do equipamento: ");
        String password = scanner.nextLine();

        List<Device> listOfDevices = DeviceLoader.loadDevices("Mimosa", "215");

        MimosaConnector mimosaConnector = new MimosaConnector(password);
        mimosaConnector.backupOfListDevices(listOfDevices);
    }

    private void datacomProcess() throws Exception {
        System.out.println("\nIniciando o processo de backup para Equipamentos Datacom...");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o login do equipamento: ");
        String username = scanner.nextLine();

        System.out.print("Digite a senha do equipamento: ");
        String password = scanner.nextLine();

        List<Device> listOfDevices = DeviceLoader.loadDevices("Datacom", "216");

        DatacomConnector datacomConnector = new DatacomConnector(username, password);
        datacomConnector.backupDevices(listOfDevices);
    }

    private void ciscoProcess() throws Exception {
        System.out.println("\nIniciando o processo de backup para Equipamentos Cisco...");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o login do equipamento: ");
        String username = scanner.nextLine();

        System.out.print("Digite a senha do equipamento: ");
        String password = scanner.nextLine();

        List<Device> listOfDevices = DeviceLoader.loadDevices("Cisco", "217");

        CiscoConnector ciscoConnector = new CiscoConnector(username, password);
        ciscoConnector.backupDevices(listOfDevices);
    }
}

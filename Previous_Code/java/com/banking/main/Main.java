package com.banking.main;

import com.banking.model.AccountType;
import com.banking.model.Address;
import com.banking.model.BankAccount;
import com.banking.model.Customer;
import com.banking.model.Transaction;
import com.banking.service.BankingService;
import com.banking.util.FileLogger;
import com.banking.wallet.WalletOperations;

import java.util.List;
import java.util.Scanner;

public class Main {

    private final BankingService service;
    private final Scanner scanner;

    public Main(BankingService service, Scanner scanner) {
        this.service = service;
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        BankingService service = new BankingService(new FileLogger());
        try (Scanner scanner = new Scanner(System.in)) {
            new Main(service, scanner).run();
        }
    }

    public void run() {
        System.out.println("=== Banking Application ===");
        while (true) {
            showMenu();
            int choice = readInt("Choose an option: ");
            if (choice == 0) {
                System.out.println("Goodbye!");
                return;
            }
            try {
                handleChoice(choice);
            } catch (RuntimeException ex) {
                System.out.println("Error: " + ex.getMessage());
            } catch (CloneNotSupportedException ex) {
                System.out.println("Clone failed: " + ex.getMessage());
            }
        }
    }

    private void showMenu() {
        System.out.println();
        System.out.println("1)  Create customer");
        System.out.println("2)  Open account");
        System.out.println("3)  Deposit");
        System.out.println("4)  Withdraw");
        System.out.println("5)  Transfer (account → account)");
        System.out.println("6)  View transactions");
        System.out.println("7)  Account details + interest");
        System.out.println("8)  Clone account (deep copy demo)");
        System.out.println("9)  Create wallet");
        System.out.println("10) Wallet add money");
        System.out.println("11) Wallet pay bill");
        System.out.println("12) Wallet transfer (wallet → wallet)");
        System.out.println("0)  Exit");
    }

    private void handleChoice(int choice) throws CloneNotSupportedException {
        switch (choice) {
            case 1 -> createCustomer();
            case 2 -> openAccount();
            case 3 -> deposit();
            case 4 -> withdraw();
            case 5 -> transfer();
            case 6 -> viewTransactions();
            case 7 -> showAccountDetails();
            case 8 -> cloneAccount();
            case 9 -> createWallet();
            case 10 -> walletAddMoney();
            case 11 -> walletPayBill();
            case 12 -> walletTransfer();
            default -> System.out.println("Unknown option.");
        }
    }

    private void createCustomer() {
        String name = readLine("Name: ");
        String email = readLine("Email: ");
        String phone = readLine("Phone (10-digit, starts with 6-9): ");
        String street = readLine("Street: ");
        String city = readLine("City: ");
        String state = readLine("State: ");
        String postal = readLine("Postal code: ");
        Customer c = service.createCustomer(name, email, phone,
                new Address(street, city, state, postal));
        System.out.println("Created: " + c);
    }

    private void openAccount() {
        String customerId = readLine("Customer ID: ");
        Customer customer = service.getCustomers().get(customerId);
        if (customer == null) {
            System.out.println("Unknown customer.");
            return;
        }
        String typeStr = readLine("Type (SAVINGS / CURRENT): ").trim().toUpperCase();
        AccountType type = AccountType.valueOf(typeStr);
        double opening = readDouble("Opening balance: ");
        BankAccount account = service.openAccount(type, customer, opening);
        System.out.println("Opened account: " + account.getAccountNumber());
    }

    private void deposit() {
        String acc = readLine("Account number: ");
        double amount = readDouble("Amount: ");
        service.deposit(acc, amount);
        System.out.println("New balance: " + service.getAccount(acc).getBalance());
    }

    private void withdraw() {
        String acc = readLine("Account number: ");
        double amount = readDouble("Amount: ");
        service.withdraw(acc, amount);
        System.out.println("New balance: " + service.getAccount(acc).getBalance());
    }

    private void transfer() {
        String from = readLine("From account: ");
        String to = readLine("To account: ");
        double amount = readDouble("Amount: ");
        service.transfer(from, to, amount);
        System.out.println("Transfer successful.");
    }

    private void viewTransactions() {
        String acc = readLine("Account number: ");
        List<Transaction> txns = service.viewTransactions(acc);
        if (txns.isEmpty()) {
            System.out.println("No transactions.");
            return;
        }
        txns.forEach(System.out::println);
    }

    private void showAccountDetails() {
        String acc = readLine("Account number: ");
        BankAccount account = service.getAccount(acc);
        account.displayDetails();
        System.out.println("Calculated interest: " + account.calculateInterest());
    }

    private void cloneAccount() throws CloneNotSupportedException {
        String acc = readLine("Account number: ");
        BankAccount original = service.getAccount(acc);
        BankAccount copy = service.cloneAccount(acc);
        System.out.println("Original customer == clone customer? " +
                (original.getCustomer() == copy.getCustomer()));
        System.out.println("Original address  == clone address?  " +
                (original.getCustomer().getAddress() == copy.getCustomer().getAddress()));
        System.out.println("(Both should be false — deep copy)");
    }

    private void createWallet() {
        String customerId = readLine("Customer ID: ");
        Customer customer = service.getCustomers().get(customerId);
        if (customer == null) {
            System.out.println("Unknown customer.");
            return;
        }
        String provider = readLine("Provider (PAYTM / PHONEPE): ").trim().toUpperCase();
        double opening = readDouble("Opening balance: ");
        WalletOperations wallet = switch (provider) {
            case "PAYTM" -> service.createPaytmWallet(customer, opening);
            case "PHONEPE" -> service.createPhonePeWallet(customer, opening);
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
        System.out.println("Created wallet: " + wallet.getWalletId());
    }

    private void walletAddMoney() {
        String id = readLine("Wallet ID: ");
        double amount = readDouble("Amount: ");
        service.walletAddMoney(id, amount);
        System.out.println("New wallet balance: " + service.getWallet(id).getBalance());
    }

    private void walletPayBill() {
        String id = readLine("Wallet ID: ");
        double amount = readDouble("Amount: ");
        service.walletPayBill(id, amount);
        System.out.println("New wallet balance: " + service.getWallet(id).getBalance());
    }

    private void walletTransfer() {
        String from = readLine("From wallet: ");
        String to = readLine("To wallet: ");
        double amount = readDouble("Amount: ");
        service.walletTransfer(from, to, amount);
        System.out.println("Wallet transfer successful.");
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return Double.parseDouble(line.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a number.");
            }
        }
    }
}

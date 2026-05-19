package main;

import exception.*;
import service.BankingService;
import wallet.PaytmWallet;
import wallet.PhonePeWallet;
import wallet.WalletOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static BankingService service = new BankingService();
    private static Scanner scanner        = new Scanner(System.in);

    // In-memory wallet registry for demo (walletId -> WalletOperations)
    private static Map<String, WalletOperations> walletRegistry = new HashMap<>();

    public static void main(String[] args) {

        System.out.println("============================================");
        System.out.println("   Smart Banking & Digital Wallet System    ");
        System.out.println("============================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Enter your choice: ");
            int choice = readInt();

            switch (choice) {
                case 1: createCustomer();      break;
                case 2: openAccount();         break;
                case 3: deposit();             break;
                case 4: withdraw();            break;
                case 5: transfer();            break;
                case 6: walletOperations();    break;
                case 7: cloneAccount();        break;
                case 8: viewTransactions();    break;
                case 9:
                    System.out.println("\n  Exiting... Thank you for using Smart Banking!");
                    running = false;
                    break;
                default:
                    System.out.println("  Invalid choice. Please enter 1-9.");
            }
        }
        scanner.close();
    }

    // -------------------------------------------------------------- Menu
    private static void printMenu() {
        System.out.println("\n--------------------------------------------");
        System.out.println("  1. Create Customer");
        System.out.println("  2. Open Account");
        System.out.println("  3. Deposit");
        System.out.println("  4. Withdraw");
        System.out.println("  5. Transfer");
        System.out.println("  6. Wallet Operations");
        System.out.println("  7. Clone Account");
        System.out.println("  8. View Transactions");
        System.out.println("  9. Exit");
        System.out.println("--------------------------------------------");
    }

    // -------------------------------------------------------------- 1. Create Customer
    private static void createCustomer() {
        System.out.println("\n  -- Create Customer --");
        System.out.print("  Customer ID  : "); String id    = scanner.nextLine().trim();
        System.out.print("  Name         : "); String name  = scanner.nextLine().trim();
        System.out.print("  Email        : "); String email = scanner.nextLine().trim();
        System.out.print("  Phone Number : "); String phone = scanner.nextLine().trim();

        try {
            service.createCustomer(id, name, email, phone);
        } catch (DuplicateCustomerException | InvalidEmailException |
                 InvalidPhoneNumberException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        } finally {
            System.out.println("  [Create Customer operation completed]");
        }
    }

    // -------------------------------------------------------------- 2. Open Account
    private static void openAccount() {
        System.out.println("\n  -- Open Account --");
        System.out.print("  Account Number : "); String accNo      = scanner.nextLine().trim();
        System.out.print("  Customer ID    : "); String custId     = scanner.nextLine().trim();
        System.out.print("  Initial Balance: "); double initBal    = readDouble();
        System.out.println("  Account Type: 1-Savings  2-Current");
        System.out.print("  Choice: "); int type = readInt();

        try {
            if (type == 1) {
                System.out.print("  Interest Rate (%): "); double rate = readDouble();
                service.openSavingsAccount(accNo, custId, initBal, rate);
            } else if (type == 2) {
                System.out.print("  Overdraft Limit: "); double od = readDouble();
                service.openCurrentAccount(accNo, custId, initBal, od);
            } else {
                System.out.println("  Invalid account type selected.");
            }
        } catch (InvalidAmountException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        } finally {
            System.out.println("  [Open Account operation completed]");
        }
    }

    // -------------------------------------------------------------- 3. Deposit
    private static void deposit() {
        System.out.println("\n  -- Deposit --");
        System.out.print("  Account Number: "); String accNo  = scanner.nextLine().trim();
        System.out.print("  Amount        : "); double amount = readDouble();
        // Exception handling is inside service.deposit()
        service.deposit(accNo, amount);
    }

    // -------------------------------------------------------------- 4. Withdraw
    private static void withdraw() {
        System.out.println("\n  -- Withdraw --");
        System.out.print("  Account Number: "); String accNo  = scanner.nextLine().trim();
        System.out.print("  Amount        : "); double amount = readDouble();
        service.withdraw(accNo, amount);
    }

    // -------------------------------------------------------------- 5. Transfer
    private static void transfer() {
        System.out.println("\n  -- Transfer --");
        System.out.print("  From Account: "); String from   = scanner.nextLine().trim();
        System.out.print("  To Account  : "); String to     = scanner.nextLine().trim();
        System.out.print("  Amount      : "); double amount = readDouble();
        service.transfer(from, to, amount);
    }

    // -------------------------------------------------------------- 6. Wallet Operations
    private static void walletOperations() {
        System.out.println("\n  -- Wallet Operations --");
        System.out.println("  1. Create Paytm Wallet");
        System.out.println("  2. Create PhonePe Wallet");
        System.out.println("  3. Add Money to Wallet");
        System.out.println("  4. Pay Bill");
        System.out.println("  5. Transfer Between Wallets");
        System.out.println("  6. View Wallet Balance");
        System.out.print("  Choice: "); int choice = readInt();

        try {
            switch (choice) {
                case 1: {
                    System.out.print("  Wallet ID  : "); String wid   = scanner.nextLine().trim();
                    System.out.print("  Owner Name : "); String owner = scanner.nextLine().trim();
                    System.out.print("  Initial Bal: "); double bal   = readDouble();
                    PaytmWallet w = service.createPaytmWallet(wid, owner, bal);
                    walletRegistry.put(wid, w);
                    break;
                }
                case 2: {
                    System.out.print("  Wallet ID  : "); String wid   = scanner.nextLine().trim();
                    System.out.print("  Owner Name : "); String owner = scanner.nextLine().trim();
                    System.out.print("  Initial Bal: "); double bal   = readDouble();
                    PhonePeWallet w = service.createPhonePeWallet(wid, owner, bal);
                    walletRegistry.put(wid, w);
                    break;
                }
                case 3: {
                    System.out.print("  Wallet ID: "); String wid    = scanner.nextLine().trim();
                    System.out.print("  Amount   : "); double amount = readDouble();
                    WalletOperations w = getWallet(wid);
                    if (w != null) w.addMoney(amount);
                    break;
                }
                case 4: {
                    System.out.print("  Wallet ID : "); String wid  = scanner.nextLine().trim();
                    System.out.print("  Bill Type : "); String bill = scanner.nextLine().trim();
                    System.out.print("  Amount    : "); double amt  = readDouble();
                    WalletOperations w = getWallet(wid);
                    if (w != null) w.payBill(bill, amt);
                    break;
                }
                case 5: {
                    System.out.print("  From Wallet ID: "); String fromId = scanner.nextLine().trim();
                    System.out.print("  To Wallet ID  : "); String toId   = scanner.nextLine().trim();
                    System.out.print("  Amount        : "); double amt    = readDouble();
                    WalletOperations from = getWallet(fromId);
                    WalletOperations to   = getWallet(toId);
                    if (from != null && to != null) from.transferToWallet(to, amt);
                    break;
                }
                case 6: {
                    System.out.print("  Wallet ID: "); String wid = scanner.nextLine().trim();
                    WalletOperations w = getWallet(wid);
                    if (w != null) w.displayBalance();
                    break;
                }
                default:
                    System.out.println("  Invalid wallet operation choice.");
            }
        } catch (InvalidAmountException | InsufficientBalanceException |
                 WalletLimitExceededException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        } finally {
            System.out.println("  [Wallet operation completed]");
        }
    }

    private static WalletOperations getWallet(String walletId) {
        WalletOperations w = walletRegistry.get(walletId);
        if (w == null) {
            System.out.println("  [ERROR] No wallet found with ID: " + walletId);
        }
        return w;
    }

    // -------------------------------------------------------------- 7. Clone Account
    private static void cloneAccount() {
        System.out.println("\n  -- Clone Account --");
        System.out.println("  1. Demonstrate Shallow vs Deep Clone (Customer)");
        System.out.println("  2. Clone Account");
        System.out.print("  Choice: "); int choice = readInt();

        try {
            if (choice == 1) {
                System.out.print("  Customer ID to clone: ");
                String custId = scanner.nextLine().trim();
                service.demonstrateShallowVsDeepClone(custId);
            } else if (choice == 2) {
                System.out.print("  Account Number to clone: ");
                String accNo = scanner.nextLine().trim();
                service.cloneAccount(accNo);
            } else {
                System.out.println("  Invalid choice.");
            }
        } catch (CloneNotSupportedException e) {
            // Checked exception: CloneNotSupportedException
            System.out.println("  [CloneNotSupportedException] " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        } finally {
            System.out.println("  [Clone operation completed]");
        }
    }

    // -------------------------------------------------------------- 8. View Transactions
    private static void viewTransactions() {
        System.out.println("\n  -- View Transactions --");
        System.out.print("  Account Number: ");
        String accNo = scanner.nextLine().trim();
        service.viewTransactions(accNo);
    }

    // -------------------------------------------------------------- Helpers
    private static int readInt() {
        try {
            int val = Integer.parseInt(scanner.nextLine().trim());
            return val;
        } catch (NumberFormatException e) {
            System.out.println("  [ERROR] Invalid number input. Defaulting to 0.");
            return 0;
        }
    }

    private static double readDouble() {
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  [ERROR] Invalid amount input. Defaulting to 0.0");
            return 0.0;
        }
    }
}

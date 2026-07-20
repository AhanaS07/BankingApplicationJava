package com.tnf.account.exception;

// Thrown when an account-to-account transfer cannot be completed.
// reconciled=true  -> the debit was rolled back; no money moved (safe to retry).
// reconciled=false -> CRITICAL: rollback also failed; balances may be inconsistent
//                     and require manual reconciliation.
public class AccountTransferException extends RuntimeException {

    private final boolean reconciled;

    public AccountTransferException(String message, boolean reconciled, Throwable cause) {
        super(message, cause);
        this.reconciled = reconciled;
    }

    public boolean isReconciled() {
        return reconciled;
    }
}

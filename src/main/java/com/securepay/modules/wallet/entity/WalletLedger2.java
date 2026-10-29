package com.securepay.modules.wallet.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** {@code wallet_ledgers2} — W2 ledger (doc 06 §6.3). */
@Entity
@Table(name = "wallet_ledgers2")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WalletLedger2 extends AbstractWalletLedger {
}

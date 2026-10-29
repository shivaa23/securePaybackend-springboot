package com.securepay.modules.wallet.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** {@code wallet_ledgers3} — W3 ledger (explicit table name in source) (doc 06 §6.3). */
@Entity
@Table(name = "wallet_ledgers3")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WalletLedger3 extends AbstractWalletLedger {
}

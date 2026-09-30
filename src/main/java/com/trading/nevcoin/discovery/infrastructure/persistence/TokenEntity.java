package com.trading.nevcoin.discovery.infrastructure.persistence;

import com.trading.nevcoin.discovery.domain.Token;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tokens")
public class TokenEntity {

    @Id
    private String mintAddress;
    private String symbol;
    private String name;
    private int decimals;
    private Instant createdAt;
    private Instant firstSeenAt;
    @Enumerated(EnumType.STRING)
    private Token.Status status;

    protected TokenEntity() { }

    public TokenEntity(Token token) {
        this.mintAddress = token.mintAddress();
        this.symbol = token.symbol();
        this.name = token.name();
        this.decimals = token.decimals();
        this.createdAt = token.createdAt();
        this.firstSeenAt = token.firstSeenAt();
        this.status = token.status();
    }

    public Token toDomain() {
        return new Token(mintAddress, symbol, name, decimals, createdAt, firstSeenAt, status);
    }
}

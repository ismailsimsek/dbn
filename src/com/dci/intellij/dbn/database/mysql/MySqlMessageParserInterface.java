package com.dci.intellij.dbn.database.mysql;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import org.jetbrains.annotations.Nullable;

import com.dci.intellij.dbn.database.DatabaseMessageParserInterface;
import com.dci.intellij.dbn.database.DatabaseObjectIdentifier;

public class MySqlMessageParserInterface implements DatabaseMessageParserInterface {
    @Nullable
    public DatabaseObjectIdentifier identifyObject(SQLException exception) {
         return null;
    }

    @Override
    public boolean isTimeoutException(SQLException e) {
        return e instanceof SQLTimeoutException;
    }

    @Override
    public boolean isModelException(SQLException e) {
        return false;
    }

    @Override
    public boolean isAuthenticationException(SQLException e) {
        return false;
    }

}
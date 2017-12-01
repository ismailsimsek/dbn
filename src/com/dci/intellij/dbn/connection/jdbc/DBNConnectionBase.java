package com.dci.intellij.dbn.connection.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

abstract class DBNConnectionBase extends DBNResource implements Connection, Closeable, Invalidable {
    protected Connection inner;

    public DBNConnectionBase(Connection inner) {
        super(ResourceType.CONNECTION);
        this.inner = inner;
    }

    protected abstract <S extends Statement> S wrap(S statement);

    /********************************************************************
     *                            Wrapped calls                         *
     ********************************************************************/
    @Override
    public DBNStatement createStatement() throws SQLException {
        return (DBNStatement) wrap(inner.createStatement());
    }

    @Override
    public DBNPreparedStatement prepareStatement(String sql) throws SQLException {
        return (DBNPreparedStatement) wrap(inner.prepareStatement(sql));
    }

    @Override
    public DBNCallableStatement prepareCall(String sql) throws SQLException {
        return (DBNCallableStatement) wrap(inner.prepareCall(sql));
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return wrap(inner.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return wrap(inner.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return wrap(inner.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return wrap(inner.prepareStatement(sql, autoGeneratedKeys));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return wrap(inner.prepareStatement(sql, columnIndexes));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return wrap(inner.prepareStatement(sql, columnNames));
    }

    @Override
    public DBNStatement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return (DBNStatement) wrap(inner.createStatement(resultSetType, resultSetConcurrency));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return wrap(inner.prepareStatement(sql, resultSetType, resultSetConcurrency));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return wrap(inner.prepareCall(sql, resultSetType, resultSetConcurrency));
    }

    /********************************************************************
     *                     Wrapped executions                           *
     ********************************************************************/
    @Override
    public String nativeSQL(String sql) throws SQLException {
        return inner.nativeSQL(sql);
    }

    @Override
    public void commit() throws SQLException {
        inner.commit();
    }

    @Override
    public void rollback() throws SQLException {
        inner.rollback();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        inner.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return inner.getAutoCommit();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return inner.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        inner.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return inner.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        inner.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return inner.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        inner.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return inner.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return inner.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        inner.clearWarnings();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return inner.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        inner.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        inner.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return inner.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return inner.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return inner.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        inner.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        inner.releaseSavepoint(savepoint);
    }

    @Override
    public Clob createClob() throws SQLException {
        return inner.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return inner.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return inner.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return inner.createSQLXML();
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        inner.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        inner.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return inner.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return inner.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return inner.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return inner.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        inner.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return inner.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        inner.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        inner.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return inner.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return inner.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return inner.isWrapperFor(iface);
    }
}

package com.dci.intellij.dbn.language.sql.dialect.oracle;

import org.jetbrains.annotations.NotNull;

import com.dci.intellij.dbn.language.sql.SQLSyntaxHighlighter;
import com.dci.intellij.dbn.language.sql.dialect.SQLLanguageDialect;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.FlexLexer;
import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.Lexer;

public class OracleSQLHighlighter extends SQLSyntaxHighlighter {
    public OracleSQLHighlighter(SQLLanguageDialect languageDialect) {
        super(languageDialect, "oracle_sql_highlighter_tokens.xml");
    }

    @NotNull
    protected Lexer createLexer() {
        FlexLexer flexLexer = new OracleSQLHighlighterFlexLexer(getTokenTypes());
        return new LayeredLexer(new FlexAdapter(flexLexer));
    }
}
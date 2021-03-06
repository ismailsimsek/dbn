package com.dci.intellij.dbn.language.common.element.impl;

import com.dci.intellij.dbn.code.common.lookup.LookupItemBuilderProvider;
import com.dci.intellij.dbn.code.common.lookup.TokenLookupItemBuilder;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.TokenTypeCategory;
import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.ElementTypeBundle;
import com.dci.intellij.dbn.language.common.element.TokenElementTypeChain;
import com.dci.intellij.dbn.language.common.element.lookup.ElementLookupContext;
import com.dci.intellij.dbn.language.common.element.lookup.ElementTypeLookupCache;
import com.dci.intellij.dbn.language.common.element.lookup.TokenElementTypeLookupCache;
import com.dci.intellij.dbn.language.common.element.parser.ParserContext;
import com.dci.intellij.dbn.language.common.element.parser.impl.TokenElementTypeParser;
import com.dci.intellij.dbn.language.common.element.path.BasicPathNode;
import com.dci.intellij.dbn.language.common.element.path.PathNode;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dci.intellij.dbn.language.common.psi.TokenPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TokenElementType extends LeafElementType implements LookupItemBuilderProvider {
    public static final String SEPARATOR = "SEPARATOR";


    private TokenLookupItemBuilder lookupItemBuilder = new TokenLookupItemBuilder(this);
    private TokenTypeCategory flavor;
    private List<TokenElementTypeChain> possibleTokenChains;
    private String text;

    public TokenElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        String typeId = def.getAttributeValue("type-id");
        text = def.getAttributeValue("text");
        TokenType tokenType = bundle.getTokenTypeBundle().getTokenType(typeId);
        setTokenType(tokenType);
        setDefaultFormatting(tokenType.getFormatting());

        String flavorName = def.getAttributeValue("flavor");
        if (StringUtil.isNotEmpty(flavorName)) {
            flavor = TokenTypeCategory.getCategory(flavorName);
        }

        setDescription(tokenType.getValue() + " " + getTokenTypeCategory());
    }

    public TokenElementType(ElementTypeBundle bundle, ElementTypeBase parent, String typeId, String id) {
        super(bundle, parent, id, (String)null);
        TokenType tokenType = bundle.getTokenTypeBundle().getTokenType(typeId);
        setTokenType(tokenType);
        setDescription(tokenType.getValue() + " " + getTokenTypeCategory());

        setDefaultFormatting(tokenType.getFormatting());
    }

    @Nullable
    public String getText() {
        return text;
    }

    @Override
    public TokenElementTypeLookupCache createLookupCache() {
        return new TokenElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    public TokenElementTypeParser createParser() {
        return new TokenElementTypeParser(this);
    }

    @Override
    public String getDebugName() {
        return "token (" + getId() + " - " + tokenType.getId() + ")";
    }

    @Override
    public Set<LeafElementType> getNextPossibleLeafs(PathNode pathNode, @NotNull ElementLookupContext context) {
        ElementType parent = getParent();
        if (isIterationSeparator()) {
            if (parent instanceof IterationElementType) {
                IterationElementType iterationElementType = (IterationElementType) parent;
                ElementTypeLookupCache lookupCache = iterationElementType.iteratedElementType.lookupCache;
                return lookupCache.collectFirstPossibleLeafs(context.reset());
            } else if (parent instanceof QualifiedIdentifierElementType){
                return super.getNextPossibleLeafs(pathNode, context);
            }
        }
        if (parent instanceof WrapperElementType) {
            WrapperElementType wrapperElementType = (WrapperElementType) parent;
            if (this.equals(wrapperElementType.getBeginTokenElement())) {
                ElementTypeLookupCache lookupCache = wrapperElementType.wrappedElement.lookupCache;
                return lookupCache.collectFirstPossibleLeafs(context.reset());
            }
        }

        return super.getNextPossibleLeafs(pathNode, context);
    }

    @Override
    public Set<LeafElementType> getNextRequiredLeafs(PathNode pathNode, ParserContext context) {
        if (isIterationSeparator()) {
            if (getParent() instanceof IterationElementType) {
                IterationElementType iterationElementType = (IterationElementType) getParent();
                return iterationElementType.iteratedElementType.lookupCache.getFirstRequiredLeafs();
            } else if (getParent() instanceof QualifiedIdentifierElementType){
                return super.getNextRequiredLeafs(pathNode, context);
            }
        }
        return super.getNextRequiredLeafs(pathNode, context);
    }

    public boolean isIterationSeparator() {
        return getId().equals(SEPARATOR);
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public boolean isIdentifier() {
        return false;
    }

    @Override
    public boolean isSameAs(LeafElementType elementType) {
        if (elementType instanceof TokenElementType) {
            TokenElementType token = (TokenElementType) elementType;
            return token.tokenType == tokenType;
        }
        return false;
    }


    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new TokenPsiElement(astNode, this);
    }

    public String toString() {
        return tokenType.getId() + " (" + getId() + ")";
    }

    public boolean isCharacter() {
        return tokenType.isCharacter();
    }

    @Override
    public TokenLookupItemBuilder getLookupItemBuilder(DBLanguage language) {
        return lookupItemBuilder;
    }

    public TokenTypeCategory getFlavor() {
        return flavor;
    }

    public TokenTypeCategory getTokenTypeCategory() {
        return flavor == null ? tokenType.getCategory() : flavor;
    }

    public List<TokenElementTypeChain> getPossibleTokenChains() {
        if (possibleTokenChains == null) {
            possibleTokenChains = new ArrayList<TokenElementTypeChain>();
            TokenElementTypeChain stump = new TokenElementTypeChain(this);
            buildPossibleChains(this, stump);
        }
        return possibleTokenChains;
    }

    private void buildPossibleChains(TokenElementType tokenElementType, TokenElementTypeChain chain) {
        PathNode pathNode = BasicPathNode.buildPathUp(tokenElementType);
        Set<LeafElementType> nextPossibleLeafs = getNextPossibleLeafs(pathNode, new ElementLookupContext());
        if (nextPossibleLeafs != null) {
            for (LeafElementType nextPossibleLeaf : nextPossibleLeafs) {
                if (nextPossibleLeaf instanceof TokenElementType) {
                    TokenElementType nextTokenElementType = (TokenElementType) nextPossibleLeaf;
                    if (nextTokenElementType.tokenType.isKeyword()) {
                        TokenElementTypeChain tokenElementTypeChain = chain.createVariant(nextTokenElementType);
                        if (possibleTokenChains == null) possibleTokenChains = new ArrayList<>();
                        possibleTokenChains.add(tokenElementTypeChain);
                        if (tokenElementTypeChain.getElementTypes().size()<3) {
                            buildPossibleChains(nextTokenElementType, tokenElementTypeChain);
                        }
                    }
                }
            }
        }
    }
}

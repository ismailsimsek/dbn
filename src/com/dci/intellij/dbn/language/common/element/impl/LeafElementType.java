package com.dci.intellij.dbn.language.common.element.impl;

import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.element.ChameleonElementType;
import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.ElementTypeBundle;
import com.dci.intellij.dbn.language.common.element.lookup.ElementLookupContext;
import com.dci.intellij.dbn.language.common.element.lookup.ElementTypeLookupCache;
import com.dci.intellij.dbn.language.common.element.parser.ParserContext;
import com.dci.intellij.dbn.language.common.element.path.BasicPathNode;
import com.dci.intellij.dbn.language.common.element.path.ParsePathNode;
import com.dci.intellij.dbn.language.common.element.path.PathNode;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiWhiteSpace;
import gnu.trove.THashSet;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public abstract class LeafElementType extends ElementTypeBase {
    public TokenType tokenType;

    private boolean optional;

    LeafElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
    }

    LeafElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, String description) {
        super(bundle, parent, id, description);
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public TokenType getTokenType() {
        return tokenType;
    }

    public void registerLeaf() {
        lookupCache.init();
        getParent().lookupCache.registerLeaf(this, this);
    }

    public abstract boolean isSameAs(LeafElementType elementType);

    public abstract boolean isIdentifier();

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    public static ElementType getPreviousElement(BasicPathNode pathNode) {
        int position = 0;
        while (pathNode != null) {
            ElementType elementType = pathNode.elementType;
            if (elementType instanceof SequenceElementType) {
                SequenceElementType sequenceElementType = (SequenceElementType) elementType;
                if (position > 0 ) {
                    return sequenceElementType.getChild(position-1).elementType;
                }
            }
            position = pathNode.getIndexInParent();
            pathNode = pathNode.parent;
        }
        return null;
    }

    public Set<LeafElementType> getNextPossibleLeafs(PathNode pathNode, @NotNull ElementLookupContext context) {
        Set<LeafElementType> possibleLeafs = new THashSet<LeafElementType>();
        int position = 1;
        while (pathNode != null) {
            ElementType elementType = pathNode.getElementType();

            if (elementType instanceof SequenceElementType) {
                SequenceElementType sequenceElementType = (SequenceElementType) elementType;

                int elementsCount = sequenceElementType.getChildCount();

                if (position < elementsCount) {
                    ElementTypeRef child = sequenceElementType.getChild(position);
                    while (child != null) {
                        if (context.check(child)) {
                            child.getLookupCache().collectFirstPossibleLeafs(context.reset(), possibleLeafs);
                            if (!child.optional) {
                                pathNode = null;
                                break;
                            }
                        }
                        child = child.getNext();
                    }
                } else if (elementType instanceof NamedElementType){
                    context.removeBranchMarkers((NamedElementType) elementType);
                }
            } else if (elementType instanceof IterationElementType) {
                IterationElementType iterationElementType = (IterationElementType) elementType;
                TokenElementType[] separatorTokens = iterationElementType.separatorTokens;
                if (separatorTokens == null) {
                    ElementTypeLookupCache lookupCache = iterationElementType.iteratedElementType.lookupCache;
                    lookupCache.collectFirstPossibleLeafs(context.reset(), possibleLeafs);
                } else {
                    possibleLeafs.addAll(Arrays.asList(separatorTokens));
                }
            } else if (elementType instanceof QualifiedIdentifierElementType) {
                QualifiedIdentifierElementType qualifiedIdentifierElementType = (QualifiedIdentifierElementType) elementType;
                if (this == qualifiedIdentifierElementType.getSeparatorToken()) {
                    break;
                }
            } else if (elementType instanceof ChameleonElementType) {
                ChameleonElementType chameleonElementType = (ChameleonElementType) elementType;
                ElementTypeBundle elementTypeBundle = chameleonElementType.getParentLanguage().getParserDefinition().getParser().getElementTypes();;
                ElementTypeLookupCache lookupCache = elementTypeBundle.getRootElementType().lookupCache;
                possibleLeafs.addAll(lookupCache.getFirstPossibleLeafs());
            }
            if (pathNode != null) {
                ElementType pathElementType = pathNode.getElementType();
                if (pathElementType != null && pathElementType.is(ElementTypeAttribute.STATEMENT) && context.isBreakOnAttribute(ElementTypeAttribute.STATEMENT)){
                    break;
                }
                position = pathNode.getIndexInParent() + 1;
                pathNode = pathNode.getParent();
            }
        }
        return possibleLeafs;
    }

    public boolean isNextPossibleToken(TokenType tokenType, ParsePathNode pathNode, ParserContext context) {
        return isNextToken(tokenType, pathNode, context, false);
    }

    public boolean isNextRequiredToken(TokenType tokenType, ParsePathNode pathNode, ParserContext context) {
        return isNextToken(tokenType, pathNode, context, true);
    }

    private boolean isNextToken(TokenType tokenType, ParsePathNode pathNode, ParserContext context, boolean required) {
        int position = -1;
        while (pathNode != null) {
            ElementType elementType = pathNode.getElementType();

            if (elementType instanceof SequenceElementType) {
                SequenceElementType sequenceElementType = (SequenceElementType) elementType;

                int elementsCount = sequenceElementType.getChildCount();
                if (position == -1) {
                    position = pathNode.getCursorPosition() + 1;
                }

                //int position = sequenceElementType.indexOf(this) + 1;
/*
                int position = pathNode.getCursorPosition();
                if (pathNode.getCurrentOffset() < context.getBuilder().getCurrentOffset()) {
                    position++;
                }
*/
                if (position < elementsCount) {
                    ElementTypeRef child = sequenceElementType.getChild(position);
                    while (child != null) {
                        ElementTypeLookupCache lookupCache = child.getLookupCache();
                        if (required) {
                            if (lookupCache.isFirstRequiredToken(tokenType) && !child.optional) {
                                return true;
                            }
                        } else {
                            if (lookupCache.isFirstPossibleToken(tokenType)) {
                                return true;
                            }
                        }

                        if (!child.optional && !child.isOptionalFromHere()) {
                            return false;
                        }
                        child = child.getNext();
                    }
                }
            } else if (elementType instanceof IterationElementType) {
                IterationElementType iterationElementType = (IterationElementType) elementType;
                TokenElementType[] separatorTokens = iterationElementType.separatorTokens;
                if (separatorTokens == null) {
                    ElementTypeLookupCache lookupCache = iterationElementType.iteratedElementType.lookupCache;
                    if (required ?
                            lookupCache.isFirstRequiredToken(tokenType) :
                            lookupCache.isFirstPossibleToken(tokenType)) {
                        return true;
                    }
                }
            } else if (elementType instanceof QualifiedIdentifierElementType) {
                QualifiedIdentifierElementType qualifiedIdentifierElementType = (QualifiedIdentifierElementType) elementType;
                if (this == qualifiedIdentifierElementType.getSeparatorToken()) {
                    break;
                }
            } else if (elementType instanceof WrapperElementType) {
                WrapperElementType wrapperElementType = (WrapperElementType) elementType;
                return wrapperElementType.getEndTokenElement().tokenType == tokenType;
            }

            position = pathNode.getIndexInParent() + 1;
            pathNode = pathNode.parent;
        }
        return false;
    }

    public Set<LeafElementType> getNextRequiredLeafs(PathNode pathNode, ParserContext context) {
        Set<LeafElementType> requiredLeafs = new THashSet<>();
        int position = 0;
        while (pathNode != null) {
            ElementType elementType = pathNode.getElementType();

            if (elementType instanceof SequenceElementType) {
                SequenceElementType sequenceElementType = (SequenceElementType) elementType;

                ElementTypeRef child = sequenceElementType.getChild(position + 1);
                while (child != null) {
                    if (!child.optional) {
                        ElementTypeLookupCache lookupCache = child.getLookupCache();
                        requiredLeafs.addAll(lookupCache.getFirstRequiredLeafs());
                        pathNode = null;
                        break;
                    }
                    child = child.getNext();
                }
            } else if (elementType instanceof IterationElementType) {
                IterationElementType iteration = (IterationElementType) elementType;
                TokenElementType[] separatorTokens = iteration.separatorTokens;
                Collections.addAll(requiredLeafs, separatorTokens);
            }
            if (pathNode != null) {
                position = pathNode.getIndexInParent();
                pathNode = pathNode.getParent();
            }
        }
        return requiredLeafs;
    }


    /**
     * Returns the index of the corresponding ElementType in it's parent
     * Only applicable if the given astNode is corresponding to an ElementType within a SequenceElementType
     * For all the other cases it returns 0.
     */
    private static int getElementTypeIndex(ASTNode astNode){
        ASTNode parentAstNode = astNode.getTreeParent();
        if (parentAstNode.getElementType() instanceof SequenceElementType) {
            SequenceElementType sequenceElementType = (SequenceElementType) parentAstNode.getElementType();
            int index = 0;
            ASTNode child = parentAstNode.getFirstChildNode();
            while (child != null) {
                if (astNode == child) {
                    break;
                }
                index++;
                child = child.getTreeNext();
                if (child instanceof PsiWhiteSpace){
                    child = child.getTreeNext();
                }
            }
            return sequenceElementType.indexOf((ElementType) astNode.getElementType(), index);
        }
        return 0;
    }
}

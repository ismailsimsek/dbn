package com.dci.intellij.dbn.language.psql;

import com.dci.intellij.dbn.language.common.DBLanguagePsiFile;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.common.psi.lookup.ObjectDefinitionLookupAdapter;
import com.dci.intellij.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class PSQLFile extends DBLanguagePsiFile {

    PSQLFile(FileViewProvider fileViewProvider, @NotNull PSQLLanguage language) {
        super(fileViewProvider, PSQLFileType.INSTANCE, language);
    }

    public BasePsiElement lookupObjectSpecification(DBObjectType objectType, CharSequence objectName) {
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                PsiLookupAdapter lookupAdapter = new ObjectDefinitionLookupAdapter(null, objectType, objectName, ElementTypeAttribute.SUBJECT);
                BasePsiElement specObject = lookupAdapter.findInScope(basePsiElement);
                if (specObject != null) {
                    return specObject.findEnclosingPsiElement(ElementTypeAttribute.OBJECT_SPECIFICATION);
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    public BasePsiElement lookupObjectDeclaration(DBObjectType objectType, CharSequence objectName) {
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                PsiLookupAdapter lookupAdapter = new ObjectDefinitionLookupAdapter(null, objectType, objectName, ElementTypeAttribute.SUBJECT);
                BasePsiElement specObject = lookupAdapter.findInScope(basePsiElement);
                if (specObject != null) {
                    return specObject.findEnclosingPsiElement(ElementTypeAttribute.OBJECT_DECLARATION);
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }
}

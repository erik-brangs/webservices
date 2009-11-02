/*******************************************************************************
 * Copyright (c) 2008 IONA Technologies PLC, Shane Clarke
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IONA Technologies PLC - initial API and implementation
 *    Shane Clarke - Rewrote API
 *******************************************************************************/
package org.eclipse.jst.ws.annotations.core.utils;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jst.ws.annotations.core.AnnotationDefinition;
import org.eclipse.jst.ws.annotations.core.AnnotationsCorePlugin;
import org.eclipse.jst.ws.annotations.core.AnnotationsManager;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeElementDeclaration;
import com.sun.mirror.declaration.AnnotationValue;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.ParameterDeclaration;

/**
 * Utility class for adding, removing and updating annotations and member value pairs.
 * <p>
 * <strong>Provisional API:</strong> This class/interface is part of an interim API that is still under
 * development and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code that uses
 * this API will almost certainly be broken (repeatedly) as the API evolves.
 * </p>
 */
public final class AnnotationUtils {

    private AnnotationUtils() {
    }

    public static void addImport(IJavaElement javaElement, String qualifiedName) throws CoreException {
        TextFileChange change = new TextFileChange("Add Import", (IFile) javaElement.getResource());
        MultiTextEdit multiTextEdit = new MultiTextEdit();
        change.setEdit(multiTextEdit);

        TextEdit annotationEdit = AnnotationUtils.createAddImportTextEdit(javaElement, qualifiedName);
        change.addEdit(annotationEdit);
        applyChange(null, change);
    }

    public static void removeImport(IJavaElement javaElement, String qualifiedName) throws CoreException {
        TextFileChange change = new TextFileChange("Remove Import", (IFile) javaElement.getResource());
        MultiTextEdit multiTextEdit = new MultiTextEdit();
        change.setEdit(multiTextEdit);

        TextEdit annotationEdit = AnnotationUtils.createRemoveImportTextEdit(javaElement, qualifiedName);
        change.addEdit(annotationEdit);
        applyChange(null, change);
    }

    public static void addAnnotation(IJavaElement javaElement, Annotation annotation) throws CoreException {
        TextFileChange change = new TextFileChange("Add annotation", (IFile) javaElement.getResource());
        MultiTextEdit multiTextEdit = new MultiTextEdit();
        change.setEdit(multiTextEdit);

        TextEdit annotationEdit = AnnotationUtils.createAddAnnotationTextEdit(javaElement, annotation);
        change.addEdit(annotationEdit);
        applyChange(null, change);
    }

    public static void removeAnnotation(IJavaElement javaElement, Annotation annotation) throws CoreException {
        TextFileChange change = new TextFileChange("Remove annotation", (IFile) javaElement.getResource());
        MultiTextEdit multiTextEdit = new MultiTextEdit();
        change.setEdit(multiTextEdit);

        TextEdit textEdit = AnnotationUtils.createRemoveAnnotationTextEdit(javaElement, annotation);
        change.addEdit(textEdit);
        applyChange(null, change);
    }

    public static void addMemberValuePair(NormalAnnotation annotation, MemberValuePair memberValuePair) throws CoreException {
        if (annotation.getRoot() instanceof CompilationUnit) {
            CompilationUnit compilationUnit = (CompilationUnit) annotation.getRoot();
            TextFileChange change = new TextFileChange("Add Member Value Pair", (IFile) compilationUnit.getJavaElement().getResource());
            MultiTextEdit multiTextEdit = new MultiTextEdit();
            change.setEdit(multiTextEdit);

            TextEdit annotationEdit = AnnotationUtils.createAddMemberValuePairTextEdit(annotation, memberValuePair);
            change.addEdit(annotationEdit);
            applyChange(null, change);
        }
    }

    public static void removeMemberValuePair(NormalAnnotation annotation, MemberValuePair memberValuePair) throws CoreException {
        if (annotation.getRoot() instanceof CompilationUnit) {
            CompilationUnit compilationUnit = (CompilationUnit) annotation.getRoot();
            TextFileChange change = new TextFileChange("Remove Member Value Pair", (IFile) compilationUnit.getJavaElement().getResource());
            MultiTextEdit multiTextEdit = new MultiTextEdit();
            change.setEdit(multiTextEdit);

            TextEdit annotationEdit = AnnotationUtils.createRemoveMemberValuePairTextEdit(annotation, memberValuePair);
            change.addEdit(annotationEdit);
            applyChange(null, change);
        }
    }

    public static void updateMemberValuePair(MemberValuePair memberValuePair, ASTNode value) throws CoreException {
        if (memberValuePair.getRoot() instanceof CompilationUnit) {
            CompilationUnit compilationUnit = (CompilationUnit) memberValuePair.getRoot();
            TextFileChange change = new TextFileChange("Update Member Value Pair", (IFile) compilationUnit.getJavaElement().getResource());
            MultiTextEdit multiTextEdit = new MultiTextEdit();
            change.setEdit(multiTextEdit);

            TextEdit annotationEdit = AnnotationUtils.createUpdateMemberValuePairTextEdit(memberValuePair, value);
            change.addEdit(annotationEdit);
            applyChange(null, change);
        }
    }

    public static void updateSingleMemberAnnotation(SingleMemberAnnotation annotation, ASTNode value)  throws CoreException {
        if (annotation.getRoot() instanceof CompilationUnit) {
            CompilationUnit compilationUnit = (CompilationUnit) annotation.getRoot();
            TextFileChange change = new TextFileChange("Update Single Member Annotation", (IFile) compilationUnit.getJavaElement().getResource());
            MultiTextEdit multiTextEdit = new MultiTextEdit();
            change.setEdit(multiTextEdit);

            TextEdit annotationEdit = AnnotationUtils.createUpdateSingleMemberAnnotationTextEdit(annotation, value);
            change.addEdit(annotationEdit);
            applyChange(null, change);
        }
    }

    public static TextEdit createAddImportTextEdit(IJavaElement javaElement, String qualifiedName) throws CoreException {
        CompilationUnit compilationUnit = SharedASTProvider.getAST(getCompilationUnitFromJavaElement(javaElement), SharedASTProvider.WAIT_YES, null);
        ImportRewrite importRewrite = CodeStyleConfiguration.createImportRewrite(compilationUnit, true);
        importRewrite.addImport(qualifiedName);
        return importRewrite.rewriteImports(null);
    }

    @SuppressWarnings("unchecked")
    public static TextEdit createRemoveImportTextEdit(IJavaElement javaElement, String qualifiedName) throws CoreException {
        CompilationUnit compilationUnit = SharedASTProvider.getAST(getCompilationUnitFromJavaElement(javaElement), SharedASTProvider.WAIT_YES, null);
        ImportRewrite importRewrite = CodeStyleConfiguration.createImportRewrite(compilationUnit, true);
        final String annotationSimpleName = qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
        final List<String> occurences = new ArrayList<String>();
        AnnotationDefinition annotationDefinition = AnnotationsManager.getAnnotationDefinitionForClass(qualifiedName);
        List<ElementType> elementTypes = Collections.emptyList();
        if (annotationDefinition != null) {
            elementTypes = annotationDefinition.getTargets();
        }
        for (ElementType elementType : elementTypes) {
            if (elementType == ElementType.PACKAGE) {
                compilationUnit.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(PackageDeclaration packageDeclaration) {
                        countAnnotationOccurrences(packageDeclaration.annotations(),
                                annotationSimpleName, occurences);
                        return false;
                    }
                });
            }
            if (elementType == ElementType.TYPE) {
                compilationUnit.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(TypeDeclaration typeDeclaration) {
                        countAnnotationOccurrences(typeDeclaration.modifiers(),
                                annotationSimpleName, occurences);
                        return false;
                    }
                });
            }
            if (elementType == ElementType.FIELD) {
                compilationUnit.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(FieldDeclaration fieldDeclaration) {
                        countAnnotationOccurrences(
                                fieldDeclaration.modifiers(),
                                annotationSimpleName, occurences);
                        return false;
                    }
                });
            }
            if (elementType == ElementType.METHOD) {
                compilationUnit.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration methodDeclaration) {
                        countAnnotationOccurrences(methodDeclaration
                                .modifiers(), annotationSimpleName, occurences);
                        return false;
                    }
                });
            }
            if (elementType == ElementType.PARAMETER) {
                compilationUnit.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(
                            SingleVariableDeclaration singleVariableDeclaration) {
                        countAnnotationOccurrences(singleVariableDeclaration
                                .modifiers(), annotationSimpleName, occurences);
                        return false;
                    }
                });
            }
        }
        if (occurences.size() == 1) {
            importRewrite.removeImport(qualifiedName);
        }
        // TODO Cleanup imports. Repeatedly adding and removing an import
        // where none existed before will
        // insert a new line on each insert.
        return importRewrite.rewriteImports(null);
    }

    private static void countAnnotationOccurrences(List<IExtendedModifier> modifiers, String annotationSimpleName,
            List<String> occurences) {
        for (IExtendedModifier extendedModifier : modifiers) {
            if (extendedModifier instanceof Annotation) {
                Annotation existingAnnotation = (Annotation) extendedModifier;
                if (AnnotationUtils.getAnnotationName(existingAnnotation).equals(annotationSimpleName)) {
                    occurences.add(annotationSimpleName);
                }
            }
        }
    }

    public static TextEdit createAddAnnotationTextEdit(IJavaElement javaElement, Annotation annotation) throws CoreException {
        switch(javaElement.getElementType()) {
        case IJavaElement.PACKAGE_DECLARATION:
            return createAddAnnotationTextEdit((IPackageDeclaration) javaElement, annotation);
        case IJavaElement.TYPE:
            return createAddAnnotationTextEdit((IType) javaElement, annotation);
        case IJavaElement.FIELD:
            return createAddAnnotationTextEdit((IField) javaElement, annotation);
        case IJavaElement.METHOD:
            return createAddAnnotationTextEdit((IMethod) javaElement, annotation);
        case IJavaElement.LOCAL_VARIABLE:
            return createAddAnnotationTextEdit((ILocalVariable) javaElement, annotation);
        default:
            return new MultiTextEdit();
        }
    }

    public static TextEdit createRemoveAnnotationTextEdit(IJavaElement javaElement, Annotation annotation) throws CoreException {
        switch(javaElement.getElementType()) {
        case IJavaElement.PACKAGE_DECLARATION:
            return createRemoveAnnotationTextEdit((IPackageDeclaration) javaElement, annotation);
        case IJavaElement.TYPE:
            return createRemoveAnnotationTextEdit((IType) javaElement, annotation);
        case IJavaElement.FIELD:
            return createRemoveAnnotationTextEdit((IField) javaElement, annotation);
        case IJavaElement.METHOD:
            return createRemoveAnnotationTextEdit((IMethod) javaElement, annotation);
        case IJavaElement.LOCAL_VARIABLE:
            return createRemoveAnnotationTextEdit((ILocalVariable) javaElement, annotation);
        default:
            return new MultiTextEdit();
        }
    }

    private static void applyChange(IProgressMonitor monitor, Change change) {
        if (change == null) {
            return;
        }

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        IUndoManager manager = RefactoringCore.getUndoManager();
        boolean successful = false;
        Change undoChange = null;
        try {
            change.initializeValidationData(monitor);
            RefactoringStatus valid = change.isValid(monitor);
            if (valid.isOK()) {
                manager.aboutToPerformChange(change);
                undoChange = change.perform(monitor);
                successful = true;
            }
        } catch (CoreException ce) {
            AnnotationsCorePlugin.log(ce.getStatus());
        } finally {
            manager.changePerformed(change, successful);
        }
        if (undoChange != null) {
            undoChange.initializeValidationData(monitor);
            manager.addUndo(undoChange.getName(), undoChange);
        }
    }

    private static TextEdit createAddAnnotationTextEdit(IPackageDeclaration packageDeclaration, Annotation annotation) throws CoreException {
        if (packageDeclaration != null && !isAnnotationPresent(packageDeclaration, AnnotationUtils.getAnnotationName(annotation))) {
            ICompilationUnit source = getCompilationUnitFromJavaElement(packageDeclaration);
            CompilationUnit compilationUnit = SharedASTProvider.getAST(source, SharedASTProvider.WAIT_YES, null);
            ASTRewrite rewriter = ASTRewrite.create(compilationUnit.getAST());

            ListRewrite listRewrite = rewriter.getListRewrite(compilationUnit.getPackage(), PackageDeclaration.ANNOTATIONS_PROPERTY);

            listRewrite.insertFirst(annotation, null);

            return rewriter.rewriteAST();
        }
        return new MultiTextEdit();
    }

    private static TextEdit createRemoveAnnotationTextEdit(IPackageDeclaration packageDeclaration, Annotation annotation) throws CoreException {
        if (packageDeclaration != null && isAnnotationPresent(packageDeclaration, getAnnotationName(annotation))) {
            ICompilationUnit source = getCompilationUnitFromJavaElement(packageDeclaration);
            CompilationUnit compilationUnit = SharedASTProvider.getAST(source, SharedASTProvider.WAIT_YES, null);
            ASTRewrite rewriter = ASTRewrite.create(compilationUnit.getAST());

            PackageDeclaration pkgDeclaration = compilationUnit.getPackage();

            ListRewrite listRewrite = rewriter.getListRewrite(pkgDeclaration, PackageDeclaration.ANNOTATIONS_PROPERTY);

            @SuppressWarnings("unchecked")
            List originalList = listRewrite.getOriginalList();
            for (Object object : originalList) {
                if (object instanceof Annotation && compareAnnotationNames((Annotation) object, annotation)) {
                    listRewrite.remove((Annotation) object, null);
                }
            }
            return rewriter.rewriteAST();
        }
        return new MultiTextEdit();
    }

    private static TextEdit createAddAnnotationTextEdit(IType type, Annotation annotation) throws CoreException {
        AbstractTypeDeclaration typeDeclaration = getTypeDeclaration(type);
        if(typeDeclaration != null && !isAnnotationPresent(type, annotation)) {
            ASTRewrite rewriter = ASTRewrite.create(typeDeclaration.getAST());

            ListRewrite listRewrite = rewriter.getListRewrite(typeDeclaration, getChildListPropertyDescriptorForType(typeDeclaration));

            listRewrite.insertFirst(annotation, null);

            return rewriter.rewriteAST();
        }
        return new MultiTextEdit();
    }

    private static TextEdit createRemoveAnnotationTextEdit(IType type, Annotation annotation) throws CoreException {
        AbstractTypeDeclaration typeDeclaration = getTypeDeclaration(type);
        if (typeDeclaration != null && isAnnotationPresent(type, annotation)) {
            ASTRewrite rewriter = ASTRewrite.create(typeDeclaration.getAST());

            ListRewrite listRewrite = rewriter.getListRewrite(typeDeclaration, getChildListPropertyDescriptorForType(typeDeclaration));

            @SuppressWarnings("unchecked")
            List originalList = listRewrite.getOriginalList();
            for (Object object : originalList) {
                if (object instanceof Annotation && compareAnnotationNames((Annotation)object, annotation)) {
                    listRewrite.remove((Annotation)object, null);
                }
            }
            return rewriter.rewriteAST();
        }
        return new MultiTextEdit();
    }

    private static TextEdit createAddAnnotationTextEdit(IMethod method, Annotation annotation) throws CoreException {
        MethodDeclaration methodDeclaration = getMethodDeclaration(method);
        if (methodDeclaration != null && !isAnnotationPresent(method, annotation)) {
            ASTRewrite rewriter = ASTRewrite.create(methodDeclaration.getAST());

            ListRewrite listRewrite = rewriter.getListRewrite(methodDeclaration,  MethodDeclaration.MODIFIERS2_PROPERTY);

            listRewrite.insertAt(annotation, 0, null);

            return rewriter.rewriteAST();
        }
        return new MultiTextEdit();
    }

    private static TextEdit createRemoveAnnotationTextEdit(IMethod method, Annotation annotation) throws CoreException {
        MethodDeclaration methodDeclaration = getMethodDeclaration(method);
        if (methodDeclaration != null && isAnnotationPresent(method, annotation)) {
            ASTRewrite rewriter = ASTRewrite.create(methodDeclaration.getAST());

            ListRewrite listRewrite = rewriter.getListRewrite(methodDeclaration,  MethodDeclaration.MODIFIERS2_PROPERTY);

            @SuppressWarnings("unchecked")
            List originalList = listRewrite.getOriginalList();
            for (Object object : originalList) {
                if (object instanceof Annotation && compareAnnotationNames((Annotation) object, annotation)) {
                    listRewrite.remove((Annotation) object, null);
                }
            }
            return rewriter.rewriteAST();
        }
        return new MultiTextEdit();
    }

    private static TextEdit createAddAnnotationTextEdit(IField field, Annotation annotation) throws CoreException {
        FieldDeclaration fieldDeclaration = getFieldDeclaration(field);
        if (fieldDeclaration != null && !isAnnotationPresent(field, annotation)) {

            ASTRewrite rewriter = ASTRewrite.create(fieldDeclaration.getAST());

            ListRewrite listRewrite = rewriter.getListRewrite(fieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);
            listRewrite.insertAt(annotation, 0, null);

            return rewriter.rewriteAST();
        }

        return new MultiTextEdit();
    }

    private static TextEdit createRemoveAnnotationTextEdit(IField field, Annotation annotation) throws CoreException {
        FieldDeclaration fieldDeclaration = getFieldDeclaration(field);
        if (fieldDeclaration != null && isAnnotationPresent(field, annotation)) {
            ASTRewrite rewriter = ASTRewrite.create(fieldDeclaration.getAST());

            ListRewrite listRewrite = rewriter.getListRewrite(fieldDeclaration, FieldDeclaration.MODIFIERS2_PROPERTY);

            @SuppressWarnings("unchecked")
            List originalList = listRewrite.getOriginalList();
            for (Object object : originalList) {
                if (object instanceof Annotation && compareAnnotationNames((Annotation) object, annotation)) {
                    listRewrite.remove((Annotation) object, null);
                }
            }
            return rewriter.rewriteAST();
        }
        return new MultiTextEdit();
    }

    private static TextEdit createAddAnnotationTextEdit(ILocalVariable methodParameter, Annotation annotation) throws CoreException {
        SingleVariableDeclaration parameter = getSingleVariableDeclaration(methodParameter);
        if (parameter != null && !isAnnotationPresent(methodParameter, AnnotationUtils.getAnnotationName(annotation))) {
            ASTRewrite rewriter = ASTRewrite.create(parameter.getAST());

            ListRewrite listRewrite = rewriter.getListRewrite(parameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);

            listRewrite.insertAt(annotation, -1, null);

            return rewriter.rewriteAST();
        }
        return new MultiTextEdit();
    }

    private static TextEdit createRemoveAnnotationTextEdit(ILocalVariable methodParameter, Annotation annotation) throws CoreException {
        SingleVariableDeclaration parameter = getSingleVariableDeclaration(methodParameter);
        if (isAnnotationPresent(methodParameter, AnnotationUtils.getAnnotationName(annotation))) {
            ASTRewrite rewriter = ASTRewrite.create(parameter.getAST());

            ListRewrite listRewrite = rewriter.getListRewrite(parameter, SingleVariableDeclaration.MODIFIERS2_PROPERTY);

            @SuppressWarnings("unchecked")
            List originalList = listRewrite.getOriginalList();
            for (Object object : originalList) {
                if (object instanceof Annotation && compareAnnotationNames((Annotation)object, annotation)) {
                    listRewrite.remove((Annotation)object, null);
                }
            }

            return rewriter.rewriteAST();
        }
        return new MultiTextEdit();
    }

    public static TextEdit createAddMemberValuePairTextEdit(NormalAnnotation annotation, MemberValuePair memberValuePair) throws CoreException {
        ASTRewrite rewriter = ASTRewrite.create(annotation.getAST());

        ListRewrite listRewrite = rewriter.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);

        listRewrite.insertLast(memberValuePair, null);

        return rewriter.rewriteAST();
    }

    public static TextEdit createRemoveMemberValuePairTextEdit(NormalAnnotation annotation, MemberValuePair memberValuePair) throws CoreException {
        ASTRewrite rewriter = ASTRewrite.create(annotation.getAST());

        ListRewrite listRewrite = rewriter.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);

        @SuppressWarnings("unchecked")
        List originalList = listRewrite.getOriginalList();
        for (Object object : originalList) {
            if (object instanceof MemberValuePair) {
                MemberValuePair mvp = (MemberValuePair) object;
                if (mvp.getName().getIdentifier().equals(memberValuePair.getName().getIdentifier())) {
                    listRewrite.remove(mvp, null);
                }
            }
        }
        return rewriter.rewriteAST();
    }

    public static TextEdit createUpdateMemberValuePairTextEdit(MemberValuePair memberValuePair, ASTNode value) throws CoreException {
        ASTRewrite rewriter = ASTRewrite.create(memberValuePair.getAST());

        rewriter.set(memberValuePair, MemberValuePair.VALUE_PROPERTY, value, null);

        return rewriter.rewriteAST();
    }

    public static TextEdit createUpdateSingleMemberAnnotationTextEdit(SingleMemberAnnotation annotation, ASTNode value) throws CoreException {
        ASTRewrite rewriter = ASTRewrite.create(annotation.getAST());

        rewriter.set(annotation, SingleMemberAnnotation.VALUE_PROPERTY, value, null);

        return rewriter.rewriteAST();
    }

    public static ICompilationUnit getCompilationUnitFromJavaElement(IJavaElement javaElement) {
        switch(javaElement.getElementType()) {
        case IJavaElement.COMPILATION_UNIT:
            return (ICompilationUnit) javaElement;
        case IJavaElement.PACKAGE_DECLARATION:
            IPackageDeclaration packageDeclaration = (IPackageDeclaration) javaElement;
            return (ICompilationUnit) packageDeclaration.getParent();
        case IJavaElement.TYPE:
            IType type = (IType) javaElement;
            return type.getCompilationUnit();
        case IJavaElement.METHOD:
            IMethod method = (IMethod) javaElement;
            return method.getCompilationUnit();
        case IJavaElement.FIELD:
            IField field = (IField) javaElement;
            return field.getCompilationUnit();
        case IJavaElement.LOCAL_VARIABLE:
            ILocalVariable localVariable = (ILocalVariable) javaElement;
            if (localVariable.getParent() instanceof IMethod) {
                return getCompilationUnitFromJavaElement(localVariable.getParent());
            }
        default:
            return JavaCore.createCompilationUnitFrom((IFile) javaElement.getResource());
        }
    }

    @SuppressWarnings("unchecked")
    public static AbstractTypeDeclaration getTypeDeclaration(IType type) {
        CompilationUnit compilationUnit = SharedASTProvider.getAST(type.getCompilationUnit(), SharedASTProvider.WAIT_YES, null);
        List<TypeDeclaration> types = compilationUnit.types();
        for (AbstractTypeDeclaration abstractTypeDeclaration : types) {
            if (compareTypeNames(abstractTypeDeclaration, type)) {
                return abstractTypeDeclaration;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static MethodDeclaration getMethodDeclaration(IMethod method) {
        AbstractTypeDeclaration typeDeclaration = getTypeDeclaration(method.getDeclaringType());
        if (typeDeclaration != null) {
            List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
            for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
                if (bodyDeclaration instanceof MethodDeclaration) {
                    MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
                    if (compareMethods(methodDeclaration, method)) {
                        return methodDeclaration;
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static FieldDeclaration getFieldDeclaration(IField field) {
        AbstractTypeDeclaration typeDeclaration = getTypeDeclaration(field.getDeclaringType());
        if (typeDeclaration != null) {
            List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
            for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
                if (bodyDeclaration instanceof FieldDeclaration) {
                    FieldDeclaration fieldDeclaration = (FieldDeclaration) bodyDeclaration;
                    if (compareFieldNames(fieldDeclaration, field)) {
                        return fieldDeclaration;
                    }
                }
            }
        }
        return null;
    }

    public static SingleVariableDeclaration getSingleVariableDeclaration(ILocalVariable javaElement) {
        if (javaElement instanceof ILocalVariable && javaElement.getParent() instanceof IMethod) {
            ILocalVariable localVariable = javaElement;
            IMethod method = (IMethod) localVariable.getParent();
            MethodDeclaration methodDeclaration = getMethodDeclaration(method);

            @SuppressWarnings("unchecked")
            List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
            for (SingleVariableDeclaration singleVariableDeclaration : parameters) {
                if (singleVariableDeclaration.getName().getIdentifier().equals(localVariable.getElementName())) {
                    return singleVariableDeclaration;
                }
            }
        }
        return null;
    }

    private static ChildListPropertyDescriptor getChildListPropertyDescriptorForType(AbstractTypeDeclaration
            abstractTypeDeclaration) {
        ChildListPropertyDescriptor childListPropertyDescriptor = null;
        if (abstractTypeDeclaration instanceof TypeDeclaration) {
            childListPropertyDescriptor = TypeDeclaration.MODIFIERS2_PROPERTY;
        }
        if (abstractTypeDeclaration instanceof EnumDeclaration) {
            childListPropertyDescriptor = EnumDeclaration.MODIFIERS2_PROPERTY;
        }
        if (abstractTypeDeclaration instanceof AnnotationTypeDeclaration) {
            childListPropertyDescriptor = AnnotationTypeDeclaration.MODIFIERS2_PROPERTY;
        }
        return childListPropertyDescriptor;
    }

    /**
     * Returns the annotations name.
     * @param annotation
     * @return the annotation name
     */
    public static String getAnnotationName(Annotation annotation) {
        Name annotationTypeName = annotation.getTypeName();
        return annotationTypeName.getFullyQualifiedName();
    }

    public static boolean compareTypeNames(AbstractTypeDeclaration abstractTypeDeclaration, IType type) {
        return abstractTypeDeclaration.getName().getIdentifier().equals(type.getElementName());
    }

    @SuppressWarnings("unchecked")
    public static boolean compareMethods(MethodDeclaration methodDeclaration, IMethod method) {
        if (methodDeclaration.getName().getIdentifier().equals(method.getElementName())) {
            String[] parametetTypes = method.getParameterTypes();
            List<SingleVariableDeclaration> methodDeclarationParameters = methodDeclaration.parameters();
            if (parametetTypes.length == methodDeclarationParameters.size()) {
                for (int i = 0; i < parametetTypes.length; i++) {
                    String simpleName1 = Signature.toString(parametetTypes[i]);
                    String simpleName2 = methodDeclarationParameters.get(i).getType().toString();
                    if (!simpleName1.equals(simpleName2)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static boolean compareMethods(MethodDeclaration methodOne, MethodDeclaration methodTwo) {
        if (methodOne.getName().getIdentifier().equals(methodTwo.getName().getIdentifier())) {
            List<SingleVariableDeclaration> methodParametersOne = methodOne.parameters();
            List<SingleVariableDeclaration> methodParametersTwo = methodTwo.parameters();
            if (methodParametersOne.size() == methodParametersTwo.size()) {
                for (int i = 0; i < methodParametersOne.size(); i++) {
                    String simpleName1 = methodParametersOne.get(i).getType().toString();
                    String simpleName2 = methodParametersTwo.get(i).getType().toString();
                    if (!simpleName1.equals(simpleName2)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static boolean compareMethods(com.sun.mirror.declaration.MethodDeclaration methodOne,
            com.sun.mirror.declaration.MethodDeclaration methodTwo) {
        return compareMethodNames(methodOne, methodTwo) && compareMethodParameterTypes(methodOne, methodTwo);
    }

    private static boolean compareMethodNames(com.sun.mirror.declaration.MethodDeclaration methodOne,
            com.sun.mirror.declaration.MethodDeclaration methodTwo) {
        return methodOne.getSimpleName().equals(methodTwo.getSimpleName());
    }

    private static boolean compareMethodParameterTypes(com.sun.mirror.declaration.MethodDeclaration methodOne,
            com.sun.mirror.declaration.MethodDeclaration methodTwo) {
        int numberOfParametersOne = methodOne.getParameters().size();
        int numberOfParametersTwo = methodTwo.getParameters().size();

        if (numberOfParametersOne == numberOfParametersTwo) {
            List<ParameterDeclaration> parametersOne = (List<ParameterDeclaration>) methodOne.getParameters();
            List<ParameterDeclaration> parametersTwo = (List<ParameterDeclaration>) methodTwo.getParameters();
            for (int i = 0; i < parametersOne.size(); i++) {
                if (!parametersOne.get(i).getType().equals(parametersTwo.get(i).getType())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static boolean compareFieldNames(FieldDeclaration fieldDeclaration, IField field) {
        List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        for (VariableDeclarationFragment variableDeclarationFragment : fragments) {
            if (variableDeclarationFragment.getName().getIdentifier().equals(field.getElementName())) {
                return true;
            }
        }
        return false;
    }

    public static String accountForOverloadedMethods(IType type, IMethod method) throws JavaModelException {
        List<IMethod> methods =  Arrays.asList(type.getMethods());
        List<IMethod> similarMethods = new ArrayList<IMethod>();
        for (IMethod methodToTest : methods) {
            if (!method.equals(methodToTest) && method.getElementName().equalsIgnoreCase(
                    methodToTest.getElementName()) && methodToTest.getSourceRange().getOffset() <
                    method.getSourceRange().getOffset()) {
                similarMethods.add(methodToTest);
            }
        }
        return similarMethods.size() > 0 ? Integer.toString(similarMethods.size()) : ""; //$NON-NLS-1$
    }

    private static boolean compareAnnotationNames(Annotation newAnnotation, Annotation existingAnnotation) {
        return AnnotationUtils.getAnnotationName(existingAnnotation).equals(
                AnnotationUtils.getAnnotationName(newAnnotation));
    }

    public static boolean isAnnotationPresent(IJavaElement javaElement, Annotation annotation) {
        return AnnotationUtils.isAnnotationPresent(javaElement, AnnotationUtils.getAnnotationName(annotation));
    }

    public static boolean isAnnotationPresent(IJavaElement javaElement, String annotationName) {
        if (javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
            return isAnnotationPresent(((ICompilationUnit)javaElement).findPrimaryType(), annotationName);
        }

        int elementType = javaElement.getElementType();

        if (elementType == IJavaElement.PACKAGE_DECLARATION
                || elementType == IJavaElement.TYPE
                || elementType == IJavaElement.METHOD
                || elementType == IJavaElement.LOCAL_VARIABLE
                || elementType == IJavaElement.FIELD) {

            List<Annotation> annotations = getAnnotations(javaElement);
            for (Annotation annotation : annotations) {
                if (AnnotationUtils.getAnnotationName(annotation).equals(annotationName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static List<Annotation> getAnnotations(IJavaElement javaElement) {
        ICompilationUnit source = AnnotationUtils.getCompilationUnitFromJavaElement(javaElement);
        CompilationUnit compilationUnit = SharedASTProvider.getAST(source, SharedASTProvider.WAIT_YES, null);

        if (javaElement.getElementType() == IJavaElement.PACKAGE_DECLARATION) {
            PackageDeclaration packageDeclaration = compilationUnit.getPackage();
            return packageDeclaration.annotations();
        }


        if (javaElement.getElementType() == IJavaElement.TYPE) {
            IType type = (IType) javaElement;
            AbstractTypeDeclaration typeDeclaration = AnnotationUtils.getTypeDeclaration(type);
            return extractAnnotations(typeDeclaration.modifiers());
        }

        if (javaElement.getElementType() == IJavaElement.METHOD) {
            IMethod method = (IMethod) javaElement;
            MethodDeclaration methodDeclaration = AnnotationUtils.getMethodDeclaration(method);
            return extractAnnotations(methodDeclaration.modifiers());
        }

        if (javaElement.getElementType() == IJavaElement.FIELD) {
            IField field = (IField) javaElement;
            FieldDeclaration fieldDeclaration = AnnotationUtils.getFieldDeclaration(field);
            return extractAnnotations(fieldDeclaration.modifiers());
        }

        if (javaElement.getElementType() == IJavaElement.LOCAL_VARIABLE) {
            SingleVariableDeclaration singleVariableDeclaration = getSingleVariableDeclaration((ILocalVariable) javaElement);
            if (singleVariableDeclaration != null) {
                return extractAnnotations(singleVariableDeclaration.modifiers());
            }
        }

        return Collections.emptyList();
    }

    private static List<Annotation> extractAnnotations(List<IExtendedModifier> extendedModifiers) {
        List<Annotation> annotations = new ArrayList<Annotation>();
        for (IExtendedModifier extendedModifier : extendedModifiers) {
            if (extendedModifier.isAnnotation()) {
                annotations.add((Annotation) extendedModifier);
            }
        }
        return annotations;
    }

    @SuppressWarnings("unchecked")
    public static List<SingleVariableDeclaration> getSingleVariableDeclarations(final IMethod method) {
        ICompilationUnit source = method.getCompilationUnit();
        CompilationUnit compilationUnit = SharedASTProvider.getAST(source, SharedASTProvider.WAIT_YES, null);
        final List<SingleVariableDeclaration> parameters = new ArrayList<SingleVariableDeclaration>();
        compilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration methodDeclaration) {
                if (compareMethods(methodDeclaration, method)) {
                    parameters.addAll(methodDeclaration.parameters());
                }
                return false;
            }
        });
        return parameters;
    }

    public static ILocalVariable getLocalVariable(IMethod method, int offset) {
        List<SingleVariableDeclaration> parameters = getSingleVariableDeclarations(method);
        for (SingleVariableDeclaration parameter : parameters) {
            int parameterStartPosition = parameter.getStartPosition();
            int parameterLength = parameter.getLength();
            if (offset >= parameterStartPosition
                    && offset <= parameterStartPosition + parameterLength) {
                return (ILocalVariable) parameter.resolveBinding().getJavaElement();
            }
        }

        return null;
    }

    public static ILocalVariable getLocalVariable(IMethod method, String paramName) {
        List<SingleVariableDeclaration> parameters = getSingleVariableDeclarations(method);
        for (SingleVariableDeclaration parameter : parameters) {
            if (parameter.getName().getIdentifier().equals(paramName)) {
                return (ILocalVariable) parameter.resolveBinding().getJavaElement();
            }
        }
        return null;
    }

    public static Annotation getAnnotation(IJavaElement javaElement, Class<? extends java.lang.annotation.Annotation> annotation) {
        List<Annotation> annotations = getAnnotations(javaElement);
        return getAnnotation(annotations, annotation);
    }

    public static AnnotationMirror getAnnotation(Declaration declaration,
            Class<? extends java.lang.annotation.Annotation> annotation) {
        Collection<AnnotationMirror> aannotationMirrors = declaration.getAnnotationMirrors();

        for (AnnotationMirror annotationMirror : aannotationMirrors) {
            com.sun.mirror.declaration.AnnotationTypeDeclaration annotationTypeDeclaration = annotationMirror
            .getAnnotationType().getDeclaration();
            if (annotationTypeDeclaration != null
                    && annotationTypeDeclaration.getQualifiedName().equals(annotation.getCanonicalName())) {
                return annotationMirror;
            }
        }
        return null;
    }

    private static Annotation getAnnotation(List<Annotation> annotations,
            Class<? extends java.lang.annotation.Annotation> annotation) {
        for (Annotation astAnnotation : annotations) {
            String typeName = astAnnotation.getTypeName().getFullyQualifiedName();
            if (typeName.equals(annotation.getCanonicalName())
                    || typeName.equals(annotation.getSimpleName())) {
                return astAnnotation;
            }
        }
        return null;
    }

    public static IAnnotation getAnnotation(Class<? extends java.lang.annotation.Annotation> annotation,
            IAnnotatable annotatable) throws JavaModelException {
        IAnnotation[] annotations = annotatable.getAnnotations();
        for (IAnnotation jdtAnnotation : annotations) {
            String annotationName = jdtAnnotation.getElementName();
            if (annotationName.equals(annotation.getCanonicalName())
                    || annotationName.equals(annotation.getSimpleName())) {
                return jdtAnnotation;
            }
        }
        return null;
    }

    public static AnnotationValue getAnnotationValue(AnnotationMirror mirror, String attributeName) {
        Map<AnnotationTypeElementDeclaration, AnnotationValue> values = mirror.getElementValues();
        Set<Map.Entry<AnnotationTypeElementDeclaration, AnnotationValue>> entrySet = values.entrySet();
        for (Map.Entry<AnnotationTypeElementDeclaration, AnnotationValue> entry : entrySet) {
            AnnotationTypeElementDeclaration element = entry.getKey();
            if (element.getSimpleName().equals(attributeName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Expression getAnnotationValue(NormalAnnotation normalAnnotation, String attributeName) {
        List<MemberValuePair> memberValuePairs = normalAnnotation.values();
        for (MemberValuePair memberValuePair : memberValuePairs) {
            if (memberValuePair.getName().getIdentifier().equals(attributeName)) {
                return memberValuePair.getValue();
            }
        }
        return null;
    }

    public static Object getAnnotationValue(IAnnotation annotation, String attributeName) throws JavaModelException {
        IMemberValuePair[] memberValuePairs = annotation.getMemberValuePairs();
        if (memberValuePairs.length > 0) {
            for (IMemberValuePair memberValuePair : memberValuePairs) {
                if (memberValuePair.getMemberName().equals(attributeName)) {
                    return memberValuePair.getValue();
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static MemberValuePair getMemberValuePair(NormalAnnotation normalAnnotation, String memberName) {
        List<MemberValuePair> memberValuesPairs = normalAnnotation.values();
        for (MemberValuePair memberValuePair : memberValuesPairs) {
            if (memberValuePair.getName().getIdentifier().equals(memberName)) {
                return memberValuePair;
            }
        }
        return null;
    }

    public static String getStringValue(AnnotationMirror mirror, String attributeName) {
        AnnotationValue annotationValue = getAnnotationValue(mirror, attributeName);
        if (annotationValue != null) {
            return annotationValue.getValue().toString();
        }
        return null;
    }

    public static String getStringValue(Annotation annotation, String attributeName) {
        if (annotation instanceof NormalAnnotation) {
            Expression expression = getAnnotationValue((NormalAnnotation) annotation, attributeName);
            if (expression != null && expression instanceof StringLiteral) {
                return ((StringLiteral) expression).getLiteralValue();
            }
        }
        return null;
    }

    public static String getStringValue(IAnnotation annotation, String attributeName)
    throws JavaModelException {
        Object value = AnnotationUtils.getAnnotationValue(annotation, attributeName);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    public static Boolean getBooleanValue(AnnotationMirror mirror, String attributeName) {
        String value = getStringValue(mirror, attributeName);
        if (value != null) {
            return Boolean.valueOf(value);
        }
        return null;
    }

    public static Boolean getBooleanValue(Annotation annotation, String attributeName) {
        if (annotation instanceof NormalAnnotation) {
            Expression expression = getAnnotationValue((NormalAnnotation) annotation, attributeName);
            if (expression != null && expression instanceof BooleanLiteral) {
                return Boolean.valueOf(((BooleanLiteral) expression).booleanValue());
            }
        }
        return null;
    }

    public static Boolean getBooleanValue(IAnnotation annotation, String attributeName) throws JavaModelException {
        String value = AnnotationUtils.getStringValue(annotation, attributeName);
        if (value != null) {
            return Boolean.valueOf(value);
        }
        return null;
    }

    public static String getEnumValue(Annotation annotation, String attributeName) {
        if (annotation instanceof NormalAnnotation) {
            Expression expression = getAnnotationValue((NormalAnnotation) annotation, attributeName);
            if (expression != null && expression instanceof QualifiedName) {
                return ((QualifiedName) expression).getName().getIdentifier();
            }
        }
        return null;
    }

    public static String getEnumValue(IAnnotation annotation, String attributeName) throws JavaModelException {
        String value = AnnotationUtils.getStringValue(annotation, attributeName);
        if (value != null && value.indexOf(".") != -1) {
            return value.substring(value.lastIndexOf(".") + 1);
        }
        return null;
    }
}

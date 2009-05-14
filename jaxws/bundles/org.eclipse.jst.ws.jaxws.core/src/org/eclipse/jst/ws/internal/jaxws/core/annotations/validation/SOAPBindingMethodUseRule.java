/*******************************************************************************
 * Copyright (c) 2009 Progress Software Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.jst.ws.internal.jaxws.core.annotations.validation;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.jst.ws.annotations.core.processor.AbstractAnnotationProcessor;
import org.eclipse.jst.ws.internal.jaxws.core.JAXWSCoreMessages;

import com.sun.mirror.apt.Messager;
import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.AnnotationTypeElementDeclaration;
import com.sun.mirror.declaration.AnnotationValue;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.EnumConstantDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;

/**
 * 
 * @author ohurley
 *
 */
public class SOAPBindingMethodUseRule extends AbstractAnnotationProcessor {

    public SOAPBindingMethodUseRule() {
    }

    @Override
    public void process() {
        Messager messager = environment.getMessager();

        AnnotationTypeDeclaration annotationDeclaration = (AnnotationTypeDeclaration) environment
                .getTypeDeclaration("javax.jws.soap.SOAPBinding"); //$NON-NLS-1$

        Collection<Declaration> annotatedTypes = environment
                .getDeclarationsAnnotatedWith(annotationDeclaration);
        
        boolean methodUseEncoded = false;
        boolean methodParameterStylePresent = false;
        
        for (Declaration declaration : annotatedTypes) {
            if (declaration instanceof MethodDeclaration) {

                Collection<AnnotationMirror> annotationMirrors = declaration.getAnnotationMirrors();
    
                for (AnnotationMirror mirror : annotationMirrors) {
                    Map<AnnotationTypeElementDeclaration, AnnotationValue> valueMap = mirror
                            .getElementValues();
                    Set<Map.Entry<AnnotationTypeElementDeclaration, AnnotationValue>> valueSet = valueMap
                            .entrySet();
                    for (Map.Entry<AnnotationTypeElementDeclaration, AnnotationValue> annotationKeyValue : 
                            valueSet) {

                        if (annotationKeyValue.getKey().getSimpleName().equals("use")) { //$NON-NLS-1$
                            if (annotationKeyValue.getValue() != null) {
                                AnnotationValue annotationValue = annotationKeyValue.getValue();
                                EnumConstantDeclaration enumConstantDeclaration = 
                                    (EnumConstantDeclaration) annotationValue.getValue();
                                if (enumConstantDeclaration.getSimpleName().equals(
                                        javax.jws.soap.SOAPBinding.Use.ENCODED.name())) {
                                	methodUseEncoded = true;
                                }
                            }
                        } else {
                        	if (annotationKeyValue.getKey().getSimpleName().equals("parameterStyle")) { //$NON-NLS-1$
                        		methodParameterStylePresent = true;
                        	}
                        }
                        
                        if (methodParameterStylePresent && methodUseEncoded) {
                            messager.printError(mirror.getPosition(),
                                    JAXWSCoreMessages.SOAPBINDING_NO_PARAMETERSTYLE_WHEN_ENCODED_MESSAGE);
                        	return;
                        }
                    }
                }
            }
        }
    }
}

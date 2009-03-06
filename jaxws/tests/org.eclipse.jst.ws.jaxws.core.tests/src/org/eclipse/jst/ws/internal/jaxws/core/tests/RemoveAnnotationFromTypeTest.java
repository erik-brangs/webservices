/*******************************************************************************
 * Copyright (c) 2009 Shane Clarke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Shane Clarke - initial API and implementation
 *******************************************************************************/
package org.eclipse.jst.ws.internal.jaxws.core.tests;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jst.ws.internal.jaxws.core.annotations.AnnotationsCore;
import org.eclipse.jst.ws.internal.jaxws.core.utils.AnnotationUtils;

/**
 * 
 * @author sclarke
 *
 */
public class RemoveAnnotationFromTypeTest extends AbstractAnnotationTest {
    @Override
    public String getPackageName() {
        return "com.example";
    }

    @Override
    public String getClassName() {
        return "Calculator.java";
    }
    
    @Override
    public String getClassContents() {
        return "@WebService()\npublic class Calculator {\n\n\tpublic int add(int i, int k) {" +
            "\n\t\treturn i + k;\n\t}\n}";
    }

    @Override
    public Annotation getAnnotation() {
        return AnnotationsCore.getAnnotation(ast, javax.jws.WebService.class, null);
    }

    public void testRemoveAnnotationFromType() {
        try {
            assertNotNull(annotation);
            assertEquals("WebService", AnnotationUtils.getAnnotationName(annotation));

            assertTrue(AnnotationUtils.isAnnotationPresent(source,
                    AnnotationUtils.getAnnotationName(annotation)));

            AnnotationUtils.removeAnnotationFromType(source, compilationUnit, rewriter, annotation,
                    textFileChange);

            assertTrue(executeChange(new NullProgressMonitor(), textFileChange));
            
            assertFalse(AnnotationUtils.isAnnotationPresent(source, AnnotationUtils
                    .getAnnotationName(annotation)));
        } catch (CoreException ce) {
            ce.printStackTrace();
        }
    }
}

/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.driver.internal.mapper.processor.dao;

import com.datastax.oss.driver.api.core.AsyncPagingIterable;
import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.SetEntity;
import com.datastax.oss.driver.internal.mapper.processor.MethodGenerator;
import com.datastax.oss.driver.internal.mapper.processor.ProcessorContext;
import com.datastax.oss.driver.internal.mapper.processor.SkipGenerationException;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class DaoGetEntityMethodGenerator implements MethodGenerator {

  private final ExecutableElement methodElement;
  private final DaoImplementationGenerator daoImplementationGenerator;
  private final String targetParameterName;
  private final TypeElement entityElement;
  private boolean pagingReturnType;

  public DaoGetEntityMethodGenerator(
      ExecutableElement methodElement,
      DaoImplementationGenerator daoImplementationGenerator,
      ProcessorContext context) {
    this.methodElement = methodElement;
    this.daoImplementationGenerator = daoImplementationGenerator;
    if (methodElement.getParameters().size() != 1) {
      context
          .getMessager()
          .error(
              methodElement, "%s method must have one parameter", SetEntity.class.getSimpleName());
      throw new SkipGenerationException();
    }
    String tmpTarget = null;
    TypeElement tmpEntityElement = null;
    VariableElement parameterElement = methodElement.getParameters().get(0);
    TypeMirror parameterType = parameterElement.asType();
    if (context.getClassUtils().implementsGettableByName(parameterType)
        || context.getClassUtils().isSame(parameterType, ResultSet.class)
        || context.getClassUtils().isSame(parameterType, AsyncResultSet.class)) {
      tmpTarget = parameterElement.getSimpleName().toString();
    }
    TypeMirror returnType = methodElement.getReturnType();
    if (returnType.getKind() == TypeKind.DECLARED) {
      Element element = ((DeclaredType) returnType).asElement();
      if (element.getKind() == ElementKind.CLASS) {
        if (element.getAnnotation(Entity.class) != null) {
          tmpEntityElement = ((TypeElement) ((DeclaredType) returnType).asElement());
        }
      } else if (element.getKind() == ElementKind.INTERFACE) {
        List<? extends TypeMirror> generics = ((DeclaredType) returnType).getTypeArguments();
        if (generics.size() == 1) {
          TypeMirror generic = generics.get(0);
          Element genericElement = ((DeclaredType) generic).asElement();
          if (genericElement.getAnnotation(Entity.class) != null) {
            if (element.getSimpleName().toString().equals(PagingIterable.class.getSimpleName())
                || element
                    .getSimpleName()
                    .toString()
                    .equals(AsyncPagingIterable.class.getSimpleName())) {
              tmpEntityElement = ((TypeElement) ((DeclaredType) generic).asElement());
              pagingReturnType = true;
            }
          }
        }
      }
    }
    if (tmpEntityElement == null) {
      context
          .getMessager()
          .error(
              methodElement,
              "Invalid return type specified. Expected annotated entity, PagingIterable, or AsyncPagingIterable ");
      throw new SkipGenerationException();
    }
    if (tmpTarget == null) {
      context
          .getMessager()
          .error(
              methodElement,
              "Could not match parameter, expected a GettableByName, ResultSet or AsyncResultSet");
      throw new SkipGenerationException();
    }
    this.entityElement = tmpEntityElement;
    this.targetParameterName = tmpTarget;
  }

  @Override
  public MethodSpec.Builder generate() {
    String helperFieldName = daoImplementationGenerator.addEntityHelperField(entityElement);

    MethodSpec.Builder overridingMethodBuilder =
        MethodSpec.methodBuilder(methodElement.getSimpleName().toString())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassName.get(methodElement.getReturnType()));
    for (VariableElement parameterElement : methodElement.getParameters()) {
      overridingMethodBuilder.addParameter(
          ClassName.get(parameterElement.asType()), parameterElement.getSimpleName().toString());
    }
    if (!pagingReturnType) {

      overridingMethodBuilder.addStatement(
          "return $L.get($L)", helperFieldName, targetParameterName);
    } else {
      overridingMethodBuilder.addStatement(
          "return $L.map($L::get)", targetParameterName, helperFieldName);
    }
    return overridingMethodBuilder;
  }
}

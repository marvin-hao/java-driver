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
package com.datastax.oss.driver.internal.mapper.processor;

import com.datastax.oss.driver.api.mapper.annotations.Mapper;
import com.squareup.javapoet.ClassName;
import javax.lang.model.element.TypeElement;

/** Entry point to generate all the types related to a {@link Mapper}-annotated interface. */
public class MapperGenerator {

  private final TypeElement interfaceElement;
  private final ClassName interfaceName;
  private final Mapper annotation;
  private final GenerationContext context;

  public MapperGenerator(TypeElement interfaceElement, GenerationContext context) {
    this.interfaceElement = interfaceElement;
    interfaceName = ClassName.get(interfaceElement);
    annotation = interfaceElement.getAnnotation(Mapper.class);
    this.context = context;
  }

  public void generate() {
    ClassName builderName = generateBuilderName();
    MapperImplementationGenerator implementation =
        new MapperImplementationGenerator(interfaceName, interfaceElement, builderName, context);
    implementation.generate();
    new MapperBuilderGenerator(builderName, interfaceName, implementation.getClassName(), context)
        .generate();
  }

  private ClassName generateBuilderName() {
    String custom = annotation.builderName();
    if (custom.isEmpty()) {
      return ClassName.get(interfaceName.packageName(), interfaceName.simpleName() + "Builder");
    } else {
      int i = custom.lastIndexOf('.');
      return ClassName.get(custom.substring(0, i), custom.substring(i + 1));
    }
  }
}
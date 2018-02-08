/**
 * Copyright 2016 Goldman Sachs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.gs.dmn.feel.analysis.semantics;

import com.gs.dmn.feel.analysis.syntax.ast.expression.Expression;

/**
 * Created by Octavian Patrascoiu on 06/09/2016.
 */
public class SemanticError extends RuntimeException {
    public SemanticError(Expression expression, String errorMessage) {
        super(String.format("'%s': %s", expression.getClass().getSimpleName(), errorMessage));
    }

    public SemanticError(Expression expression, String errorMessage, Exception e) {
        super(String.format("'%s': %s", expression.getClass().getSimpleName(), errorMessage), e);
    }
}

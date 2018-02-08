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
package com.gs.dmn.transformation.template;

public class POJOTemplateProvider implements TemplateProvider {
    @Override
    public String baseTemplatePath() {
        return "/templates/dmn2java";
    }

    @Override
    public String itemDefinitionInterfaceTemplate() {
        return "itemDefinitionInterface.ftl";
    }

    @Override
    public String itemDefinitionClassTemplate() {
        return "itemDefinitionClass.ftl";
    }

    @Override
    public String bkmTemplateName() {
        return "bkm.ftl";
    }

    @Override
    public String decisionTemplateName() {
        return "decision.ftl";
    }

    @Override
    public String decisionTableRuleOutputTemplate() {
        return "decisionTableRuleOutput.ftl";
    }

    @Override
    public String testBaseTemplatePath() {
        return "/templates/tck";
    }

    public String testTemplateName() {
        return "junit.ftl";
    }
}

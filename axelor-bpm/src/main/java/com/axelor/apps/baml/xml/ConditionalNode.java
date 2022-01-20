/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.baml.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class ConditionalNode extends ProcessActionNode {

  @XmlAttribute(name = "expression")
  private String expression;

  @XmlAttribute(name = "expressionValue")
  private String expressionValue;

  public String getExpression() {
    return expression;
  }

  public String expressionValue() {
    return expressionValue;
  }

  @Override
  public String toCode(boolean dynamic) {

    if (expression == null) {
      return "";
    }

    StringBuilder codeBuilder = new StringBuilder();
    codeBuilder.append("\nif(" + expression + ") {");
    codeBuilder.append(super.toCode(dynamic));
    codeBuilder.append("\n}");

    return codeBuilder.toString();
  }
}

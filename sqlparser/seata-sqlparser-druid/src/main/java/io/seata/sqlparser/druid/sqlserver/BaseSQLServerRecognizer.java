/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.sqlparser.druid.sqlserver;

import com.alibaba.druid.sql.ast.*;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.BaseRecognizer;
import io.seata.sqlparser.struct.Null;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author will
 */
public abstract class BaseSQLServerRecognizer extends BaseRecognizer {

    /**
     * Instantiates a new sqlserver base recognizer
     *
     * @param originalSql the original sql
     */
    public BaseSQLServerRecognizer(String originalSql) {
        super(originalSql);
    }

    public SQLServerOutputVisitor createOutputVisitor(final ParametersHolder parametersHolder,
                                                  final ArrayList<List<Object>> paramAppenderList,
                                                  final StringBuilder sb) {
        return new SQLServerOutputVisitor(sb) {

            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues = parametersHolder.getParameters().get(x.getIndex() + 1);
                    if (paramAppenderList.isEmpty()) {
                        oneParamValues.forEach(t -> paramAppenderList.add(new ArrayList<>()));
                    }
                    for (int i = 0; i < oneParamValues.size(); i++) {
                        Object o = oneParamValues.get(i);
                        paramAppenderList.get(i).add(o instanceof Null ? null : o);
                    }
                }
                return super.visit(x);
            }
        };
    }

    public String getWhereCondition(SQLExpr where, final ParametersHolder parametersHolder,
                                    final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        executeVisit(where, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    public String getWhereCondition(SQLExpr where) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        executeVisit(where, new SQLServerOutputVisitor(sb));
        return sb.toString();
    }

//    protected String getLimit(SQLStatement sqlStatement, SQLType sqlType) {
//        SQLLimit limit = null;
//        if (SQLType.UPDATE == sqlType) {
//            limit = ((SQLUpdateStatement)sqlStatement).getLimit();
//        } else if (SQLType.DELETE == sqlType) {
//            limit = ((SQLDeleteStatement)sqlStatement).getLimit();
//        }
//        if (limit != null) {
//            StringBuilder builder = new StringBuilder(" LIMIT ");
//            SQLIntegerExpr expr;
//            if (limit.getOffset() != null) {
//                expr = (SQLIntegerExpr)limit.getOffset();
//                builder.append(expr.getNumber()).append(",");
//            }
//            if (limit.getRowCount() != null) {
//                expr = (SQLIntegerExpr)limit.getRowCount();
//                builder.append(expr.getNumber());
//            }
//            return builder.toString();
//        }
//        return null;
//    }

    protected String getOrderBy(SQLStatement sqlStatement, SQLType sqlType) {
        SQLOrderBy orderBy = null;
        if (SQLType.UPDATE == sqlType) {
            orderBy = ((SQLUpdateStatement)sqlStatement).getOrderBy();
        } else if (SQLType.DELETE == sqlType) {
            orderBy = ((SQLUpdateStatement)sqlStatement).getOrderBy();
        }
        if (orderBy != null) {
            String space = " ";
            String comma = ",";
            StringBuilder builder = new StringBuilder(space).append("ORDER BY").append(space);
            List<SQLSelectOrderByItem> items = orderBy.getItems();
            for (int i = 0; i < items.size(); i++) {
                SQLSelectOrderByItem item = items.get(i);
                SQLIdentifierExpr expr = (SQLIdentifierExpr)item.getExpr();
                builder.append(expr.getName());
                SQLOrderingSpecification specification = item.getType();
                if (specification != null) {
                    builder.append(space);
                    builder.append(specification.name);
                }
                if (i + 1 != items.size()) {
                    builder.append(comma);
                }
            }
            return builder.toString();
        }
        return null;
    }

}

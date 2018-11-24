/*
 * GRAKN.AI - THE KNOWLEDGE GRAPH
 * Copyright (C) 2018 Grakn Labs Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.core.graql.query.pattern.property;

import grakn.core.graql.concept.Concept;
import grakn.core.graql.concept.Type;
import grakn.core.graql.exception.GraqlQueryException;
import grakn.core.graql.query.pattern.Var;
import grakn.core.graql.admin.Atomic;
import grakn.core.graql.admin.ReasonerQuery;
import grakn.core.graql.query.pattern.VarPatternAdmin;
import grakn.core.graql.internal.gremlin.EquivalentFragmentSet;
import grakn.core.graql.internal.reasoner.atom.property.IsAbstractAtom;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.Set;

import static grakn.core.graql.internal.gremlin.sets.EquivalentFragmentSets.isAbstract;

/**
 * Represents the {@code is-abstract} property on a {@link Type}.
 *
 * This property can be matched or inserted.
 *
 * This property states that a type cannot have direct instances.
 *
 */
public class IsAbstract extends AbstractVar implements UniqueVarProperty {

    private static final IsAbstract INSTANCE = new IsAbstract();

    public static final String NAME = "is-abstract";

    private IsAbstract() {

    }

    public static IsAbstract get() {
        return INSTANCE;
    }

    @Override
    public void buildString(StringBuilder builder) {
        builder.append(NAME);
    }

    @Override
    public Collection<EquivalentFragmentSet> match(Var start) {
        return ImmutableSet.of(isAbstract(this, start));
    }

    @Override
    String getName() {
        return NAME;
    }

    @Override
    public Collection<Executor> define(Var var) throws GraqlQueryException {
        Executor.Method method = executor -> {
            Concept concept = executor.get(var);
            if (concept.isType()) {
                concept.asType().isAbstract(true);
            } else {
                throw GraqlQueryException.insertAbstractOnNonType(concept.asSchemaConcept());
            }
        };

        return ImmutableSet.of(Executor.builder(method).requires(var).build());
    }

    @Override
    public Collection<Executor> undefine(Var var) throws GraqlQueryException {
        Executor.Method method = executor -> {
            Type type = executor.get(var).asType();
            if (!type.isDeleted()) {
                type.isAbstract(false);
            }
        };

        return ImmutableSet.of(Executor.builder(method).requires(var).build());
    }

    @Override
    public Atomic mapToAtom(VarPatternAdmin var, Set<VarPatternAdmin> vars, ReasonerQuery parent) {
        return IsAbstractAtom.create(var.var(), parent);
    }
}
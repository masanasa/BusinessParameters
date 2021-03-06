package be.kwakeroni.parameters.basic.definition;

import be.kwakeroni.parameters.basic.definition.builder.RangedDefinitionBuilder;
import be.kwakeroni.parameters.basic.definition.factory.RangedDefinitionVisitor;
import be.kwakeroni.parameters.definition.api.DefinitionVisitorContext;
import be.kwakeroni.parameters.definition.api.ParameterGroupDefinition;
import be.kwakeroni.parameters.definition.api.builder.DefinitionBuilder;
import be.kwakeroni.parameters.definition.api.builder.DefinitionBuilderFinalizer;
import be.kwakeroni.parameters.types.api.ParameterType;
import be.kwakeroni.parameters.types.support.BasicType;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Created by kwakeroni on 11.04.17.
 */
final class DefaultRangedDefinition implements RangedDefinitionVisitor.Definition, ParameterGroupDefinition {

    private String rangeParameter;
    private ParameterGroupDefinition subGroupDefinition;
    private Factory factory;

    private interface Factory {
        <G> G createGroup(RangedDefinitionVisitor<G> factory, RangedDefinitionVisitor.Definition definition, G subGroup);
    }

    @Override
    public String getName() {
        return subGroupDefinition.getName();
    }

    public String getRangeParameter() {
        return rangeParameter;
    }

    @Override
    public ParameterGroupDefinition getDefinition() {
        return this;
    }

    @Override
    public <G> G apply(DefinitionVisitorContext<G> context) {
        G subGroup = subGroupDefinition.apply(context);
        return factory.createGroup(RangedDefinitionVisitor.from(context), this, subGroup);
    }

    static Builder builder() {
        return new DefaultRangedDefinition().new Builder();
    }

    private final class Builder implements RangedDefinitionBuilder {
        private DefinitionBuilder subGroup;

        @Override
        public <T extends Comparable<? super T>> RangedDefinitionBuilder withComparableRangeParameter(String name, ParameterType<T> type) {
            rangeParameter = name;
            factory = new Factory() {
                @Override
                public <G> G createGroup(RangedDefinitionVisitor<G> f, RangedDefinitionVisitor.Definition d, G s) {
                    return f.visit(d, type, s);
                }
            };
            return this;
        }

        @Override
        public <T> RangedDefinitionBuilder withRangeParameter(String name, ParameterType<T> type, Comparator<? super T> comparator) {
            rangeParameter = name;
            factory = new Factory() {
                @Override
                public <G> G createGroup(RangedDefinitionVisitor<G> factory, RangedDefinitionVisitor.Definition definition, G subGroup) {
                    return factory.visit(definition, type, comparator, subGroup);
                }
            };
            return this;
        }

        @Override
        public <T, B> RangedDefinitionBuilder withRangeParameter(String name, BasicType<T, B> type) {
            rangeParameter = name;
            factory = new Factory() {
                @Override
                public <G> G createGroup(RangedDefinitionVisitor<G> factory, RangedDefinitionVisitor.Definition definition, G subGroup) {
                    return factory.visit(definition, type, subGroup);
                }
            };
            return this;
        }

        @Override
        public <T, B> RangedDefinitionBuilder withRangeParameter(String name, ParameterType<T> type, Function<T, B> converter, BasicType<B, B> basicType) {
            rangeParameter = name;
            factory = new Factory() {
                @Override
                public <G> G createGroup(RangedDefinitionVisitor<G> factory, RangedDefinitionVisitor.Definition definition, G subGroup) {
                    return factory.visit(definition, type, converter, basicType, subGroup);
                }
            };
            return this;
        }

        @Override
        public RangedDefinitionBuilder mappingTo(DefinitionBuilder subGroup) {
            this.subGroup = subGroup;
            return this;
        }

        @Override
        public ParameterGroupDefinition build(String name, Function<DefinitionBuilderFinalizer, DefinitionBuilderFinalizer> theirFinalizer) {
            subGroupDefinition = subGroup.build(name, myFinalizer().andThen(theirFinalizer));
            return DefaultRangedDefinition.this;
        }

        private Function<DefinitionBuilderFinalizer, DefinitionBuilderFinalizer> myFinalizer() {
            return builder -> builder.prependParameter(rangeParameter);
        }

    }
}

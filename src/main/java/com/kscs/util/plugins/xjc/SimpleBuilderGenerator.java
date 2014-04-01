package com.kscs.util.plugins.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.outline.FieldOutline;

/**
 * Helper class to generate fluent builder classes in two steps
 */
public class SimpleBuilderGenerator extends BuilderGenerator {

	SimpleBuilderGenerator(final ApiConstructs apiConstructs, final BuilderOutline classOutline) {
		super(apiConstructs, classOutline);
	}

	protected void generateBuilderMember(final FieldOutline fieldOutline, final JFieldVar declaredField, final JBlock initBody, final JVar productParam) {
		final String propertyName = fieldOutline.getPropertyInfo().getName(true);
		if (fieldOutline.getPropertyInfo().isCollection()) {

			if (declaredField.type().isArray()) {
				generateArrayProperty(initBody, productParam, declaredField, propertyName, declaredField.type().elementType(), this.builderClass);
			} else {

				final JClass elementType = ((JClass) declaredField.type()).getTypeParameters().get(0);

				final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, declaredField.type(), declaredField.name(), JExpr._new(this.apiConstructs.arrayListClass.narrow(elementType)));

				final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
				final JVar addListParam = addListMethod.param(JMod.FINAL, fieldOutline.getRawType(), declaredField.name());
				addListMethod.body().invoke(JExpr._this().ref(declaredField), ApiConstructs.ADD_ALL).arg(addListParam);
				addListMethod.body()._return(JExpr._this());

				final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
				final JVar addVarargsParam = addVarargsMethod.varParam(elementType, declaredField.name());
				addVarargsMethod.body().invoke(addListMethod).arg(this.apiConstructs.asList(addVarargsParam));
				addVarargsMethod.body()._return(JExpr._this());

				final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
				final JVar withListParam = withListMethod.param(JMod.FINAL, fieldOutline.getRawType(), declaredField.name());
				withListMethod.body().assign(JExpr._this().ref(declaredField), withListParam);
				withListMethod.body()._return(JExpr._this());

				final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
				final JVar withVarargsParam = withVarargsMethod.varParam(elementType, declaredField.name());
				withVarargsMethod.body().invoke(withListMethod).arg(this.apiConstructs.asList(withVarargsParam));
				withVarargsMethod.body()._return(JExpr._this());

				if (this.hasImmutablePlugin) {
					initBody.assign(productParam.ref(declaredField), this.apiConstructs.unmodifiableList(JExpr._this().ref(builderField)));
				} else {
					initBody.assign(productParam.ref(declaredField), JExpr._this().ref(builderField));
				}
			}
		} else {
			final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, declaredField.type(), declaredField.name());
			final JMethod withMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withMethod.param(JMod.FINAL, fieldOutline.getRawType(), declaredField.name());
			final JBlock body = withMethod.body();
			body.assign(JExpr._this().ref(builderField), param);
			body._return(JExpr._this());
			initBody.assign(productParam.ref(declaredField), JExpr._this().ref(builderField));
		}
	}


	protected void generateBuilderMemberOverride(final FieldOutline superFieldOutline, final JFieldVar declaredSuperField, final String superPropertyName) {
		if (superFieldOutline.getPropertyInfo().isCollection()) {
			final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
			addListMethod.annotate(Override.class);
			final JVar addListParam = addListMethod.param(JMod.FINAL, superFieldOutline.getRawType(), declaredSuperField.name());
			addListMethod.body().invoke(JExpr._super(), ApiConstructs.ADD_METHOD_PREFIX + superPropertyName).arg(addListParam);
			addListMethod.body()._return(JExpr._this());

			final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
			addVarargsMethod.annotate(Override.class);
			final JVar addVarargsParam = addVarargsMethod.varParam(((JClass) declaredSuperField.type()).getTypeParameters().get(0), declaredSuperField.name());
			addVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.ADD_METHOD_PREFIX + superPropertyName).arg(addVarargsParam);
			addVarargsMethod.body()._return(JExpr._this());

			final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
			withListMethod.annotate(Override.class);
			final JVar withListParam = withListMethod.param(JMod.FINAL, superFieldOutline.getRawType(), declaredSuperField.name());
			withListMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withListParam);
			withListMethod.body()._return(JExpr._this());

			final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
			withVarargsMethod.annotate(Override.class);
			final JVar withVarargsParam = withVarargsMethod.varParam(((JClass) declaredSuperField.type()).getTypeParameters().get(0), declaredSuperField.name());
			withVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withVarargsParam);
			withVarargsMethod.body()._return(JExpr._this());
		} else {
			final JMethod withMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
			withMethod.annotate(Override.class);
			final JVar param = withMethod.param(JMod.FINAL, superFieldOutline.getRawType(), declaredSuperField.name());
			withMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(param);
			withMethod.body()._return(JExpr._this());
		}
	}


	@Override
	protected JDefinedClass generateExtendsClause(final BuilderOutline superClassBuilder) {
		return this.builderClass._extends(superClassBuilder.getDefinedBuilderClass());
	}

	@Override
	protected JMethod generateBuildMethod(final JMethod initMethod) {
		final JMethod buildMethod = this.builderClass.method(JMod.PUBLIC, this.definedClass, ApiConstructs.BUILD_METHOD_NAME);
		buildMethod.body()._return((JExpr._this().invoke(initMethod).arg(JExpr._new(this.definedClass))));
		return buildMethod;

	}

	@Override
	protected JMethod generateBuilderMethod() {
		final JMethod builderMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass, ApiConstructs.BUILDER_METHOD_NAME);
		builderMethod.body()._return(JExpr._new(this.builderClass));
		return builderMethod;
	}
}

package org.jjflyboy.forge.addon.crud.commands;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.Id;

import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.javaee.jpa.ui.setup.JPASetupWizard;
import org.jboss.forge.addon.parser.java.beans.ProjectOperations;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.command.PrerequisiteCommandsProvider;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.Member;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceCommonDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceUnitCommon;
import org.jjflyboy.forge.addon.crud.CrudToolGenerationContext;
import org.jjflyboy.forge.addon.crud.CrudToolResourceGenerator;

public class CrudOnEntityCommand extends AbstractProjectCommand implements PrerequisiteCommandsProvider {

	@Inject
	@WithAttributes(label = "Targets", required = true)
	private UISelectMany<JavaClassSource> targets;

	@Inject
	@WithAttributes(label = "Generator", required = true)
	private UISelectOne<CrudToolResourceGenerator> generator;

	@Inject
	@WithAttributes(label = "Persistence Unit", required = true)
	private UISelectOne<String> persistenceUnit;

	@Inject
	@WithAttributes(label = "Target Package Name", required = true, type = InputType.JAVA_PACKAGE_PICKER)
	private UIInput<String> packageName;

	@Inject
	@WithAttributes(label = "Base Class package name", required = false)
	private UIInput<String> baseClassPackageName;

	@Inject
	@WithAttributes(label = "Overwrite existing classes?", enabled = false, defaultValue = "false")
	private UIInput<Boolean> overwrite;

	@Inject
	private CrudToolResourceGenerator defaultResourceGenerator;

	@Inject
	private ProjectOperations projectOperations;

	@Override

	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(CrudOnEntityCommand.class).name("Crud: generate")
				.category(Categories.create("crud"));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		UIContext context = builder.getUIContext();
		Project project = getSelectedProject(context);
		JPAFacet<PersistenceCommonDescriptor> persistenceFacet = project.getFacet(JPAFacet.class);
		JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
		List<JavaClassSource> allEntities = persistenceFacet.getAllEntities();
		List<JavaClassSource> supportedEntities = new ArrayList<>();
		for (JavaClassSource entity : allEntities) {
			if (isEntityWithSimpleKey(entity, javaSourceFacet)) {
				supportedEntities.add(entity);
			}
		}
		targets.setValueChoices(supportedEntities);
		targets.setItemLabelConverter(new Converter<JavaClassSource, String>() {
			@Override
			public String convert(JavaClassSource source) {
				return source == null ? null : source.getQualifiedName();
			}
		});
		List<String> persistenceUnits = new ArrayList<>();
		List<PersistenceUnitCommon> allUnits = persistenceFacet.getConfig().getAllPersistenceUnit();
		for (PersistenceUnitCommon persistenceUnit : allUnits) {
			persistenceUnits.add(persistenceUnit.getName());
		}
		if (!persistenceUnits.isEmpty()) {
			persistenceUnit.setValueChoices(persistenceUnits).setDefaultValue(persistenceUnits.get(0));
		}

		packageName.setDefaultValue(javaSourceFacet.getBasePackage() + ".crud");

		String baseClassPackage = null;
		for (JavaResource x : projectOperations.getProjectInterfaces(project)) {

			JavaType<?> jt = x.getJavaType();
			if(jt.getName().equals("EntityFinder")) {
				baseClassPackage = jt.getPackage();
			}
		}
		baseClassPackage = baseClassPackage == null ? javaSourceFacet.getBasePackage() + ".crud.base"
				: baseClassPackage;
		baseClassPackageName.setDefaultValue(baseClassPackage);

		generator.setDefaultValue(defaultResourceGenerator);
		if (context.getProvider().isGUI()) {
			generator.setItemLabelConverter(new Converter<CrudToolResourceGenerator, String>() {
				@Override
				public String convert(CrudToolResourceGenerator source) {
					return source == null ? null : source.getDescription();
				}
			});
		} else {
			generator.setItemLabelConverter(new Converter<CrudToolResourceGenerator, String>() {
				@Override
				public String convert(CrudToolResourceGenerator source) {
					return source == null ? null : source.getName();
				}
			});
		}
		builder.add(targets)
		.add(generator)
		.add(packageName)
		.add(baseClassPackageName)
		.add(persistenceUnit)
		.add(overwrite);
	}

	private boolean isEntityWithSimpleKey(JavaClassSource entity, JavaSourceFacet javaSourceFacet) {
		for (Member<?> member : entity.getMembers()) {
			// FORGE-823 Only add entities with @Id as valid entities for crud
			// resource generation.
			// Composite keys are not yet supported.
			if (member.hasAnnotation(Id.class)) {
				return true;
			}
		}
		JavaClassSource superClass = getSuperClass(entity, javaSourceFacet);
		if (superClass != null) {
			return isEntityWithSimpleKey(superClass, javaSourceFacet);
		}
		return false;
	}

	private JavaClassSource getSuperClass(JavaClassSource entity, JavaSourceFacet javaSourceFacet) {
		String superClass = entity.getSuperType();
		JavaClassSource result = null;
		if (!superClass.equals(Object.class.getName())) {
			try {
				result = javaSourceFacet.getJavaResource(superClass).getJavaType();
			} catch (FileNotFoundException e) {
				// hopefully we won't see this problem.
				throw new RuntimeException();
			}
		}
		return result;
	}

	@Override
	public Result execute(final UIExecutionContext context) throws Exception {
		UIContext uiContext = context.getUIContext();
		CrudToolGenerationContext generationContext = createContextFor(uiContext);
		Set<JavaSource<?>> generated = generateCrud(generationContext);
		Project project = generationContext.getProject();
		JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);

		// save the generated classes
		for (JavaSource<?> javaClass : generated) {
			// don't overwrite
			JavaResource js = javaSourceFacet.getJavaResource(javaClass);
			if (js == null || !js.exists()) {
				javaSourceFacet.saveJavaSource(javaClass);
			}
		}
		String path = packageName.getValue().replace('.', '/');
		uiContext.setSelection(javaSourceFacet.getSourceDirectory().getChild(path));
		return Results.success("crud created");
	}

	private Set<JavaSource<?>> generateCrud(CrudToolGenerationContext generationContext) throws Exception {
		CrudToolResourceGenerator selectedGenerator = generator.getValue();
		Set<JavaSource<?>> classes = new HashSet<>();
		for (JavaClassSource target : targets.getValue()) {
			generationContext.setEntity(target);
			List<JavaSource<?>> artifacts = selectedGenerator.generateFrom(generationContext);
			classes.addAll(artifacts);
		}
		return classes;
	}

	@Override
	protected boolean isProjectRequired() {
		return true;
	}

	private CrudToolGenerationContext createContextFor(final UIContext context) {
		CrudToolGenerationContext generationContext = new CrudToolGenerationContext();
		generationContext.setProject(getSelectedProject(context));
		generationContext.setPersistenceUnitName(persistenceUnit.getValue());
		generationContext.setTargetPackageName(packageName.getValue());
		generationContext.setBaseClassPackageName(baseClassPackageName.getValue());
		return generationContext;
	}

	@Override
	public NavigationResult getPrerequisiteCommands(UIContext context) {
		NavigationResultBuilder builder = NavigationResultBuilder.create();
		Project project = getSelectedProject(context);
		if (project != null) {
			if (!project.hasFacet(JPAFacet.class)) {
				builder.add(JPASetupWizard.class);
			}
		}
		return builder.build();
	}

	@Inject
	private ProjectFactory projectFactory;

	@Override
	protected ProjectFactory getProjectFactory() {
		return projectFactory;
	}

}
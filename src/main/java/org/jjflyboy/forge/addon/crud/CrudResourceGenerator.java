package org.jjflyboy.forge.addon.crud;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.templates.Template;
import org.jboss.forge.addon.templates.TemplateFactory;
import org.jboss.forge.addon.templates.freemarker.FreemarkerTemplate;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaAnnotationSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class CrudResourceGenerator implements CrudToolResourceGenerator {

	@Inject
	TemplateFactory templateFactory;

	@Inject
	ResourceFactory resourceFactory;

	@Override
	public List<JavaClassSource> generateFrom(CrudToolGenerationContext context) throws Exception {
		List<JavaClassSource> result = new ArrayList<>();

		JavaClassSource entity = context.getEntity();
		String persistenceUnitName = context.getPersistenceUnitName();

		Map<Object, Object> map = new HashMap<>();
		map.put("entity", entity);
		map.put("persistenceUnitName", persistenceUnitName);
		map.put("context", context);

		List<JavaSource<?>> javaSources = new ArrayList<>();

		// these are common to all entities
		javaSources.add(createEntityManagerProducer(map));
		javaSources.add(createCreatorInterface(map));
		javaSources.add(createCreatorImpl(map));
		javaSources.add(createUpdaterInterface(map));
		javaSources.add(createUpdaterImpl(map));
		javaSources.add(createRemoverInterface(map));
		javaSources.add(createRemoverImpl(map));
		javaSources.add(createSpecificationInterface(map));
		javaSources.add(createFinderInterface(map));
		javaSources.add(createFinderListResultInterface(map));
		javaSources.add(createFinderSingleResultInterface(map));
		javaSources.add(createFindListGeneric(map));
		javaSources.add(createFindSingleGeneric(map));

		// these depend on entity
		javaSources.add(createCreatorEntityInterface(map));
		javaSources.add(createUpdaterEntityInterface(map));
		javaSources.add(createRemoverEntityInterface(map));
		javaSources.add(createEntityFinderListResultInterface(map));
		javaSources.add(createEntityFinderSingleResultInterface(map));

		JavaSourceFacet javaSourceFacet = context.getProject().getFacet(JavaSourceFacet.class);
		for(JavaSource<?> js: javaSources) {
			javaSourceFacet.saveJavaSource(js);
		}

		result.add(createCreatorEntityImpl(map));
		result.add(createUpdaterEntityImplementation(map));
		result.add(createRemoverEntityImpl(map));
		result.add(createEntityFinderListResultImplementation(map));
		result.add(createEntityFinderSingleResultImplementation(map));
		// result.add(createEntityAbstractBuilder(map));
		// result.add(createEntityBuilder(map));

		return result;
	}


	private JavaClassSource createEntityFinderSingleResultImplementation(Map<Object, Object> map) {
		return generateClassFromTemplate(map, "EntityFinderSingleResultImplementation.jv");
	}

	private JavaClassSource createEntityFinderListResultImplementation(Map<Object, Object> map) {
		return generateClassFromTemplate(map, "EntityFinderListResultImplementation.jv");
	}

	private JavaSource<?> createEntityFinderSingleResultInterface(Map<Object, Object> map) {
		return generateInterfaceFromTemplate(map, "EntityFinderSingleResultInterface.jv");
	}

	private JavaSource<?> createEntityFinderListResultInterface(Map<Object, Object> map) {
		return generateInterfaceFromTemplate(map, "EntityFinderListResultInterface.jv");
	}

	private JavaSource<?> createFinderSingleResultInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "FinderSingleResultInterface.txt");
	}

	private JavaSource<?> createFinderListResultInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "FinderListResultInterface.txt");
	}

	private JavaSource<?> createFindSingleGeneric(Map<Object, Object> map) {
		return generateClassFromTextFile(map, "FinderSingleGeneric.txt");
	}

	private JavaSource<?> createFindListGeneric(Map<Object, Object> map) {
		return generateClassFromTextFile(map, "FinderListGeneric.txt");
	}

	private JavaSource<?> createSpecificationInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "Specification.txt");
	}

	private JavaClassSource createEntityBuilder(Map<Object, Object> map) {
		return generateClassFromTemplate(map,  "EntityCreateBuilder.jv");
	}


	private JavaClassSource createEntityAbstractBuilder(Map<Object, Object> map) {
		return generateClassFromTemplate(map,  "EntityAbstractBuilder.jv");
	}


	private JavaAnnotationSource createBusinessKeyAnnotation(String packageName) {
		JavaAnnotationSource businessKeyAnnotation = Roaster.create(JavaAnnotationSource.class)
				.setName("BusinessKey");
		businessKeyAnnotation.addAnnotation(Retention.class).setEnumValue(RetentionPolicy.RUNTIME);
		businessKeyAnnotation.addAnnotation(Target.class).setEnumValue(ElementType.FIELD, ElementType.METHOD);
		businessKeyAnnotation.setPackage(packageName);
		return businessKeyAnnotation;
	}

	private JavaSource<?> createUpdaterImpl(Map<Object, Object> map) {
		return generateClassFromTextFile(map, "UpdaterImplementation.txt");
	}

	private JavaSource<?> createCreatorImpl(Map<Object, Object> map) {
		return generateClassFromTextFile(map, "CreatorImplementation.txt");
	}

	private JavaSource<?> createRemoverImpl(Map<Object, Object> map) {
		return generateClassFromTextFile(map, "RemoverImplementation.txt");
	}

	private JavaSource<?> createRemoverEntityInterface(Map<Object, Object> map) {
		return generateInterfaceFromTemplate(map, "EntityRemoverInterface.jv");
	}

	private JavaClassSource createRemoverEntityImpl(Map<Object, Object> map) {
		return generateClassFromTemplate(map, "EntityRemoverImplementation.jv");
	}

	private JavaSource<?> createUpdaterEntityInterface(Map<Object, Object> map) {
		return generateInterfaceFromTemplate(map, "EntityUpdaterInterface.jv");
	}

	private JavaClassSource createUpdaterEntityImplementation(Map<Object, Object> map) {
		return generateClassFromTemplate(map, "EntityUpdaterImplementation.jv");
	}

	private JavaSource<?> createCreatorEntityInterface(Map<Object, Object> map) {
		return generateInterfaceFromTemplate(map, "EntityCreatorInterface.jv");
	}

	private JavaClassSource createCreatorEntityImpl(Map<Object, Object> map) {
		return generateClassFromTemplate(map, "EntityCreatorImplementation.jv");
	}

	private JavaClassSource generateClassFromTemplate(Map<Object, Object> map, String template) {
		String output = applyTemplate(map, template);
		JavaClassSource source = parse(JavaClassSource.class, output);
		finalizeGeneration(map, source);
		return source;
	}

	private JavaInterfaceSource generateInterfaceFromTemplate(Map<Object, Object> map, String template) {
		String output = applyTemplate(map, template);
		JavaInterfaceSource source = parse(JavaInterfaceSource.class, output);
		finalizeGeneration(map, source);
		return source;
	}


	private void finalizeGeneration(Map<Object, Object> map, JavaSource<?> resource) {
		resource.addImport(((JavaClassSource)map.get("entity")).getQualifiedName());
		resource.setPackage(((CrudToolGenerationContext)map.get("context")).getTargetPackageName());
	}


	private String applyTemplate(Map<Object, Object> map, String template) {
		Resource<URL> templateResource = resourceFactory.create(getClass().getResource(template));
		String output = applyTemplate(map, templateResource);
		return output;
	}

	private <T extends JavaType<T>> T parse(Class<T> c, String string) {
		return Roaster.parse(c, string);
	}

	private String applyTemplate(Map<Object, Object> map, Resource<URL> templateResource) {
		Template processor = templateFactory.create(templateResource, FreemarkerTemplate.class);
		try {
			return processor.process(map);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private JavaClassSource generateClassFromTextFile(Map<Object, Object> map, String name) {
		Resource<URL> resource = resourceFactory.create(getClass().getResource(name));
		JavaClassSource source;
		try {
			source = Roaster.parse(JavaClassSource.class, resource.getUnderlyingResourceObject());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		source.setPackage(((CrudToolGenerationContext)map.get("context")).getTargetPackageName());
		return source;
	}

	private JavaInterfaceSource generateInterfaceFromTextFile(Map<Object, Object> map, String name) {
		Resource<URL> resource = resourceFactory.create(getClass().getResource(name));
		JavaInterfaceSource source;
		try {
			source = Roaster.parse(JavaInterfaceSource.class, resource.getUnderlyingResourceObject());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		source.setPackage(((CrudToolGenerationContext)map.get("context")).getTargetPackageName());
		return source;
	}

	private JavaInterfaceSource createCreatorInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "CreatorInterface.txt");
	}

	private JavaInterfaceSource createRemoverInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "RemoverInterface.txt");
	}

	private JavaInterfaceSource createUpdaterInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "UpdaterInterface.txt");
	}

	private JavaInterfaceSource createCreateBuilderInterface(Map<Object, Object> map) {
		CrudToolGenerationContext context = (CrudToolGenerationContext)map.get("context");
		String targetPackageName = context.getTargetPackageName();
		JavaInterfaceSource source = Roaster.create(JavaInterfaceSource.class)
				.setPackage(targetPackageName)
				.setName("ICreateBuilder");
		source.addTypeVariable()
		.setName("T");
		source.addMethod()
		.setName("apply")
		.setReturnType("T");
		return source;
	}


	private JavaInterfaceSource createUpdateBuilderInterface(Map<Object, Object> map) {
		CrudToolGenerationContext context = (CrudToolGenerationContext)map.get("context");
		String targetPackageName = context.getTargetPackageName();
		JavaInterfaceSource source = Roaster.create(JavaInterfaceSource.class)
				.setPackage(targetPackageName)
				.setName("IUpdateBuilder");
		source.addTypeVariable()
		.setName("T");
		source.addMethod()
		.setName("apply")
		.setReturnType("T")
		.addParameter("T", "t");
		return source;
	}

	private JavaInterfaceSource createFinderInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "FinderInterface.txt");
	}

	private JavaClassSource createEntityManagerProducer(Map<Object, Object> map) {
		CrudToolGenerationContext context = (CrudToolGenerationContext)map.get("context");
		String targetPackageName = context.getTargetPackageName();
		String unitName = (String) map.get("persistenceUnitName");

		JavaClassSource producer = Roaster.create(JavaClassSource.class)
				.setPackage(targetPackageName)
				.setName("EntityManagerProducer");
		producer.addAnnotation(ApplicationScoped.class);

		//producer.addImport(EntityManager.class);
		FieldSource<JavaClassSource> emfield = producer.addField()
				.setName("entityManager")
				.setPrivate()
				.setType(EntityManager.class);
		;

		producer.addImport(PersistenceContext.class);
		AnnotationSource<JavaClassSource> pc = emfield.addAnnotation(PersistenceContext.class);
		if(unitName != null) {
			pc.setStringValue("unitName", unitName);
		}

		MethodSource<JavaClassSource> method = producer.addMethod()
				.setName("getEntityManager")
				.setReturnType(EntityManager.class)
				.setPublic()
				.setBody("return entityManager;");
		method.addAnnotation(Produces.class);
		method.addAnnotation(RequestScoped.class);

		return producer;
	}

	@Override
	public String getDescription() {
		return "Generates crud commands for each jpa entity.";
	}

	@Override
	public String getName() {
		return "CRUD_GENERATOR";
	}

}

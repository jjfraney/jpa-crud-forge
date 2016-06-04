package org.jjflyboy.forge.addon.crud;

import java.io.IOException;
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

import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.templates.Template;
import org.jboss.forge.addon.templates.TemplateFactory;
import org.jboss.forge.addon.templates.freemarker.FreemarkerTemplate;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
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
	public List<JavaSource<?>> generateFrom(CrudToolGenerationContext context) throws Exception {

		JavaClassSource entity = context.getEntity();
		String persistenceUnitName = context.getPersistenceUnitName();

		Map<Object, Object> map = new HashMap<>();
		map.put("entity", entity);
		map.put("persistenceUnitName", persistenceUnitName);
		map.put("baseClassPackageName", context.getBaseClassPackageName());
		map.put("targetPackageName", context.getTargetPackageName());
		map.put("context", context);

		// these are common to all entities
		List<JavaSource<?>> baseClasses = new ArrayList<>();
		baseClasses.add(createEntityManagerProducer(map));
		baseClasses.add(createSpecificationInterface(map));
		baseClasses.add(createOperationsInterface(map));
		baseClasses.add(createOperationsImplementation(map));
		baseClasses.add(createQueryInterface(map));
		baseClasses.add(createQueryImplementation(map));
		for(JavaSource<?> js: baseClasses) {
			js.setPackage(context.getBaseClassPackageName());
		}

		List<JavaSource<?>> entityCrudClasses = new ArrayList<>();
		// these depend on entity
		entityCrudClasses.add(createEntityOperationsInterface(map));
		entityCrudClasses.add(createEntityQueryInterface(map));
		entityCrudClasses.add(createEntityOperationsImplementation(map));
		entityCrudClasses.add(createEntityQueryImplementation(map));
		for(JavaSource<?> js: entityCrudClasses) {
			js.setPackage(context.getTargetPackageName());
		}

		List<JavaSource<?>> result = new ArrayList<>();
		result.addAll(entityCrudClasses);
		result.addAll(baseClasses);
		return result;
	}


	private JavaSource<?> createSpecificationInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "Specification.txt");
	}

	private JavaSource<?> createEntityOperationsInterface(Map<Object, Object> map) {
		return generateInterfaceFromTemplate(map, "EntityOperationsInterface.jv");
	}

	private JavaSource<?> createEntityOperationsImplementation(Map<Object, Object> map) {
		return generateClassFromTemplate(map, "EntityOperationsImplementation.jv");
	}

	private JavaSource<?> createEntityQueryInterface(Map<Object, Object> map) {
		return generateInterfaceFromTemplate(map, "EntityQueryInterface.jv");
	}

	private JavaSource<?> createEntityQueryImplementation(Map<Object, Object> map) {
		return generateClassFromTemplate(map, "EntityQueryImplementation.jv");
	}

	private JavaSource<?> createQueryInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "QueryInterface.txt");
	}

	private JavaSource<?> createQueryImplementation(Map<Object, Object> map) {
		return generateClassFromTextFile(map, "QueryImplementation.txt");
	}

	private JavaSource<?> createOperationsInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "OperationsInterface.txt");
	}

	private JavaSource<?> createOperationsImplementation(Map<Object, Object> map) {
		return generateClassFromTextFile(map, "OperationsImplementation.txt");
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
		return source;
	}

	private JavaClassSource createEntityManagerProducer(Map<Object, Object> map) {
		String unitName = (String) map.get("persistenceUnitName");

		JavaClassSource producer = Roaster.create(JavaClassSource.class)
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

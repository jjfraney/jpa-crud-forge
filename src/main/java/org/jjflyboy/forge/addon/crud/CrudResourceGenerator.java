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
		baseClasses.add(createPersisterInterface(map));
		baseClasses.add(createPersisterImpl(map));
		baseClasses.add(createMergerInterface(map));
		baseClasses.add(createMergerImpl(map));
		baseClasses.add(createRemoverInterface(map));
		baseClasses.add(createRemoverImpl(map));
		baseClasses.add(createSpecificationInterface(map));
		baseClasses.add(createFinderInterface(map));
		baseClasses.add(createFinderListResultInterface(map));
		baseClasses.add(createFinderSingleResultInterface(map));
		baseClasses.add(createFindListGeneric(map));
		baseClasses.add(createFindSingleGeneric(map));
		for(JavaSource<?> js: baseClasses) {
			js.setPackage(context.getBaseClassPackageName());
		}

		List<JavaSource<?>> entityCrudClasses = new ArrayList<>();
		// these depend on entity
		entityCrudClasses.add(createPersisterEntityInterface(map));
		entityCrudClasses.add(createMergerEntityInterface(map));
		entityCrudClasses.add(createRemoverEntityInterface(map));
		entityCrudClasses.add(createEntityFinderListResultInterface(map));
		entityCrudClasses.add(createEntityFinderSingleResultInterface(map));
		entityCrudClasses.add(createPersisterEntityImpl(map));
		entityCrudClasses.add(createMergerEntityImplementation(map));
		entityCrudClasses.add(createRemoverEntityImpl(map));
		entityCrudClasses.add(createEntityFinderListResultImplementation(map));
		entityCrudClasses.add(createEntityFinderSingleResultImplementation(map));
		for(JavaSource<?> js: entityCrudClasses) {
			js.setPackage(context.getTargetPackageName());
		}

		List<JavaSource<?>> result = new ArrayList<>();
		result.addAll(entityCrudClasses);
		result.addAll(baseClasses);
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


	private JavaSource<?> createMergerImpl(Map<Object, Object> map) {
		return generateClassFromTextFile(map, "MergerImplementation.txt");
	}

	private JavaSource<?> createPersisterImpl(Map<Object, Object> map) {
		return generateClassFromTextFile(map, "PersisterImplementation.txt");
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

	private JavaSource<?> createMergerEntityInterface(Map<Object, Object> map) {
		return generateInterfaceFromTemplate(map, "EntityMergerInterface.jv");
	}

	private JavaClassSource createMergerEntityImplementation(Map<Object, Object> map) {
		return generateClassFromTemplate(map, "EntityMergerImplementation.jv");
	}

	private JavaSource<?> createPersisterEntityInterface(Map<Object, Object> map) {
		return generateInterfaceFromTemplate(map, "EntityPersisterInterface.jv");
	}

	private JavaClassSource createPersisterEntityImpl(Map<Object, Object> map) {
		return generateClassFromTemplate(map, "EntityPersisterImplementation.jv");
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

	private JavaInterfaceSource createPersisterInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "PersisterInterface.txt");
	}

	private JavaInterfaceSource createRemoverInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "RemoverInterface.txt");
	}

	private JavaInterfaceSource createMergerInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "MergerInterface.txt");
	}

	private JavaInterfaceSource createFinderInterface(Map<Object, Object> map) {
		return generateInterfaceFromTextFile(map, "FinderInterface.txt");
	}

	private JavaClassSource createEntityManagerProducer(Map<Object, Object> map) {
		CrudToolGenerationContext context = (CrudToolGenerationContext)map.get("context");
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

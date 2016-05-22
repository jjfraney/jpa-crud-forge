package org.jjflyboy.forge.addon.crud;

import java.util.List;

import org.jboss.forge.roaster.model.source.JavaSource;

public interface CrudToolResourceGenerator
{
	/**
	 * A readable description for this strategy
	 */
	String getName();

	/**
	 * A human-readable description for this strategy
	 */
	String getDescription();

	/**
	 * Generate a crud tools based on a context
	 */
	List<JavaSource<?>> generateFrom(CrudToolGenerationContext context) throws Exception;
}
